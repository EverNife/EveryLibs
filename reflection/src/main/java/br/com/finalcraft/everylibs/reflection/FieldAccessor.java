package br.com.finalcraft.everylibs.reflection;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * A typed read/write handle over a single resolved {@link Field}.
 *
 * @param <T> the field type.
 */
public interface FieldAccessor<T> {

    /**
     * Retrieve the content of the field.
     *
     * @param target the target object, or {@code null} for a static field.
     * @return the value of the field.
     */
    T get(Object target);

    /**
     * Set the content of the field.
     *
     * @param target the target object, or {@code null} for a static field.
     * @param value  the new value of the field.
     */
    void set(Object target, T value);

    /**
     * Determine if the given object carries this field (the field's declaring
     * class is assignable from the target's class).
     *
     * @param target the object to test.
     * @return {@code true} if it does.
     */
    boolean hasField(Object target);

    /**
     * @return the backing {@link Field}.
     */
    Field getField();

    /**
     * @return {@code true} if the backing field is static (no instance needed).
     */
    default boolean isStatic() {
        return Modifier.isStatic(getField().getModifiers());
    }

    /**
     * Read a static field (no target needed).
     *
     * @throws ReflectionException if the field is not static.
     */
    default T get() {
        if (!isStatic()) {
            throw new ReflectionException("Field '" + getField().getName() + "' is not static; pass a target instance to get(target).");
        }
        return get(null);
    }

    /**
     * Write a static field (no target needed).
     *
     * @throws ReflectionException if the field is not static.
     */
    default void set(T value) {
        if (!isStatic()) {
            throw new ReflectionException("Field '" + getField().getName() + "' is not static; pass a target instance to set(target, value).");
        }
        set(null, value);
    }
}
