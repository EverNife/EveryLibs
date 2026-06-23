package br.com.finalcraft.everylibs.reflection;

import br.com.finalcraft.everylibs.reflection.internal.ReflectionCache;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class CacheTest {

    static class Holder {
        private String secret = "init";
    }

    @Test
    void sameLookupReturnsCachedInstance() {
        ReflectionCache.clear();
        FieldAccessor<String> first = FCReflectionUtil.fields().getField(Holder.class, "secret", String.class);
        FieldAccessor<String> second = FCReflectionUtil.fields().getField(Holder.class, "secret", String.class);
        assertSame(first, second);
    }

    @Test
    void clearForcesReResolution() {
        ReflectionCache.clear();
        FieldAccessor<String> before = FCReflectionUtil.fields().getField(Holder.class, "secret", String.class);
        ReflectionCache.clear();
        FieldAccessor<String> after = FCReflectionUtil.fields().getField(Holder.class, "secret", String.class);
        assertNotSame(before, after);
    }

    @Test
    void missReturnsNullConsistently() {
        ReflectionCache.clear();
        // First miss records a negative entry; the second is served from it. Both return null.
        assertNull(FCReflectionUtil.fields().getField(Holder.class, "ghost", String.class, 0));
        assertNull(FCReflectionUtil.fields().getField(Holder.class, "ghost", String.class, 0));
    }
}
