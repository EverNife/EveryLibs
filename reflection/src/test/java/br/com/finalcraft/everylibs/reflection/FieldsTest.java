package br.com.finalcraft.everylibs.reflection;

import org.junit.jupiter.api.Test;

import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FieldsTest {

    static class Holder {
        private String secret = "init";
        private static int counter = 5;
    }

    static class FieldParent {
        int inherited = 1;
    }

    static class FieldChild extends FieldParent {
        int own = 2;
    }

    @Test
    void readsAndWritesInstanceField() {
        FieldAccessor<String> accessor = FCReflectionUtil.fields().getField(Holder.class, "secret", String.class);
        Holder holder = new Holder();

        assertEquals("init", accessor.get(holder));
        accessor.set(holder, "changed");
        assertEquals("changed", accessor.get(holder));
        assertFalse(accessor.isStatic());
        assertTrue(accessor.hasField(holder));
    }

    @Test
    void hasFieldReturnsFalseForNullTarget() {
        FieldAccessor<String> accessor = FCReflectionUtil.fields().getField(Holder.class, "secret", String.class);
        assertFalse(accessor.hasField(null));
    }

    @Test
    void readsAndWritesStaticField() {
        FieldAccessor<Integer> accessor = FCReflectionUtil.fields().getField(Holder.class, "counter", int.class);

        assertEquals(5, accessor.get(null));
        accessor.set(null, 9);
        assertEquals(9, accessor.get(null));
        assertTrue(accessor.isStatic());

        accessor.set(null, 5); // restore for test isolation
    }

    @Test
    void singleLookupWalksSuperclasses() {
        FieldAccessor<Integer> accessor = FCReflectionUtil.fields().getField(FieldChild.class, "inherited", int.class);
        FieldChild child = new FieldChild();
        assertEquals(1, accessor.get(child));
    }

    @Test
    void getAllFieldsHonorsIncludeInherited() {
        List<FieldAccessor<?>> declaredOnly = FCReflectionUtil.fields().getAllFields(FieldChild.class, false);
        List<FieldAccessor<?>> withInherited = FCReflectionUtil.fields().getAllFields(FieldChild.class, true);

        assertEquals(1, declaredOnly.size());
        assertEquals("own", declaredOnly.get(0).getField().getName());

        assertEquals(2, withInherited.size());
        assertEquals("own", withInherited.get(0).getField().getName());
        assertEquals("inherited", withInherited.get(1).getField().getName());
    }

    @Test
    void returnsNullWhenFieldAbsent() {
        assertNull(FCReflectionUtil.fields().getField(Holder.class, "nonExistentXyz", String.class));
    }

    @Test
    void fieldWalkerIsLazyAndRespectsInheritance() {
        Iterator<FieldAccessor<?>> deep = FCReflectionUtil.fields().fieldWalker(FieldChild.class, true);
        assertTrue(deep.hasNext());
        assertEquals("own", deep.next().getField().getName());
        assertEquals("inherited", deep.next().getField().getName());
        assertFalse(deep.hasNext());

        Iterator<FieldAccessor<?>> shallow = FCReflectionUtil.fields().fieldWalker(FieldChild.class, false);
        assertEquals("own", shallow.next().getField().getName());
        assertFalse(shallow.hasNext()); // does not descend into FieldParent
    }

    @Test
    void staticConvenienceOverloads() {
        FieldAccessor<Integer> counter = FCReflectionUtil.fields().getField(Holder.class, "counter", int.class);
        assertEquals(5, counter.get());     // no-arg get()
        counter.set(11);                    // set(value)
        assertEquals(11, counter.get());
        counter.set(5);                     // restore for isolation

        FieldAccessor<String> secret = FCReflectionUtil.fields().getField(Holder.class, "secret", String.class);
        assertThrows(ReflectionException.class, secret::get); // not static
    }

    @Test
    void setRejectsWrongValueType() {
        FieldAccessor<Object> secret = FCReflectionUtil.fields().getField(Holder.class, "secret");
        ReflectionException ex = assertThrows(ReflectionException.class, () -> secret.set(new Holder(), 123));
        assertTrue(ex.getMessage().contains("expects java.lang.String"));
    }
}
