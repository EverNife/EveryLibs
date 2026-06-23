package br.com.finalcraft.everylibs.reflection.lookup;

import br.com.finalcraft.everylibs.reflection.ConstructorInvoker;
import br.com.finalcraft.everylibs.reflection.internal.HandleConstructorInvoker;
import br.com.finalcraft.everylibs.reflection.internal.MemberKey;
import br.com.finalcraft.everylibs.reflection.internal.ReflectionCache;
import br.com.finalcraft.everylibs.reflection.internal.TypeMatching;
import jakarta.annotation.Nullable;

import java.lang.reflect.Constructor;

/**
 * Constructor lookup with primitive/wrapper-aware parameter matching.
 * <p>
 * {@link #getConstructor(Class, Class[])} returns an invoker, or {@code null} if no matching
 * constructor exists — a miss is not an exception. Every resolved invoker is cached.
 * <p>
 * A stateless singleton reached through {@code FCReflectionUtil.constructors()} or
 * {@link #INSTANCE}.
 */
public final class ConstructorReflection {

    public static final ConstructorReflection INSTANCE = new ConstructorReflection();

    private ConstructorReflection() {
    }

    /**
     * @return an invoker for the matching constructor (primitive/wrapper-aware), or {@code null}
     * if none matches.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> ConstructorInvoker<T> getConstructor(Class<T> clazz, Class<?>... params) {
        Class<?>[] signature = params == null ? EMPTY : params;
        ReflectionCache.PerClassCache cache = ReflectionCache.forOwner(clazz);
        MemberKey key = new MemberKey(null, null, signature, 0);
        ConstructorInvoker<?> invoker = ReflectionCache.resolve(cache.constructors(), cache.negativeConstructors(), key, () -> {
            Constructor<T> constructor = resolve(clazz, signature);
            return constructor == null ? null : new HandleConstructorInvoker<>(constructor);
        });
        return (ConstructorInvoker<T>) invoker;
    }

    @Nullable
    public ConstructorInvoker<Object> getConstructor(String className, Class<?>... params) {
        Class<Object> clazz = ClassReflection.INSTANCE.getUntypedClass(className);
        return clazz == null ? null : getConstructor(clazz, params);
    }

    @SuppressWarnings("unchecked")
    private static <T> Constructor<T> resolve(Class<T> clazz, Class<?>[] params) {
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (TypeMatching.parametersMatch(constructor.getParameterTypes(), params)) {
                return (Constructor<T>) constructor;
            }
        }
        return null;
    }

    private static final Class<?>[] EMPTY = new Class<?>[0];
}
