package br.com.finalcraft.everylibs.reflection.lookup;

import br.com.finalcraft.everylibs.reflection.FieldAccessor;
import br.com.finalcraft.everylibs.reflection.MethodInvoker;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Inheritance-aware annotation lookup on methods and classes, scanning helpers, and small
 * parameter introspection helpers.
 * <p>
 * "Deeply" walks the whole type hierarchy — superclasses <em>and</em> interfaces — because
 * marker annotations frequently sit on interfaces (e.g. {@code CraftPlayer} → {@code Player} →
 * {@code HumanEntity}), not just superclasses. A miss returns {@code null}, never an exception.
 * <p>
 * A stateless singleton reached through {@code FCReflectionUtil.annotations()} or {@link #INSTANCE}.
 */
public final class AnnotationReflection {

    public static final AnnotationReflection INSTANCE = new AnnotationReflection();

    private AnnotationReflection() {
    }

    /**
     * Find an annotation on a method, falling back to the same method signature on each
     * superclass and interface (stopping at {@link Object}).
     *
     * @return the annotation, or {@code null} if absent.
     */
    @Nullable
    public <A extends Annotation> A getAnnotationDeeply(Method method, Class<A> annotationType) {
        A direct = method.getAnnotation(annotationType);
        if (direct != null) {
            return direct;
        }
        String name = method.getName();
        Class<?>[] parameterTypes = method.getParameterTypes();
        Deque<Class<?>> queue = new ArrayDeque<>();
        Set<Class<?>> seen = new HashSet<>();
        enqueueSupertypes(method.getDeclaringClass(), queue, seen);
        while (!queue.isEmpty()) {
            Class<?> type = queue.poll();
            try {
                A annotation = type.getDeclaredMethod(name, parameterTypes).getAnnotation(annotationType);
                if (annotation != null) {
                    return annotation;
                }
            } catch (NoSuchMethodException | LinkageError ignored) {
                // This supertype does not redeclare the method (or a signature type cannot link
                // on this runtime); keep walking. A miss is null, not an exception.
            }
            enqueueSupertypes(type, queue, seen);
        }
        return null;
    }

    /**
     * Find an annotation on a class, falling back to its superclasses and interfaces
     * (stopping at {@link Object}).
     *
     * @return the annotation, or {@code null} if absent.
     */
    @Nullable
    public <A extends Annotation> A getAnnotationDeeply(Class<?> clazz, Class<A> annotationType) {
        Deque<Class<?>> queue = new ArrayDeque<>();
        Set<Class<?>> seen = new HashSet<>();
        queue.add(clazz);
        seen.add(clazz);
        while (!queue.isEmpty()) {
            Class<?> type = queue.poll();
            A annotation = type.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
            enqueueSupertypes(type, queue, seen);
        }
        return null;
    }

    /**
     * @return every method of {@code type} (declared across the hierarchy, dedup'd) carrying
     * {@code annotationType} directly or on an overridden supertype method.
     */
    public List<MethodInvoker<?>> methodsAnnotatedWith(Class<?> type, Class<? extends Annotation> annotationType) {
        return MethodReflection.INSTANCE
                .getMethods(type, method -> getAnnotationDeeply(method, annotationType) != null)
                .collect(Collectors.toList());
    }

    /**
     * @return every field of {@code type} (declared, including inherited) carrying {@code annotationType}.
     */
    public List<FieldAccessor<?>> fieldsAnnotatedWith(Class<?> type, Class<? extends Annotation> annotationType) {
        List<FieldAccessor<?>> result = new ArrayList<>();
        for (FieldAccessor<?> accessor : FieldReflection.INSTANCE.getAllFields(type, true)) {
            if (accessor.getField().isAnnotationPresent(annotationType)) {
                result.add(accessor);
            }
        }
        return result;
    }

    /**
     * Find the index of the first parameter matching {@code argType}.
     *
     * @param checkAssignable {@code true} to match assignable subtypes; {@code false} to require an
     *                        exact type match.
     * @return the index, or {@code -1} if no parameter matches.
     */
    public int getArgIndex(Method method, Class<?> argType, boolean checkAssignable) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            boolean matches = checkAssignable ? argType.isAssignableFrom(parameterTypes[i]) : argType == parameterTypes[i];
            if (matches) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return the parameter type at {@code index}, or {@code null} if out of range.
     */
    @Nullable
    public Class<?> getArgAtIndex(Method method, int index) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (index < 0 || index >= parameterTypes.length) {
            return null;
        }
        return parameterTypes[index];
    }

    private static void enqueueSupertypes(Class<?> type, Deque<Class<?>> queue, Set<Class<?>> seen) {
        addType(type.getSuperclass(), queue, seen);
        for (Class<?> itf : type.getInterfaces()) {
            addType(itf, queue, seen);
        }
    }

    private static void addType(Class<?> type, Deque<Class<?>> queue, Set<Class<?>> seen) {
        if (type != null && type != Object.class && seen.add(type)) {
            queue.add(type);
        }
    }
}
