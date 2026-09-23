package br.com.finalcraft.everylibs.reflection;

import br.com.finalcraft.everylibs.reflection.lookup.ClassLookup;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
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

    @Test
    void lookupSaysFoundAndAbsentApart() {
        ClassLookup found = FCReflectionUtil.getClasses().lookupClass("java.lang.String");
        assertTrue(found.isFound());
        assertEquals(String.class, found.getType());

        ClassLookup absent = FCReflectionUtil.getClasses().lookupClass("no.such.ClassXyz");
        assertTrue(absent.isAbsent());
        assertNull(absent.getType());
        assertNull(absent.getLinkageError());
    }

    @Test
    void aClassWhoseInitializerFailsIsUnlinkableNotAbsent() {
        String name = FailsToInitialize.class.getName(); // a class literal does not run <clinit>

        ClassLookup lookup = FCReflectionUtil.getClasses().lookupClass(name);

        assertTrue(lookup.isUnlinkable(), lookup.toString());
        assertInstanceOf(ExceptionInInitializerError.class, lookup.getLinkageError());
        assertNull(FCReflectionUtil.getClasses().getClass(name), "the null-returning lookup still reads it as a miss");
        assertFalse(FCReflectionUtil.getClasses().isClassLoaded(name));
    }

    @Test
    void aClassPresentWithoutItsSuperclassIsUnlinkableThroughALoader() {
        ClassLoader missingParent = new ChildOnlyLoader(getClass().getClassLoader());

        ClassLookup lookup = FCReflectionUtil.getClasses().lookupClass(LinkChild.class.getName(), missingParent);

        assertTrue(lookup.isUnlinkable(), lookup.toString());
        assertInstanceOf(NoClassDefFoundError.class, lookup.getLinkageError());
        assertTrue(FCReflectionUtil.getClasses().lookupClass("no.such.X", missingParent).isAbsent());
    }

    static class FailsToInitialize {
        static final int VALUE = fail();

        private static int fail() {
            throw new IllegalStateException("initializer failure on purpose");
        }
    }

    static class LinkParent {
    }

    static class LinkChild extends LinkParent {
    }

    /** Defines {@link LinkChild} itself and refuses to find {@link LinkParent}, so the child cannot link. */
    private static final class ChildOnlyLoader extends ClassLoader {

        ChildOnlyLoader(ClassLoader parent) {
            super(parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (name.equals(LinkParent.class.getName())) {
                throw new ClassNotFoundException(name);
            }
            if (!name.equals(LinkChild.class.getName())) {
                return super.loadClass(name, resolve);
            }
            try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                for (int read; (read = in.read(buffer)) != -1; ) {
                    bytes.write(buffer, 0, read);
                }
                return defineClass(name, bytes.toByteArray(), 0, bytes.size());
            } catch (IOException e) {
                throw new ClassNotFoundException(name, e);
            }
        }
    }
}
