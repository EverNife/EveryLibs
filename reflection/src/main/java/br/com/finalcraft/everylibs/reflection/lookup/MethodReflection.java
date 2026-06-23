package br.com.finalcraft.everylibs.reflection.lookup;

import br.com.finalcraft.everylibs.reflection.MethodInvoker;
import br.com.finalcraft.everylibs.reflection.internal.HandleMethodInvoker;
import br.com.finalcraft.everylibs.reflection.internal.MemberKey;
import br.com.finalcraft.everylibs.reflection.internal.ReflectionCache;
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
 * preserved as the search walks up the superclass chain. An empty {@code params}
 * array means "ignore parameters", matching the first method of the given name
 * (fewest parameters first). An <em>exact</em> parameter/return-type match always
 * wins; a primitive/wrapper-compatible match is used only when no exact match exists,
 * so existing overload selection never changes. A real method wins over a synthetic
 * bridge of the same signature.
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
     * superclass chain; a subclass override shadows the superclass declaration, and a
     * real method wins over a synthetic bridge) that matches {@code filter}, each
     * wrapped as a {@link MethodInvoker}.
     */
    public Stream<MethodInvoker<?>> getMethods(Class<?> clazz, Predicate<Method> filter) {
        Map<String, Method> unique = new LinkedHashMap<>();
        Class<?> current = clazz;
        while (current != null) {
            for (Method method : current.getDeclaredMethods()) {
                String signature = method.getName() + Arrays.toString(method.getParameterTypes());
                Method existing = unique.get(signature);
                // Nearest declaration wins; a real method always wins over a synthetic
                // bridge of the same erased signature (a covariant override emits both).
                if (existing == null || (isBridgeLike(existing) && !isBridgeLike(method))) {
                    unique.put(signature, method);
                }
            }
            current = current.getSuperclass();
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
        Class<?> current = clazz;
        while (current != null) {
            List<Method> byArity = Arrays.stream(current.getDeclaredMethods())
                    .sorted(Comparator.comparingInt(method -> method.getParameterTypes().length))
                    .collect(Collectors.toList());
            for (Method method : byArity) {
                if (name != null && !method.getName().equals(name)) {
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
            current = current.getSuperclass();
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
