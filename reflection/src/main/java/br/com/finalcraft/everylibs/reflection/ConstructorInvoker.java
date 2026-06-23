package br.com.finalcraft.everylibs.reflection;

import java.lang.reflect.Constructor;

/**
 * A typed construction handle over a single resolved {@link Constructor}.
 *
 * @param <T> the constructed type.
 */
public interface ConstructorInvoker<T> {

    /**
     * Construct a new instance.
     *
     * @param arguments the arguments to pass, one per declared parameter.
     * @return the constructed object.
     */
    T newInstance(Object... arguments);

    /**
     * @return the backing {@link Constructor}.
     */
    Constructor<T> getConstructor();
}
