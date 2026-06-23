package br.com.finalcraft.everylibs.reflection.lookup;

import jakarta.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentHashMap;

import br.com.finalcraft.everylibs.reflection.internal.ReflectionCache;

/**
 * Class-name resolution with a name-&gt;{@link Class} cache.
 * <p>
 * {@link #getClass(String)} returns the class, or {@code null} if it cannot be found — a miss is
 * not an exception. A class that is present but fails to <em>link</em> (e.g. a moved dependency on
 * this server version) is also treated as a miss (returns {@code null}) rather than escaping as an
 * {@link Error}.
 * <p>
 * The single-argument lookup uses {@link Class#forName(String)} (initializing, via the module's
 * loader) and caches through {@link WeakReference} so a class from a dead reload generation is
 * re-resolved. The loader overload uses {@link Class#forName(String, boolean, ClassLoader)} with
 * {@code initialize=false} (no {@code <clinit>}), sees other plugins' classes, and is not cached.
 * <p>
 * A stateless singleton reached through {@code FCReflectionUtil.classes()} or {@link #INSTANCE}.
 */
public final class ClassReflection {

    public static final ClassReflection INSTANCE = new ClassReflection();

    private ClassReflection() {
    }

    /**
     * @return the class for {@code lookupName}, or {@code null} if absent or unlinkable on this runtime.
     */
    @Nullable
    public Class<?> getClass(String lookupName) {
        Class<?> cached = peek(lookupName);
        if (cached != null) {
            return cached;
        }
        try {
            return cache(lookupName, Class.forName(lookupName));
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /**
     * Resolve a class through a specific {@link ClassLoader} without initializing it (no
     * {@code <clinit>}). Useful for probing NMS/OBC types from the caller's plugin loader.
     *
     * @return the class, or {@code null} if absent or unlinkable through {@code loader}.
     */
    @Nullable
    public Class<?> getClass(String lookupName, ClassLoader loader) {
        try {
            return Class.forName(lookupName, false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /**
     * Try each candidate name through {@code loader} and return the first that resolves — the
     * usual cross-version NMS probe ("try these names, take the one that exists on this server").
     *
     * @return the first resolvable class, or {@code null} if none resolves.
     */
    @Nullable
    public Class<?> getFirstClass(ClassLoader loader, String... candidateNames) {
        for (String candidate : candidateNames) {
            Class<?> resolved = getClass(candidate, loader);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    /**
     * Try each candidate name through the module loader (initializing) and return the first hit.
     *
     * @return the first resolvable class, or {@code null} if none resolves.
     */
    @Nullable
    public Class<?> getFirstClass(String... candidateNames) {
        for (String candidate : candidateNames) {
            Class<?> resolved = getClass(candidate);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    /**
     * Retrieve a class without knowing its type at compile time (e.g. an NMS/OBC type).
     *
     * @return the class typed to the caller's expected {@code T} via erasure, or {@code null} if absent.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> Class<T> getUntypedClass(String lookupName) {
        return (Class<T>) getClass(lookupName);
    }

    /**
     * @return {@code true} if the named class can be loaded (and is linkable) right now.
     */
    public boolean isClassLoaded(String name) {
        try {
            return Class.forName(name) != null;
        } catch (ClassNotFoundException | LinkageError ignored) {
            // LinkageError covers NoClassDefFoundError (present but unlinkable on this runtime).
            return false;
        }
    }

    private static Class<?> peek(String name) {
        ConcurrentHashMap<String, WeakReference<Class<?>>> cache = ReflectionCache.classes();
        WeakReference<Class<?>> ref = cache.get(name);
        if (ref == null) {
            return null;
        }
        Class<?> clazz = ref.get();
        if (clazz == null) {
            // The cached class (and its classloader) was collected — re-resolve to the live generation.
            cache.remove(name, ref);
            return null;
        }
        return clazz;
    }

    private static Class<?> cache(String name, Class<?> clazz) {
        ReflectionCache.classes().put(name, new WeakReference<>(clazz));
        return clazz;
    }
}
