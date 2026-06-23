package br.com.finalcraft.everylibs.reflection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConstructorsTest {

    static class Target {
        final int x;
        final String s;

        Target() {
            this(0, "default");
        }

        Target(int x, String s) {
            this.x = x;
            this.s = s;
        }
    }

    @Test
    void buildsNoArgAndParameterizedInstances() {
        ConstructorInvoker<Target> noArg = FCReflectionUtil.constructors().getConstructor(Target.class);
        assertEquals(0, noArg.newInstance().x);

        ConstructorInvoker<Target> twoArg = FCReflectionUtil.constructors().getConstructor(Target.class, int.class, String.class);
        Target built = twoArg.newInstance(7, "hi");
        assertEquals(7, built.x);
        assertEquals("hi", built.s);

        assertNotNull(twoArg.getConstructor());
    }

    @Test
    void matchesPrimitiveAgainstWrapper() {
        // Requesting Integer.class must match the int.class constructor.
        ConstructorInvoker<Target> invoker = FCReflectionUtil.constructors().getConstructor(Target.class, Integer.class, String.class);
        assertEquals(7, invoker.newInstance(7, "hi").x);
    }

    @Test
    void returnsNullWhenConstructorAbsent() {
        assertNull(FCReflectionUtil.constructors().getConstructor(Target.class, double.class));
    }
}
