package org.infinispan.odata.producer;

import org.odata4j.core.OError;

/**
 * Another implementation of OError interface for needs of InfinispanProducer.
 *
 * @author Tomas Sykora <tomas@infinispan.org>
 */
public class OErrorImpl implements OError {

    private String message;

    public OErrorImpl(String message) {
        this.message = message;
    }

    /**
     * Gets the error code, a mandatory service-defined string.
     * <p/>
     * <p>This value may be used to provide a more specific substatus to the returned HTTP response code.
     *
     * @return the error code
     */
    @Override
    public String getCode() {
        return null;
    }

    /**
     * Gets the error-message text, a human readable message describing the error.
     *
     * @return the error-message text
     */
    @Override
    public String getMessage() {
        return this.message;
    }

    /**
     * Gets the inner error, service specific debugging information that might assist a service implementer in determining the cause of an error.
     * <p/>
     * <p>Should only be used in development environments in order to guard against potential security concerns around information disclosure.
     *
     * @return the inner error
     */
    @Override
    public String getInnerError() {
        return null;
    }
}
