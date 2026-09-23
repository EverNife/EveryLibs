package br.com.finalcraft.everylibs.reflection.lookup;

import jakarta.annotation.Nullable;

/**
 * What resolving one class name found: the class, nothing at all, or a class that is there but
 * cannot be used on this runtime.
 * <p>
 * {@link ClassReflection#getClass(String)} answers the last two with the same {@code null}. That is
 * the right answer for "can I use it?", and the wrong one for "why not?": an absent class is a
 * normal platform difference, while an unlinkable one is a runtime that carries the type and
 * cannot load it - a moved dependency, a remapping classloader, a failed static initializer - and
 * whoever asked usually has to say so instead of taking the "absent" branch in silence.
 */
public final class ClassLookup {

    public enum Outcome {
        /** The class resolved and can be used. */
        FOUND,
        /** No class by that name is reachable from the loader asked. */
        ABSENT,
        /** The class is reachable, but linking or initializing it failed - see {@link #getLinkageError()}. */
        UNLINKABLE
    }

    private final String name;
    private final Outcome outcome;
    private final Class<?> type;
    private final LinkageError linkageError;

    private ClassLookup(String name, Outcome outcome, Class<?> type, LinkageError linkageError) {
        this.name = name;
        this.outcome = outcome;
        this.type = type;
        this.linkageError = linkageError;
    }

    static ClassLookup found(String name, Class<?> type) {
        return new ClassLookup(name, Outcome.FOUND, type, null);
    }

    static ClassLookup absent(String name) {
        return new ClassLookup(name, Outcome.ABSENT, null, null);
    }

    static ClassLookup unlinkable(String name, LinkageError linkageError) {
        return new ClassLookup(name, Outcome.UNLINKABLE, null, linkageError);
    }

    /** @return the class name that was looked up. */
    public String getName() {
        return name;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    /** @return the resolved class, or {@code null} unless the outcome is {@link Outcome#FOUND}. */
    @Nullable
    public Class<?> getType() {
        return type;
    }

    /** @return what the runtime threw, or {@code null} unless the outcome is {@link Outcome#UNLINKABLE}. */
    @Nullable
    public LinkageError getLinkageError() {
        return linkageError;
    }

    public boolean isFound() {
        return outcome == Outcome.FOUND;
    }

    public boolean isAbsent() {
        return outcome == Outcome.ABSENT;
    }

    public boolean isUnlinkable() {
        return outcome == Outcome.UNLINKABLE;
    }

    @Override
    public String toString() {
        return outcome == Outcome.UNLINKABLE
                ? "ClassLookup{" + name + " UNLINKABLE: " + linkageError + "}"
                : "ClassLookup{" + name + " " + outcome + "}";
    }
}
