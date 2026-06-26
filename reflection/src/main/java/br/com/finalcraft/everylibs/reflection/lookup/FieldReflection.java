package br.com.finalcraft.everylibs.reflection.lookup;

import br.com.finalcraft.everylibs.reflection.FieldAccessor;
import br.com.finalcraft.everylibs.reflection.internal.HandleFieldAccessor;
import br.com.finalcraft.everylibs.reflection.internal.MemberKey;
import br.com.finalcraft.everylibs.reflection.internal.ReflectionCache;
import br.com.finalcraft.everylibs.reflection.internal.TypeHierarchy;
import jakarta.annotation.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Field lookup. Every {@code getField} returns the accessor, or {@code null} if no matching
 * field exists — a miss is not an exception. The search walks up the superclass chain and then
 * across implemented interfaces (so interface constants are found), and every resolved accessor is
 * cached.
 * <p>
 * A stateless singleton reached through {@code FCReflectionUtil.fields()} or {@link #INSTANCE}.
 */
public final class FieldReflection {

    public static final FieldReflection INSTANCE = new FieldReflection();

    private FieldReflection() {
    }

    /**
     * Find a field by an optional name and/or compatible type, skipping {@code index} earlier
     * matches, walking up the superclass chain and then across implemented interfaces.
     *
     * @return the accessor, or {@code null} if none matches.
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType, int index) {
        ReflectionCache.PerClassCache cache = ReflectionCache.forOwner(target);
        MemberKey key = new MemberKey(name, fieldType, null, index);
        FieldAccessor<?> accessor = ReflectionCache.resolve(
                cache.fields(), cache.negativeFields(), key, () -> resolve(target, name, fieldType, index));
        return (FieldAccessor<T>) accessor;
    }

    @Nullable
    public <T> FieldAccessor<T> getField(Class<?> target, String name) {
        return getField(target, name, null, 0);
    }

    @Nullable
    public <T> FieldAccessor<T> getField(Class<?> target, String name, Class<T> fieldType) {
        return getField(target, name, fieldType, 0);
    }

    @Nullable
    public <T> FieldAccessor<T> getField(Class<?> target, Class<T> fieldType, int index) {
        return getField(target, null, fieldType, index);
    }

    @Nullable
    public <T> FieldAccessor<T> getField(String className, String name, Class<T> fieldType) {
        Class<?> target = ClassReflection.INSTANCE.getClass(className);
        return target == null ? null : getField(target, name, fieldType);
    }

    @Nullable
    public <T> FieldAccessor<T> getField(String className, Class<T> fieldType, int index) {
        Class<?> target = ClassReflection.INSTANCE.getClass(className);
        return target == null ? null : getField(target, fieldType, index);
    }

    /**
     * List every field of {@code target} in declaration order, optionally including inherited
     * fields (superclasses nearest first, then interface constants). Eagerly builds an accessor for
     * each field; use {@link #fieldWalker(Class, boolean)} to traverse lazily and stop early.
     */
    public List<FieldAccessor<?>> getAllFields(Class<?> target, boolean includeInherited) {
        List<FieldAccessor<?>> accessors = new ArrayList<>();
        Iterator<FieldAccessor<?>> walker = fieldWalker(target, includeInherited);
        while (walker.hasNext()) {
            accessors.add(walker.next());
        }
        return accessors;
    }

    /**
     * A lazy iterator over the fields of {@code target} in declaration order. An accessor is built
     * only when {@link Iterator#next()} is called, so a caller can stop at any point without paying
     * to wrap the remaining fields.
     *
     * @param includeInherited {@code true} to continue into superclasses (nearest first) and then
     *                         interface constants; {@code false} to walk only {@code target}'s
     *                         declared fields.
     */
    public Iterator<FieldAccessor<?>> fieldWalker(Class<?> target, boolean includeInherited) {
        List<Class<?>> types = includeInherited
                ? TypeHierarchy.searchOrder(target)
                : (target == null ? Collections.<Class<?>>emptyList() : Collections.<Class<?>>singletonList(target));
        return new Iterator<FieldAccessor<?>>() {
            private final Iterator<Class<?>> typeIterator = types.iterator();
            private Field[] declared = EMPTY_FIELDS;
            private int index = 0;

            @Override
            public boolean hasNext() {
                while (index >= declared.length) {
                    if (!typeIterator.hasNext()) {
                        return false;
                    }
                    declared = typeIterator.next().getDeclaredFields();
                    index = 0;
                }
                return true;
            }

            @Override
            public FieldAccessor<?> next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return new HandleFieldAccessor<>(declared[index++]);
            }
        };
    }

    private static FieldAccessor<?> resolve(Class<?> target, String name, Class<?> fieldType, int index) {
        int remaining = index;
        for (Class<?> type : TypeHierarchy.searchOrder(target)) {
            for (Field field : type.getDeclaredFields()) {
                if ((name == null || field.getName().equals(name))
                        && (fieldType == null || fieldType.isAssignableFrom(field.getType()))) {
                    if (remaining <= 0) {
                        return new HandleFieldAccessor<>(field);
                    }
                    remaining--;
                }
            }
        }
        return null;
    }

    private static final Field[] EMPTY_FIELDS = new Field[0];
}
