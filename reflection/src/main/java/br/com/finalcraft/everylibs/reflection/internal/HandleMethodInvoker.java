package br.com.finalcraft.everylibs.reflection.internal;

import br.com.finalcraft.everylibs.reflection.MethodInvoker;
import br.com.finalcraft.everylibs.reflection.ReflectionException;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * The single {@link MethodInvoker} implementation, backing invocation with a bound
 * {@link MethodHandle} and falling back to reflective {@link Method} invocation if the
 * method cannot be unreflected.
 * <p>
 * Internal helper (not part of the published contract). The handle is specialized once at
 * construction into a generic spreader ({@code (Object[])Object}) so each call skips the
 * per-call {@code asType} adapter and array build that {@code invokeWithArguments} pays.
 * Arguments pass straight through, position for position, with no implicit array re-wrapping
 * (the fixed-arity handle maps a single array argument onto a single array parameter).
 */
public final class HandleMethodInvoker<T> implements MethodInvoker<T> {

    private final Method method;
    private final boolean isStatic;
    private final int parameterCount;
    private final MethodHandle spreader;

    public HandleMethodInvoker(Method method) {
        this.method = ReflectionSupport.makeAccessible(method);
        this.isStatic = Modifier.isStatic(method.getModifiers());
        this.parameterCount = method.getParameterCount();
        MethodHandle spreaderHandle = null;
        try {
            MethodHandle handle = ReflectionSupport.unreflect(method);
            spreaderHandle = handle.asType(handle.type().generic())
                    .asSpreader(Object[].class, handle.type().parameterCount());
        } catch (RuntimeException ignored) {
            // Unreflect/spreader blocked; the reflective fallback below still serves accessible methods.
        }
        this.spreader = spreaderHandle;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T invoke(Object target, Object... arguments) {
        Object[] args = arguments == null ? EMPTY : arguments;
        if (args.length != parameterCount) {
            throw new ReflectionException(String.format(
                    "Method '%s' expects %d argument(s) but got %d.", method.getName(), parameterCount, args.length));
        }
        try {
            if (spreader != null) {
                Object[] all = isStatic ? args : prepend(target, args);
                return (T) spreader.invoke(all);
            }
            return (T) method.invoke(target, args);
        } catch (InvocationTargetException e) {
            // The reflective fallback wraps a thrown body in InvocationTargetException;
            // unwrap so the reported cause matches the MethodHandle path.
            Throwable cause = e.getCause();
            throw ReflectionException.invocationFailed(method, cause != null ? cause : e);
        } catch (Throwable e) {
            throw ReflectionException.invocationFailed(method, e);
        }
    }

    @Override
    public Method getMethod() {
        return method;
    }

    @Override
    public boolean isStatic() {
        // A method's modifiers are fixed at class load; cache the precomputed value instead of
        // recomputing Modifier.isStatic on every call.
        return isStatic;
    }

    private static final Object[] EMPTY = new Object[0];

    private static Object[] prepend(Object first, Object[] rest) {
        Object[] full = new Object[rest.length + 1];
        full[0] = first;
        System.arraycopy(rest, 0, full, 1, rest.length);
        return full;
    }
}
