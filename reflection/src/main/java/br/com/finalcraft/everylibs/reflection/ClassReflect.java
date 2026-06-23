package br.com.finalcraft.everylibs.reflection;

import br.com.finalcraft.everylibs.reflection.lookup.AnnotationReflection;
import br.com.finalcraft.everylibs.reflection.lookup.ClassReflection;
import br.com.finalcraft.everylibs.reflection.lookup.ConstructorReflection;
import br.com.finalcraft.everylibs.reflection.lookup.FieldReflection;
import br.com.finalcraft.everylibs.reflection.lookup.MethodReflection;
import jakarta.annotation.Nullable;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A fluent, discoverable handle bound to one target {@link Class}.
 * <p>
 * Purely an ergonomic delegate over the per-member lookups ({@link FieldReflection},
 * {@link MethodReflection}, {@link ConstructorReflection}, {@link AnnotationReflection}); it holds
 * no lookup logic of its own. Every lookup returns the resolved handle or {@code null} (a miss is
 * not an exception).
 * <p>
 * The handle carries the target's type as {@code <C>}, so {@link #constructor(Class[])} returns a
 * sound {@link ConstructorInvoker} of {@code C} with no casts. Use {@link #of(String)} for the
 * untyped (NMS) case.
 *
 * @param <C> the target class's type.
 */
public final class ClassReflect<C> {

    private final Class<C> target;

    private ClassReflect(Class<C> target) {
        this.target = target;
    }

    public static <C> ClassReflect<C> of(Class<C> target) {
        return new ClassReflect<>(target);
    }

    /**
     * @return a handle bound to the named class, or {@code null} if the class cannot be resolved.
     */
    @Nullable
    public static ClassReflect<?> of(String className) {
        Class<?> target = ClassReflection.INSTANCE.getClass(className);
        return target == null ? null : ofUnknown(target);
    }

    @SuppressWarnings("unchecked")
    private static ClassReflect<?> ofUnknown(Class<?> target) {
        return new ClassReflect<>((Class<Object>) target);
    }

    public Class<C> target() {
        return target;
    }

    @Nullable
    public <T> FieldAccessor<T> field(String name) {
        return FieldReflection.INSTANCE.getField(target, name);
    }

    @Nullable
    public <T> FieldAccessor<T> field(String name, Class<T> fieldType) {
        return FieldReflection.INSTANCE.getField(target, name, fieldType);
    }

    @Nullable
    public <T> FieldAccessor<T> field(Class<T> fieldType, int index) {
        return FieldReflection.INSTANCE.getField(target, fieldType, index);
    }

    public List<FieldAccessor<?>> fields(boolean includeInherited) {
        return FieldReflection.INSTANCE.getAllFields(target, includeInherited);
    }

    public Iterator<FieldAccessor<?>> fieldWalker(boolean includeInherited) {
        return FieldReflection.INSTANCE.fieldWalker(target, includeInherited);
    }

    @Nullable
    public <T> MethodInvoker<T> method(String name, Class<?>... params) {
        return MethodReflection.INSTANCE.getMethod(target, name, params);
    }

    @Nullable
    public <R> MethodInvoker<R> typedMethod(String name, Class<R> returnType, Class<?>... params) {
        return MethodReflection.INSTANCE.getTypedMethod(target, name, returnType, params);
    }

    public Stream<MethodInvoker<?>> methods(Predicate<Method> filter) {
        return MethodReflection.INSTANCE.getMethods(target, filter);
    }

    @Nullable
    public ConstructorInvoker<C> constructor(Class<?>... params) {
        return ConstructorReflection.INSTANCE.getConstructor(target, params);
    }

    @Nullable
    public <A extends Annotation> A annotation(Class<A> type) {
        return AnnotationReflection.INSTANCE.getAnnotationDeeply(target, type);
    }
}
