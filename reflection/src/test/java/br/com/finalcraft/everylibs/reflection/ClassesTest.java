package br.com.finalcraft.everylibs.reflection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassesTest {

    @Test
    void getClassResolvesOrReturnsNull() {
        assertEquals(String.class, FCReflectionUtil.getClasses().getClass("java.lang.String"));
        assertNull(FCReflectionUtil.getClasses().getClass("no.such.ClassXyz"));
    }

    @Test
    void getUntypedClassResolves() {
        Class<String> stringClass = FCReflectionUtil.getClasses().getUntypedClass("java.lang.String");
        assertEquals(String.class, stringClass);
    }

    @Test
    void isClassLoadedReflectsAvailability() {
        assertTrue(FCReflectionUtil.getClasses().isClassLoaded("java.lang.Integer"));
        assertFalse(FCReflectionUtil.getClasses().isClassLoaded("no.such.ClassXyz"));
    }

    @Test
    void loaderAwareLookupAndFirstClass() {
        ClassLoader loader = getClass().getClassLoader();
        assertEquals(String.class, FCReflectionUtil.getClasses().getClass("java.lang.String", loader));
        assertNull(FCReflectionUtil.getClasses().getClass("no.such.X", loader));

        Class<?> first = FCReflectionUtil.getClasses().getFirstClass(loader, "no.such.A", "java.lang.Integer", "no.such.B");
        assertEquals(Integer.class, first);
    }
}
