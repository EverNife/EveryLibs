package br.com.finalcraft.everylibs.reflection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClassesTest {

    @Test
    void getClassResolvesOrReturnsNull() {
        assertEquals(String.class, FCReflectionUtil.classes().getClass("java.lang.String"));
        assertNull(FCReflectionUtil.classes().getClass("no.such.ClassXyz"));
    }

    @Test
    void getUntypedClassResolves() {
        Class<String> stringClass = FCReflectionUtil.classes().getUntypedClass("java.lang.String");
        assertEquals(String.class, stringClass);
    }

    @Test
    void isClassLoadedReflectsAvailability() {
        assertTrue(FCReflectionUtil.classes().isClassLoaded("java.lang.Integer"));
        assertFalse(FCReflectionUtil.classes().isClassLoaded("no.such.ClassXyz"));
    }

    @Test
    void loaderAwareLookupAndFirstClass() {
        ClassLoader loader = getClass().getClassLoader();
        assertEquals(String.class, FCReflectionUtil.classes().getClass("java.lang.String", loader));
        assertNull(FCReflectionUtil.classes().getClass("no.such.X", loader));

        Class<?> first = FCReflectionUtil.classes().getFirstClass(loader, "no.such.A", "java.lang.Integer", "no.such.B");
        assertEquals(Integer.class, first);
    }
}
