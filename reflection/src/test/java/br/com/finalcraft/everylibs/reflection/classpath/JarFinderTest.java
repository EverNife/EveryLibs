package br.com.finalcraft.everylibs.reflection.classpath;

import br.com.finalcraft.everylibs.reflection.ReflectionException;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JarFinderTest {

    @Test
    void locatesAClassOnTheClasspath() {
        URL location = JarFinder.getLocation(JarFinderTest.class);
        assertNotNull(location);

        File file = JarFinder.urlToFile(location);
        assertNotNull(file);
        assertTrue(file.exists());


    }

    @Test
    void nullUrlConvertsToNull() {
        assertNull(JarFinder.urlToFile((URL) null));
    }

    @Test
    void hardInvalidUrlThrowsReflectionException() {
        assertThrows(ReflectionException.class, () -> JarFinder.urlToFile("not-a-valid-url"));
    }
}
