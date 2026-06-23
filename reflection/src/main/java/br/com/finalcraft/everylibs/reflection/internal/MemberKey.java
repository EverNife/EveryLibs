package br.com.finalcraft.everylibs.reflection.internal;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Immutable, value-equal cache key for a member lookup <em>within a single owner class</em>.
 * <p>
 * Internal helper (not part of the published contract). The owner is no longer part of the
 * key: member caches are now partitioned per owner {@link Class} via {@link ReflectionCache}'s
 * {@link ClassValue}, so the key only needs to distinguish lookups on the same owner — an
 * optional member name, an optional type (return type for methods, field type for fields), the
 * requested parameter signature, and an index. A plain final class (kept off Jabel's record
 * path); equality and hashing come from Lombok, comparing the defensively copied param array
 * element for element.
 */
@Getter
@EqualsAndHashCode
public final class MemberKey {

    private final String name;
    private final Class<?> type;
    private final Class<?>[] params;
    private final int index;

    public MemberKey(String name, Class<?> type, Class<?>[] params, int index) {
        this.name = name;
        this.type = type;
        this.params = params == null ? null : params.clone();
        this.index = index;
    }
}
