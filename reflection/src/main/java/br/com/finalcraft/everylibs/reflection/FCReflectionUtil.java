package br.com.finalcraft.everylibs.reflection;

import br.com.finalcraft.everylibs.reflection.internal.ReflectionCache;
import br.com.finalcraft.everylibs.reflection.lookup.AnnotationReflection;
import br.com.finalcraft.everylibs.reflection.lookup.ClassReflection;
import br.com.finalcraft.everylibs.reflection.lookup.ConstructorReflection;
import br.com.finalcraft.everylibs.reflection.lookup.FieldReflection;
import br.com.finalcraft.everylibs.reflection.lookup.MethodReflection;

/**
 * The single entry point into the reflection module: a tree of per-member lookups.
 * <p>
 * Each accessor returns a stateless singleton so the whole API is reachable from one
 * import, member-type first:
 * <pre>{@code
 *   FCReflectionUtil.methods().getMethod(Foo.class, "bar", int.class);
 *   FCReflectionUtil.fields().getField(Foo.class, "name", String.class);
 *   FCReflectionUtil.classes().getClass("net.minecraft.server.Foo");
 * }</pre>
 * For a class-first fluent style, use {@link #of(Class)} (a {@link ClassReflect}
 * handle): {@code FCReflectionUtil.of(Foo.class).method("bar", int.class)}.
 */
public final class FCReflectionUtil {

    private FCReflectionUtil() {
    }

    public static FieldReflection fields() {
        return FieldReflection.INSTANCE;
    }

    public static MethodReflection methods() {
        return MethodReflection.INSTANCE;
    }

    public static ConstructorReflection constructors() {
        return ConstructorReflection.INSTANCE;
    }

    public static ClassReflection classes() {
        return ClassReflection.INSTANCE;
    }

    public static AnnotationReflection annotations() {
        return AnnotationReflection.INSTANCE;
    }

    /**
     * @return a class-first fluent handle bound to {@code target}, carrying its type.
     */
    public static <C> ClassReflect<C> of(Class<C> target) {
        return ClassReflect.of(target);
    }

    /**
     * @return a class-first fluent handle bound to the named class (untyped), or {@code null} if
     * the class cannot be resolved.
     */
    @jakarta.annotation.Nullable
    public static ClassReflect<?> of(String className) {
        return ClassReflect.of(className);
    }

    /**
     * Drop every cached lookup. Call on classloader churn (e.g. plugin reload) to
     * avoid pinning stale {@link Class} references.
     */
    public static void clearCache() {
        ReflectionCache.clear();
    }
}
