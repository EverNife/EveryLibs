package br.com.finalcraft.everylibs.reflection;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the interface-aware behaviour of field/method lookup: a {@code default} method or interface
 * constant the target only inherits (never redeclares) is found, while a class declaration shadows
 * an inherited default and a static interface method is not treated as inherited.
 */
class InterfaceWalkTest {

    interface Named {
        String CONSTANT = "k";              // interface constant (public static final)

        default String describe() {
            return "named";
        }

        String required();                  // abstract -> implemented by each class

        static String origin() {            // static -> never inherited by implementors
            return "iface";
        }
    }

    static class Plain implements Named {
        @Override
        public String required() {
            return "r";
        }
        // deliberately does NOT override describe()
    }

    static class OverridesDescribe implements Named {
        @Override
        public String required() {
            return "r";
        }

        @Override
        public String describe() {
            return "overridden";
        }
    }

    interface RefinedNamed extends Named {
        @Override
        default String describe() {
            return "refined";
        }
    }

    static class RefinedImpl implements RefinedNamed {
        @Override
        public String required() {
            return "r";
        }
    }

    @Test
    void findsUnoverriddenInterfaceDefaultMethod() {
        MethodInvoker<String> describe = FCReflectionUtil.getMethods().getMethod(Plain.class, "describe");
        assertNotNull(describe);
        assertEquals("named", describe.invoke(new Plain()));
    }

    @Test
    void classOverrideShadowsInterfaceDefault() {
        MethodInvoker<String> describe = FCReflectionUtil.getMethods().getMethod(OverridesDescribe.class, "describe");
        assertEquals(OverridesDescribe.class, describe.getMethod().getDeclaringClass());
        assertEquals("overridden", describe.invoke(new OverridesDescribe()));
    }

    @Test
    void nearestInterfaceDefaultWins() {
        // RefinedNamed.describe() overrides Named.describe(); the nearer interface must win.
        MethodInvoker<String> describe = FCReflectionUtil.getMethods().getMethod(RefinedImpl.class, "describe");
        assertEquals(RefinedNamed.class, describe.getMethod().getDeclaringClass());
        assertEquals("refined", describe.invoke(new RefinedImpl()));
    }

    @Test
    void staticInterfaceMethodIsNotInherited() {
        // A static interface method is callable only on the interface, never on an implementor.
        assertNull(FCReflectionUtil.getMethods().getMethod(Plain.class, "origin"));
    }

    @Test
    void findsInterfaceConstant() {
        FieldAccessor<String> constant = FCReflectionUtil.getFields().getField(Plain.class, "CONSTANT", String.class);
        assertNotNull(constant);
        assertTrue(constant.isStatic());
        assertEquals("k", constant.get(null));
    }

    @Test
    void getMethodsIncludesDefaultsButNotStatics() {
        long defaults = FCReflectionUtil.getMethods()
                .getMethods(Plain.class, method -> method.getName().equals("describe"))
                .count();
        assertEquals(1, defaults);

        long statics = FCReflectionUtil.getMethods()
                .getMethods(Plain.class, method -> method.getName().equals("origin"))
                .count();
        assertEquals(0, statics);
    }

    @Test
    void getAllFieldsIncludesInterfaceConstantsOnlyWhenInherited() {
        List<String> declaredOnly = FCReflectionUtil.getFields().getAllFields(Plain.class, false).stream()
                .map(accessor -> accessor.getField().getName())
                .collect(Collectors.toList());
        assertFalse(declaredOnly.contains("CONSTANT")); // Plain declares no fields of its own

        List<String> withInherited = FCReflectionUtil.getFields().getAllFields(Plain.class, true).stream()
                .map(accessor -> accessor.getField().getName())
                .collect(Collectors.toList());
        assertTrue(withInherited.contains("CONSTANT"));
    }
}
