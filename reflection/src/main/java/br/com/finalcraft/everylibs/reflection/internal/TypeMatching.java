package br.com.finalcraft.everylibs.reflection.internal;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Primitive/wrapper-aware {@code Class[]} matching.
 * <p>
 * Internal helper (not part of the published contract). Extracted from the old
 * {@code arrayEqualsIgnorePrimitives}, but it resolves the primitive of a wrapper
 * through a static table instead of bootstrapping via reflection on the {@code TYPE}
 * field.
 */
public final class TypeMatching {

    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE;
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER;

    static {
        Map<Class<?>, Class<?>> wrapperToPrimitive = new HashMap<>();
        wrapperToPrimitive.put(Boolean.class, boolean.class);
        wrapperToPrimitive.put(Byte.class, byte.class);
        wrapperToPrimitive.put(Character.class, char.class);
        wrapperToPrimitive.put(Short.class, short.class);
        wrapperToPrimitive.put(Integer.class, int.class);
        wrapperToPrimitive.put(Long.class, long.class);
        wrapperToPrimitive.put(Float.class, float.class);
        wrapperToPrimitive.put(Double.class, double.class);
        wrapperToPrimitive.put(Void.class, void.class);

        Map<Class<?>, Class<?>> primitiveToWrapper = new HashMap<>();
        for (Map.Entry<Class<?>, Class<?>> entry : wrapperToPrimitive.entrySet()) {
            primitiveToWrapper.put(entry.getValue(), entry.getKey());
        }

        WRAPPER_TO_PRIMITIVE = Collections.unmodifiableMap(wrapperToPrimitive);
        PRIMITIVE_TO_WRAPPER = Collections.unmodifiableMap(primitiveToWrapper);
    }

    private TypeMatching() {
    }

    /**
     * Compare two arrays of classes, treating a primitive and its wrapper as a match.
     *
     * @param declared  the declared parameter types of a member.
     * @param requested the parameter types asked for by the caller.
     * @return {@code true} if the arrays match element for element.
     */
    public static boolean parametersMatch(Class<?>[] declared, Class<?>[] requested) {
        if (declared == requested) {
            return true;
        }
        if (declared == null || requested == null || declared.length != requested.length) {
            return false;
        }
        for (int i = 0; i < declared.length; i++) {
            if (!isCompatible(declared[i], requested[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return {@code true} if the two classes are equal or are a primitive/wrapper pair.
     */
    public static boolean isCompatible(Class<?> declared, Class<?> requested) {
        if (Objects.equals(declared, requested)) {
            return true;
        }
        if (declared == null || requested == null) {
            return false;
        }
        return Objects.equals(normalize(declared), normalize(requested));
    }

    /**
     * Reduce a type to a canonical form: a wrapper collapses to its primitive.
     * Any other type is returned unchanged.
     */
    public static Class<?> normalize(Class<?> type) {
        if (type == null) {
            return null;
        }
        Class<?> primitive = WRAPPER_TO_PRIMITIVE.get(type);
        return primitive != null ? primitive : type;
    }

    /**
     * @return the wrapper type for a primitive, or the type itself if not primitive.
     */
    public static Class<?> wrap(Class<?> type) {
        if (type == null) {
            return null;
        }
        Class<?> wrapper = PRIMITIVE_TO_WRAPPER.get(type);
        return wrapper != null ? wrapper : type;
    }
}
