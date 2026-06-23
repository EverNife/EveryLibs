package br.com.finalcraft.everylibs.reflection;

import java.lang.reflect.Member;

/**
 * The single unchecked exception thrown across the reflection module.
 * <p>
 * It signals a <em>real</em> failure — access denied, an invoked target that threw, or a
 * misuse (wrong argument count, wrong value type, a static-only call on an instance member).
 * A simple "not found" is <strong>not</strong> an exception: lookups return {@code null}
 * instead.
 */
public class ReflectionException extends RuntimeException {

    public ReflectionException(String message) {
        super(message);
    }

    public ReflectionException(String message, Throwable cause) {
        super(message, cause);
    }

    public static ReflectionException accessDenied(Member member, Throwable cause) {
        return new ReflectionException("Reflective access denied on " + member + ".", cause);
    }

    public static ReflectionException invocationFailed(Member member, Throwable cause) {
        return new ReflectionException("Reflective invocation failed on " + member + ".", cause);
    }
}
