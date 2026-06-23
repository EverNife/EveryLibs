package br.com.finalcraft.everylibs.reflection.internal;

import br.com.finalcraft.everylibs.reflection.ConstructorInvoker;
import br.com.finalcraft.everylibs.reflection.FieldAccessor;
import br.com.finalcraft.everylibs.reflection.MethodInvoker;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Caches for resolved lookups.
 * <p>
 * Internal helper (not part of the published contract). Member caches (field/method/constructor)
 * are partitioned per owner {@link Class} through a {@link ClassValue}: the JVM evicts an owner's
 * entry once that {@code Class} and its classloader become unreachable, so a plugin reload does
 * not pin the old classloader. The class-name cache holds {@link WeakReference} values for the
 * same reason. Each partition pairs a positive map (resolved accessor/invoker, which already holds
 * its bound {@link java.lang.invoke.MethodHandle}) with a bounded negative set of keys known to be
 * absent — safe because a loaded class's member set never changes.
 * <p>
 * {@link #clear()} swaps in a fresh {@link ClassValue}, so every existing partition is orphaned at
 * once (any write still in flight lands in the now-unreachable old partition and is collected with
 * it). Use it on classloader churn as an explicit safety net.
 */
public final class ReflectionCache {

    /** Per-owner negative-cache capacity (LRU). Absent members per class are few in practice. */
    private static final int NEGATIVE_CAPACITY = 256;

    private static volatile ClassValue<PerClassCache> perClass = newPartitions();

    private static final ConcurrentHashMap<String, WeakReference<Class<?>>> CLASSES = new ConcurrentHashMap<>();

    private ReflectionCache() {
    }

    private static ClassValue<PerClassCache> newPartitions() {
        return new ClassValue<PerClassCache>() {
            @Override
            protected PerClassCache computeValue(Class<?> type) {
                return new PerClassCache();
            }
        };
    }

    /**
     * @return the cache partition for {@code owner} in the current generation.
     */
    public static PerClassCache forOwner(Class<?> owner) {
        return perClass.get(owner);
    }

    public static ConcurrentHashMap<String, WeakReference<Class<?>>> classes() {
        return CLASSES;
    }

    /**
     * Return the cached value for {@code key}; on a positive miss run {@code resolver} OUTSIDE any
     * map lock and cache the result, and on a hard miss (resolver returns {@code null}) record the
     * key as negative so the next identical lookup returns immediately. Returns {@code null} on a
     * miss.
     */
    public static <V> V resolve(ConcurrentHashMap<MemberKey, V> positive, Set<MemberKey> negative,
                                MemberKey key, Supplier<V> resolver) {
        if (negative.contains(key)) {
            return null;
        }
        V cached = positive.get(key);
        if (cached != null) {
            return cached;
        }
        V resolved = resolver.get();
        if (resolved == null) {
            negative.add(key);
            return null;
        }
        V existing = positive.putIfAbsent(key, resolved);
        return existing != null ? existing : resolved;
    }

    /**
     * Drop every cached lookup. Clears the class-name cache and swaps in a fresh set of per-owner
     * partitions, so stale entries (and any concurrent in-flight write to an old partition) become
     * unreachable rather than surviving into the new generation.
     */
    public static void clear() {
        CLASSES.clear();
        perClass = newPartitions();
    }

    static <K> Set<K> boundedLruSet(int max) {
        return Collections.synchronizedSet(Collections.newSetFromMap(
                new LinkedHashMap<K, Boolean>(64, 0.75f, false) {
                    @Override
                    protected boolean removeEldestEntry(Map.Entry<K, Boolean> eldest) {
                        return size() > max;
                    }
                }));
    }

    /**
     * One owner class's resolved-member caches plus their bounded negative sets.
     */
    public static final class PerClassCache {

        private final ConcurrentHashMap<MemberKey, FieldAccessor<?>> fields = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<MemberKey, MethodInvoker<?>> methods = new ConcurrentHashMap<>();
        private final ConcurrentHashMap<MemberKey, ConstructorInvoker<?>> constructors = new ConcurrentHashMap<>();
        private final Set<MemberKey> negativeFields = boundedLruSet(NEGATIVE_CAPACITY);
        private final Set<MemberKey> negativeMethods = boundedLruSet(NEGATIVE_CAPACITY);
        private final Set<MemberKey> negativeConstructors = boundedLruSet(NEGATIVE_CAPACITY);

        public ConcurrentHashMap<MemberKey, FieldAccessor<?>> fields() {
            return fields;
        }

        public ConcurrentHashMap<MemberKey, MethodInvoker<?>> methods() {
            return methods;
        }

        public ConcurrentHashMap<MemberKey, ConstructorInvoker<?>> constructors() {
            return constructors;
        }

        public Set<MemberKey> negativeFields() {
            return negativeFields;
        }

        public Set<MemberKey> negativeMethods() {
            return negativeMethods;
        }

        public Set<MemberKey> negativeConstructors() {
            return negativeConstructors;
        }
    }
}
