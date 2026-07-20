package br.com.finalcraft.everylibs.util.numberwrapper;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberWrapperTest {

    @Test
    void intAndDoubleOfSameValueAreEqual() {
        assertEquals(NumberWrapper.of(5), NumberWrapper.of(5.0));
    }

    @Test
    void intAndDoubleOfSameValueShareHashCode() {
        assertEquals(NumberWrapper.of(5).hashCode(), NumberWrapper.of(5.0).hashCode());
    }

    @Test
    void hashSetDeduplicatesEqualWrappers() {
        Set<NumberWrapper> set = new HashSet<>();
        set.add(NumberWrapper.of(5));
        set.add(NumberWrapper.of(5.0));

        assertEquals(1, set.size());
        assertTrue(set.contains(NumberWrapper.of(5)));
    }
}
