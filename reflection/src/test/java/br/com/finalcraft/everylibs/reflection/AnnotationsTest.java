package br.com.finalcraft.everylibs.reflection;

import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class AnnotationsTest {

    @Retention(RetentionPolicy.RUNTIME)
    @interface Marker {
        String value();
    }

    @Marker("base")
    static class AnnBase {
        @Marker("method")
        public void annotated() {
        }
    }

    static class AnnDerived extends AnnBase {
        @Override
        public void annotated() { // overridden without re-declaring the annotation
        }
    }

    static class ArgHolder {
        void take(String a, int b) {
        }
    }

    @Marker("iface")
    interface MarkedIface {
    }

    static class ImplViaIface implements MarkedIface {
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface Handler {
    }

    static class WithHandlers {
        @Handler
        public void a() {
        }

        @Handler
        public void b() {
        }

        public void c() {
        }
    }

    @Test
    void findsClassAnnotationOnSuperclass() {
        Marker marker = FCReflectionUtil.getAnnotations().getAnnotationDeeply(AnnDerived.class, Marker.class);
        assertNotNull(marker);
        assertEquals("base", marker.value());
    }

    @Test
    void findsMethodAnnotationOnSuperclassMethod() throws NoSuchMethodException {
        Method overridden = AnnDerived.class.getDeclaredMethod("annotated");
        Marker marker = FCReflectionUtil.getAnnotations().getAnnotationDeeply(overridden, Marker.class);
        assertNotNull(marker);
        assertEquals("method", marker.value());
    }

    @Test
    void returnsNullWhenAnnotationAbsent() {
        assertNull(FCReflectionUtil.getAnnotations().getAnnotationDeeply(ArgHolder.class, Marker.class));
    }

    @Test
    void resolvesArgumentIndexAndType() throws NoSuchMethodException {
        Method take = ArgHolder.class.getDeclaredMethod("take", String.class, int.class);

        assertEquals(0, FCReflectionUtil.getAnnotations().getArgIndex(take, String.class, false));
        assertEquals(1, FCReflectionUtil.getAnnotations().getArgIndex(take, int.class, false));
        assertEquals(-1, FCReflectionUtil.getAnnotations().getArgIndex(take, Double.class, false));

        assertEquals(String.class, FCReflectionUtil.getAnnotations().getArgAtIndex(take, 0));
        assertNull(FCReflectionUtil.getAnnotations().getArgAtIndex(take, 5));
    }

    @Test
    void findsClassAnnotationOnInterface() {
        Marker marker = FCReflectionUtil.getAnnotations().getAnnotationDeeply(ImplViaIface.class, Marker.class);
        assertNotNull(marker);
        assertEquals("iface", marker.value());
    }

    @Test
    void scansMethodsCarryingAnnotation() {
        List<MethodInvoker<?>> handlers = FCReflectionUtil.getAnnotations().methodsAnnotatedWith(WithHandlers.class, Handler.class);
        assertEquals(2, handlers.size());
    }
}
