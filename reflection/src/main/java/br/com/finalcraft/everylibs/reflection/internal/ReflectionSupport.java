package br.com.finalcraft.everylibs.reflection.internal;

import br.com.finalcraft.everylibs.reflection.ReflectionException;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Accessibility and {@link MethodHandle} helper (a {@code makeAccessible} analog).
 * <p>
 * Internal helper (not part of the published contract). Centralizes
 * {@code setAccessible(true)} and unreflects members into bound method handles
 * through a single shared {@link MethodHandles#lookup()}. Setting the accessible
 * flag before unreflecting bypasses access checks, so private members resolve on
 * the Java 8 API floor without {@code privateLookupIn} (Java 9).
 * <p>
 * Checked {@link ReflectiveOperationException}s never escape: they are wrapped in
 * {@link ReflectionException}.
 */
public final class ReflectionSupport {

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private ReflectionSupport() {
    }

    public static <M extends AccessibleObject> M makeAccessible(M member) {
        try {
            member.setAccessible(true);
        } catch (RuntimeException ignored) {
            // Access stays as-is; the reflective fallback in each invoker still works
            // for accessible members, and unreflect will surface a clear error otherwise.
        }
        return member;
    }

    public static MethodHandle unreflectGetter(Field field) {
        try {
            return LOOKUP.unreflectGetter(makeAccessible(field));
        } catch (IllegalAccessException e) {
            throw ReflectionException.accessDenied(field, e);
        }
    }

    public static MethodHandle unreflectSetter(Field field) {
        try {
            return LOOKUP.unreflectSetter(makeAccessible(field));
        } catch (IllegalAccessException e) {
            throw ReflectionException.accessDenied(field, e);
        }
    }

    public static MethodHandle unreflect(Method method) {
        try {
            // Fixed arity: arguments map 1:1 to parameters, with no varargs spreading.
            return LOOKUP.unreflect(makeAccessible(method)).asFixedArity();
        } catch (IllegalAccessException e) {
            throw ReflectionException.accessDenied(method, e);
        }
    }

    public static <T> MethodHandle unreflectConstructor(Constructor<T> constructor) {
        try {
            return LOOKUP.unreflectConstructor(makeAccessible(constructor)).asFixedArity();
        } catch (IllegalAccessException e) {
            throw ReflectionException.accessDenied(constructor, e);
        }
    }
}
