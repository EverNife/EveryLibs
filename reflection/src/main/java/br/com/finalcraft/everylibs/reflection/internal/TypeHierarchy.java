package br.com.finalcraft.everylibs.reflection.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the ordered list of types to scan for a member lookup: the whole superclass chain first
 * (nearest first, up to and including {@link Object}), then every implemented interface in
 * breadth-first order (nearest first), each type listed once.
 * <p>
 * The class tier is emitted entirely before the interface tier on purpose: a member declared on the
 * class (or a superclass) must shadow an interface {@code default} of the same name, mirroring
 * Java's own resolution. The interface tier is what lets a lookup see {@code default} methods and
 * interface constants that a superclass-only walk would miss — while breadth-first ordering makes a
 * nearer (more specific) interface win over a farther one.
 * <p>
 * Internal helper (not part of the published contract).
 */
public final class TypeHierarchy {

    private TypeHierarchy() {
    }

    /**
     * @return the search order for {@code start}: its superclass chain (including {@code Object})
     * followed by its interfaces, breadth-first, each type exactly once. Empty when {@code start}
     * is {@code null}.
     */
    public static List<Class<?>> searchOrder(Class<?> start) {
        if (start == null) {
            return Collections.emptyList();
        }
        List<Class<?>> order = new ArrayList<>();
        Set<Class<?>> seen = new HashSet<>();

        // Class tier: superclass chain, nearest first, up to and including Object. Emitted in full
        // before any interface so a class/superclass member wins over an inherited default.
        for (Class<?> type = start; type != null; type = type.getSuperclass()) {
            if (seen.add(type)) {
                order.add(type);
            }
        }

        // Interface tier: breadth-first from the interfaces implemented anywhere on the class chain,
        // so the nearest interface to the target is visited first.
        Deque<Class<?>> queue = new ArrayDeque<>();
        for (Class<?> type = start; type != null; type = type.getSuperclass()) {
            for (Class<?> itf : type.getInterfaces()) {
                if (seen.add(itf)) {
                    order.add(itf);
                    queue.add(itf);
                }
            }
        }
        while (!queue.isEmpty()) {
            for (Class<?> itf : queue.poll().getInterfaces()) {
                if (seen.add(itf)) {
                    order.add(itf);
                    queue.add(itf);
                }
            }
        }
        return order;
    }
}
