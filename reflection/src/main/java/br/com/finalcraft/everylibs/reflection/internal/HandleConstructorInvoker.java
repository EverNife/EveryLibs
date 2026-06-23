package br.com.finalcraft.everylibs.reflection.internal;

import br.com.finalcraft.everylibs.reflection.ConstructorInvoker;
import br.com.finalcraft.everylibs.reflection.ReflectionException;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * The single {@link ConstructorInvoker} implementation, backing construction with a bound
 * generic spreader {@link MethodHandle} and falling back to reflective
 * {@link Constructor#newInstance} if the constructor cannot be unreflected.
 * <p>
 * Internal helper (not part of the published contract). Throws {@link ReflectionException}
 * instead of a raw {@code RuntimeException}.
 */
public final class HandleConstructorInvoker<T> implements ConstructorInvoker<T> {

    private final Constructor<T> constructor;
    private final int parameterCount;
    private final MethodHandle spreader;

    public HandleConstructorInvoker(Constructor<T> constructor) {
        this.constructor = ReflectionSupport.makeAccessible(constructor);
        this.parameterCount = constructor.getParameterCount();
        MethodHandle spreaderHandle = null;
        try {
            MethodHandle handle = ReflectionSupport.unreflectConstructor(constructor);
            spreaderHandle = handle.asType(handle.type().generic())
                    .asSpreader(Object[].class, handle.type().parameterCount());
        } catch (RuntimeException ignored) {
            // Unreflect/spreader blocked; the reflective fallback below still serves accessible constructors.
        }
        this.spreader = spreaderHandle;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T newInstance(Object... arguments) {
        Object[] args = arguments == null ? EMPTY : arguments;
        if (args.length != parameterCount) {
            throw new ReflectionException(String.format(
                    "Constructor %s expects %d argument(s) but got %d.", constructor, parameterCount, args.length));
        }
        try {
            if (spreader != null) {
                return (T) spreader.invoke(args);
            }
            return constructor.newInstance(args);
        } catch (InvocationTargetException e) {
            // The reflective fallback wraps a thrown body in InvocationTargetException;
            // unwrap so the reported cause matches the MethodHandle path.
            Throwable cause = e.getCause();
            throw ReflectionException.invocationFailed(constructor, cause != null ? cause : e);
        } catch (Throwable e) {
            throw ReflectionException.invocationFailed(constructor, e);
        }
    }

    @Override
    public Constructor<T> getConstructor() {
        return constructor;
    }

    private static final Object[] EMPTY = new Object[0];
}
