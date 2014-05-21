package org.infinispan.odata.facades;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.junit.Assert;
import org.odata4j.consumer.ODataConsumer;
import org.odata4j.consumer.behaviors.MethodTunnelingBehavior;
import org.odata4j.format.FormatType;
import org.odata4j.jersey.consumer.ODataJerseyConsumer;
import org.odata4j.jersey.consumer.ODataJerseyConsumer.Builder;
import org.odata4j.jersey.producer.server.ODataJerseyServer;
import org.odata4j.producer.resources.DefaultODataApplication;
import org.odata4j.producer.resources.RootApplication;
import org.odata4j.producer.server.ODataServer;

/**
 * Reused and modified class from odata4j InMemoryProducer example.
 */
public class JerseyRuntimeFacade implements RuntimeFacade {

    static {
        // ensure that the correct JAX-RS implementation is loaded
        RuntimeDelegate runtimeDelegate = new com.sun.jersey.server.impl.provider.RuntimeDelegateImpl();
        RuntimeDelegate.setInstance(runtimeDelegate);
        Assert.assertEquals(runtimeDelegate, RuntimeDelegate.getInstance());
    }

    @Override
    public void hostODataServer(String baseUri) {
        ODataServer server = startODataServer(baseUri);
        System.out.println("Infinispan OData server successfully started.");
        System.out.println("Service is listening at: " + baseUri);
        System.out.println("Metadata document is ready for access at: " + baseUri + "$metadata");
    }

    @Override
    public ODataServer startODataServer(String baseUri) {
        return this.createODataServer(baseUri).start();
    }

    @Override
    public ODataConsumer create(String endpointUri, FormatType format, String methodToTunnel) {
        Builder builder = ODataJerseyConsumer.newBuilder(endpointUri);

        if (format != null) {
            builder = builder.setFormatType(format);
        }

        if (methodToTunnel != null) {
            builder = builder.setClientBehaviors(new MethodTunnelingBehavior(methodToTunnel));
        }

        return builder.build();
    }

    private ODataServer createODataServer(String baseUri) {

        return new ODataJerseyServer(baseUri, DefaultODataApplication.class, RootApplication.class);
                  // if needed, use from package: com.sun.jersey.api.container.filter.LoggingFilter;
//                .addJerseyResponseFilter(LoggingFilter.class).setJerseyTrace(true)
//                .addJerseyRequestFilter(LoggingFilter.class).setJerseyTrace(true); // log all requests
    }

    @Override
    public String getWebResource(String uri) {
        WebResource webResource = new Client().resource(uri);
        return webResource.get(String.class);
    }

    @Override
    public String acceptAndReturn(String uri, MediaType mediaType) {
        uri = uri.replace(" ", "%20");
        WebResource webResource = new Client().resource(uri);
        return webResource.accept(mediaType).get(String.class);
    }

    @Override
    public String getWebResource(String uri, String accept) {
        String resource = new Client().resource(uri).accept(accept).get(String.class);
        return resource;
    }

    @Override
    public void accept(String uri, MediaType mediaType) {
        uri = uri.replace(" ", "%20");
        WebResource webResource = new Client().resource(uri);
        webResource.accept(mediaType);
    }

}