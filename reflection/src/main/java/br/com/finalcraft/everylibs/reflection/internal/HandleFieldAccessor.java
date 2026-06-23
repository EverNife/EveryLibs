package br.com.finalcraft.everylibs.reflection.internal;

import br.com.finalcraft.everylibs.reflection.FieldAccessor;
import br.com.finalcraft.everylibs.reflection.ReflectionException;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * The single {@link FieldAccessor} implementation, backing get/set with bound
 * {@link MethodHandle}s and falling back to reflective {@link Field} access if the
 * field cannot be unreflected.
 * <p>
 * Internal helper (not part of the published contract). Replaces the two identical
 * anonymous {@code FieldAccessor} bodies of the old god class.
 */
public final class HandleFieldAccessor<T> implements FieldAccessor<T> {

    private final Field field;
    private final boolean isStatic;
    private final MethodHandle getter;
    private final MethodHandle setter;

    public HandleFieldAccessor(Field field) {
        this.field = ReflectionSupport.makeAccessible(field);
        this.isStatic = Modifier.isStatic(field.getModifiers());
        MethodHandle getterHandle = null;
        MethodHandle setterHandle = null;
        try {
            getterHandle = ReflectionSupport.unreflectGetter(field);
            setterHandle = ReflectionSupport.unreflectSetter(field);
        } catch (ReflectionException ignored) {
            // Unreflect blocked; the reflective fallback below still serves accessible fields.
        }
        this.getter = getterHandle;
        this.setter = setterHandle;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T get(Object target) {
        try {
            if (getter != null) {
                return (T) (isStatic ? getter.invokeWithArguments() : getter.invokeWithArguments(target));
            }
            return (T) field.get(target);
        } catch (Throwable e) {
            throw ReflectionException.invocationFailed(field, e);
        }
    }

    @Override
    public void set(Object target, T value) {
        Class<?> fieldType = field.getType();
        if (value == null) {
            if (fieldType.isPrimitive()) {
                throw new ReflectionException(String.format(
                        "Field '%s' is primitive %s and cannot be set to null.", field.getName(), fieldType.getName()));
            }
        } else if (!fieldType.isPrimitive() && !fieldType.isInstance(value)) {
            throw new ReflectionException(String.format(
                    "Field '%s' expects %s but got %s.", field.getName(), fieldType.getName(), value.getClass().getName()));
        }
        try {
            if (setter != null) {
                if (isStatic) {
                    setter.invokeWithArguments(value);
                } else {
                    setter.invokeWithArguments(target, value);
                }
            } else {
                field.set(target, value);
            }
        } catch (Throwable e) {
            throw ReflectionException.invocationFailed(field, e);
        }
    }

    @Override
    public boolean hasField(Object target) {
        return target != null && field.getDeclaringClass().isAssignableFrom(target.getClass());
    }

    @Override
    public Field getField() {
        return field;
    }

    @Override
    public boolean isStatic() {
        // A field's modifiers are fixed at class load; cache the precomputed value instead of
        // recomputing Modifier.isStatic on every call.
        return isStatic;
    }
}
