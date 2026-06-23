package br.com.finalcraft.everylibs.reflection;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodsTest {

    static class StaticHolder {
        static String greet(String name) {
            return "hi " + name;
        }
    }

    static class ArrayTarget {
        int count(Object[] array) {
            return array.length;
        }
    }

    // Return-type covariance across the hierarchy: Base.make() returns Object, not String.
    static class Base {
        Object make() {
            return "x";
        }
    }

    static class Derived extends Base {
    }

    static class TypedBase {
        String name() {
            return "n";
        }
    }

    static class TypedDerived extends TypedBase {
    }

    static class MultiMethod {
        public void alpha() {
        }

        public void beta() {
        }

        private int gamma(int x) {
            return x;
        }
    }

    // Covariant override: the compiler emits a synthetic bridge "Object make()" alongside
    // the real "String make()".
    static class CovParent {
        Object make() {
            return "parent";
        }
    }

    static class CovChild extends CovParent {
        @Override
        String make() {
            return "child";
        }
    }

    static class Thrower {
        void boom() {
            throw new IllegalStateException("boom");
        }
    }

    static class Primitives {
        double timesTwo(double value) {
            return value * 2;
        }
    }

    @Test
    void invokesStaticMethod() {
        MethodInvoker<String> greet = FCReflectionUtil.methods().getMethod(StaticHolder.class, "greet", String.class);
        assertTrue(greet.isStatic());
        assertEquals("hi bob", greet.invoke(null, "bob"));
    }

    @Test
    void singleArrayArgumentIsNotReWrapped() {
        MethodInvoker<Integer> count = FCReflectionUtil.methods().getMethod(ArrayTarget.class, "count", Object[].class);
        Object[] array = {"a", "b", "c"};
        // The array is passed as a single argument; it must land on the array parameter as-is.
        assertEquals(3, count.invoke(new ArrayTarget(), new Object[]{array}));
    }

    @Test
    void typedLookupPreservesReturnTypeAcrossSuperclasses() {
        // Derived has no make(); Base.make() returns Object, so a String-typed lookup must miss.
        assertNull(FCReflectionUtil.methods().getTypedMethod(Derived.class, "make", String.class));

        // A matching return type still resolves through the superclass.
        MethodInvoker<String> name = FCReflectionUtil.methods().getTypedMethod(TypedDerived.class, "name", String.class);
        assertEquals("n", name.invoke(new TypedDerived()));
    }

    @Test
    void getMethodsFiltersWithPredicateAndFindsPrivateMembers() {
        long starting = FCReflectionUtil.methods().getMethods(MultiMethod.class, m -> m.getName().startsWith("a")).count();
        assertEquals(1, starting);

        List<String> declared = FCReflectionUtil.methods().getMethods(MultiMethod.class, m -> m.getDeclaringClass() == MultiMethod.class)
                .map(invoker -> invoker.getMethod().getName())
                .collect(Collectors.toList());
        assertTrue(declared.contains("gamma")); // private method discovered (declared scan)
    }

    @Test
    void returnsNullWhenMethodAbsent() {
        assertNull(FCReflectionUtil.methods().getMethod(StaticHolder.class, "nope"));
    }

    @Test
    void prefersRealMethodOverSyntheticBridge() {
        MethodInvoker<String> real = FCReflectionUtil.methods().getTypedMethod(CovChild.class, "make", String.class);
        assertFalse(real.getMethod().isBridge());
        assertEquals(String.class, real.getMethod().getReturnType());
        assertEquals("child", real.invoke(new CovChild()));

        MethodInvoker<?> viaStream = FCReflectionUtil.methods().getMethods(CovChild.class, m -> m.getName().equals("make"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected a make() method"));
        assertFalse(viaStream.getMethod().isBridge());
        assertEquals(String.class, viaStream.getMethod().getReturnType());
    }

    @Test
    void invocationReportsTheUserExceptionAsCause() {
        MethodInvoker<Void> boom = FCReflectionUtil.methods().getMethod(Thrower.class, "boom");
        ReflectionException ex = assertThrows(ReflectionException.class, () -> boom.invoke(new Thrower()));
        assertTrue(ex.getCause() instanceof IllegalStateException);
        assertEquals("boom", ex.getCause().getMessage());
    }

    @Test
    void matchesPrimitiveParamAgainstWrapperToken() {
        // No exact match for [Double]; the primitive-compatible fallback finds timesTwo(double).
        MethodInvoker<Double> m = FCReflectionUtil.methods().getMethod(Primitives.class, "timesTwo", Double.class);
        assertEquals(6.0, m.invoke(new Primitives(), 3.0));
    }

    @Test
    void invokeStaticSkipsTheNullTarget() {
        MethodInvoker<String> greet = FCReflectionUtil.methods().getMethod(StaticHolder.class, "greet", String.class);
        assertEquals("hi bob", greet.invokeStatic("bob"));

        MethodInvoker<Integer> count = FCReflectionUtil.methods().getMethod(ArrayTarget.class, "count", Object[].class);
        assertThrows(ReflectionException.class, () -> count.invokeStatic((Object) new Object[0]));
    }

    @Test
    void invokeRejectsWrongArity() {
        MethodInvoker<String> greet = FCReflectionUtil.methods().getMethod(StaticHolder.class, "greet", String.class);
        ReflectionException ex = assertThrows(ReflectionException.class, () -> greet.invoke(null, "a", "b"));
        assertTrue(ex.getMessage().contains("expects 1"));
    }
}
