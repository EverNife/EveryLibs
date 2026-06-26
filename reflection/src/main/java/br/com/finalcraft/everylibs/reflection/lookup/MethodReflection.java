package br.com.finalcraft.everylibs.reflection.lookup;

import br.com.finalcraft.everylibs.reflection.MethodInvoker;
import br.com.finalcraft.everylibs.reflection.internal.HandleMethodInvoker;
import br.com.finalcraft.everylibs.reflection.internal.MemberKey;
import br.com.finalcraft.everylibs.reflection.internal.ReflectionCache;
import br.com.finalcraft.everylibs.reflection.internal.TypeHierarchy;
import br.com.finalcraft.everylibs.reflection.internal.TypeMatching;
import jakarta.annotation.Nullable;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Method lookup. {@code getMethod}/{@code getTypedMethod} return the invoker, or {@code null} if
 * no matching method exists — a miss is not an exception. Every resolved invoker is cached.
 * <p>
 * A single recursive core drives every lookup, so the optional return-type filter is
 * preserved as the search walks up the superclass chain and then across implemented
 * interfaces (picking up {@code default} methods a class does not override). An empty
 * {@code params} array means "ignore parameters", matching the first method of the
 * given name (fewest parameters first). An <em>exact</em> parameter/return-type match
 * always wins; a primitive/wrapper-compatible match is used only when no exact match
 * exists, so existing overload selection never changes. A method declared on the class
 * (or a superclass) shadows an inherited {@code default} of the same name, and a real
 * method wins over a synthetic bridge of the same signature.
 * <p>
 * A stateless singleton reached through {@code FCReflectionUtil.methods()} or
 * {@link #INSTANCE}.
 */
public final class MethodReflection {

    public static final MethodReflection INSTANCE = new MethodReflection();

    private MethodReflection() {
    }

    /**
     * @return an invoker for the method, or {@code null} if none matches.
     */
    @Nullable
    public <T> MethodInvoker<T> getMethod(Class<?> clazz, String name, Class<?>... params) {
        return resolveInvoker(clazz, name, null, params);
    }

    @Nullable
    public <T> MethodInvoker<T> getMethod(String className, String name, Class<?>... params) {
        Class<?> clazz = ClassReflection.INSTANCE.getClass(className);
        return clazz == null ? null : this.<T>getMethod(clazz, name, params);
    }

    /**
     * @return an invoker for the method whose return type matches {@code returnType}, or
     * {@code null} if none matches.
     */
    @Nullable
    public <R> MethodInvoker<R> getTypedMethod(Class<?> clazz, String name, Class<R> returnType, Class<?>... params) {
        return resolveInvoker(clazz, name, returnType, params);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <T> MethodInvoker<T> resolveInvoker(Class<?> clazz, String name, Class<?> returnType, Class<?>[] params) {
        Class<?>[] signature = params == null ? EMPTY : params;
        ReflectionCache.PerClassCache cache = ReflectionCache.forOwner(clazz);
        MemberKey key = new MemberKey(name, returnType, signature, 0);
        MethodInvoker<?> invoker = ReflectionCache.resolve(cache.methods(), cache.negativeMethods(), key, () -> {
            Method method = resolve(clazz, name, returnType, signature);
            return method == null ? null : new HandleMethodInvoker<>(method);
        });
        return (MethodInvoker<T>) invoker;
    }

    /**
     * Stream every method of {@code clazz} (declared, all visibilities, across the
     * superclass chain, plus the {@code default} methods of implemented interfaces; a
     * subclass override shadows the superclass declaration and an inherited interface
     * {@code default}, and a real method wins over a synthetic bridge) that matches
     * {@code filter}, each wrapped as a {@link MethodInvoker}.
     */
    public Stream<MethodInvoker<?>> getMethods(Class<?> clazz, Predicate<Method> filter) {
        Map<String, Method> unique = new LinkedHashMap<>();
        for (Class<?> type : TypeHierarchy.searchOrder(clazz)) {
            // Inherited interfaces contribute only their concrete default methods: a static
            // interface method is not inherited, and an abstract one is implemented somewhere on
            // the class chain (already scanned). The target type itself keeps full visibility.
            boolean defaultsOnly = type != clazz && type.isInterface();
            for (Method method : type.getDeclaredMethods()) {
                if (defaultsOnly && !method.isDefault()) {
                    continue;
                }
                String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                Method existing = unique.get(signature);
                // Nearest declaration wins (the class tier precedes interfaces); a real method
                // always wins over a synthetic bridge of the same erased signature (a covariant
                // override emits both).
                if (existing == null || (isBridgeLike(existing) && !isBridgeLike(method))) {
                    unique.put(signature, method);
                }
            }
        }
        return unique.values().stream()
                .filter(filter)
                .map(method -> (MethodInvoker<?>) new HandleMethodInvoker<>(method));
    }

    public Stream<MethodInvoker<?>> getMethods(String className, Predicate<Method> filter) {
        return getMethods(ClassReflection.INSTANCE.getClass(className), filter);
    }

    private static Method resolve(Class<?> clazz, String name, Class<?> returnType, Class<?>[] params) {
        Method exactBridge = null;
        Method compatibleReal = null;
        Method compatibleBridge = null;
        for (Class<?> type : TypeHierarchy.searchOrder(clazz)) {
            // Inherited interfaces contribute only their concrete default methods (a static
            // interface method is not inherited; an abstract one is implemented on the class chain
            // already scanned). The target type itself keeps full visibility.
            boolean defaultsOnly = type != clazz && type.isInterface();
            List<Method> byArity = Arrays.stream(type.getDeclaredMethods())
                    .sorted(Comparator.comparingInt(method -> method.getParameterTypes().length))
                    .collect(Collectors.toList());
            for (Method method : byArity) {
                if (name != null && !method.getName().equals(name)) {
                    continue;
                }
                if (defaultsOnly && !method.isDefault()) {
                    continue;
                }
                boolean bridge = isBridgeLike(method);
                boolean exactReturn = returnType == null || method.getReturnType().equals(returnType);
                boolean exactParams = params.length == 0 || Arrays.equals(method.getParameterTypes(), params);
                if (exactReturn && exactParams) {
                    if (!bridge) {
                        return method; // best: exact, real method
                    }
                    if (exactBridge == null) {
                        exactBridge = method;
                    }
                    continue;
                }
                boolean compatibleReturn = returnType == null || TypeMatching.isCompatible(method.getReturnType(), returnType);
                boolean compatibleParams = params.length == 0 || TypeMatching.parametersMatch(method.getParameterTypes(), params);
                if (compatibleReturn && compatibleParams) {
                    if (!bridge) {
                        if (compatibleReal == null) {
                            compatibleReal = method;
                        }
                    } else if (compatibleBridge == null) {
                        compatibleBridge = method;
                    }
                }
            }
        }
        if (exactBridge != null) {
            return exactBridge;
        }
        if (compatibleReal != null) {
            return compatibleReal;
        }
        return compatibleBridge;
    }

    private static boolean isBridgeLike(Method method) {
        return method.isBridge() || method.isSynthetic();
    }

    private static final Class<?>[] EMPTY = new Class<?>[0];
}
