package br.com.finalcraft.everylibs.reflection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClassReflectTest {

    static class Widget {
        final int id;

        Widget(int id) {
            this.id = id;
        }

        String label() {
            return "w" + id;
        }
    }

    @Test
    void typedHandleConstructsAndInvokesWithoutCasts() {
        ClassReflect<Widget> handle = FCReflectionUtil.of(Widget.class);

        ConstructorInvoker<Widget> ctor = handle.constructor(int.class); // ConstructorInvoker<Widget>, no cast needed
        Widget widget = ctor.newInstance(7);
        assertEquals(7, widget.id);

        MethodInvoker<String> label = handle.typedMethod("label", String.class); // R bound to String
        assertEquals("w7", label.invoke(widget));
    }

    @Test
    void untypedHandleFromName() {
        ClassReflect<?> handle = FCReflectionUtil.of(Widget.class.getName());
        assertEquals(Widget.class, handle.target());
    }
}
