package br.com.finalcraft.everylibs.reflection;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * A typed invocation handle over a single resolved {@link Method}.
 * <p>
 * Arguments are passed straight through to the underlying method, position for
 * position, with no implicit re-wrapping: to call a method whose sole parameter
 * is an array, pass that array as the single argument.
 *
 * @param <T> the method return type.
 */
public interface MethodInvoker<T> {

    /**
     * Invoke the method on a specific target object.
     *
     * @param target    the target object, or {@code null} for a static method.
     * @param arguments the arguments to pass, one per declared parameter.
     * @return the return value, or {@code null} for a {@code void} method.
     */
    T invoke(Object target, Object... arguments);

    /**
     * @return the backing {@link Method}.
     */
    Method getMethod();

    /**
     * @return {@code true} if the backing method is static (no instance needed).
     */
    default boolean isStatic() {
        return Modifier.isStatic(getMethod().getModifiers());
    }

    /**
     * Invoke a static method (no target needed).
     *
     * @throws ReflectionException if the method is not static.
     */
    default T invokeStatic(Object... arguments) {
        if (!isStatic()) {
            throw new ReflectionException("Method '" + getMethod().getName() + "' is not static; pass a target instance to invoke(target, args).");
        }
        return invoke(null, arguments);
    }
}
