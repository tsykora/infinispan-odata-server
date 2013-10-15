package org.tsykora.odata.facades;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.RuntimeDelegate;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.container.filter.LoggingFilter;
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

public class JerseyRuntimeFacade implements RuntimeFacade {

    static {
        // ensure that the correct JAX-RS implementation is loaded
        RuntimeDelegate runtimeDelegate = new com.sun.jersey.server.impl.provider.RuntimeDelegateImpl();
        RuntimeDelegate.setInstance(runtimeDelegate);
        Assert.assertEquals(runtimeDelegate, RuntimeDelegate.getInstance());
    }

    @Override
    public void hostODataServer(String baseUri) {
//        try {
            ODataServer server = startODataServer(baseUri);
//          server.setHttpAuthenticator (need added classes + improvements later)

            System.out.println("OData Infinispan server successfully started.");
            System.out.println("-Dlog4j.configuration=file:///path/log4j.xml can be passed.");
            System.out.println("Service listening at: " + baseUri);
            System.out.println("Metadata ready for access at: " + baseUri + "$metadata");
//            System.out.println("Press RETURN key to stop server and exit...");
//            new BufferedReader(new InputStreamReader(System.in)).readLine();
//            server.stop();
//        } catch (IOException e) {
//            throw Throwables.propagate(e);
//        }
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

        return new ODataJerseyServer(baseUri, DefaultODataApplication.class, RootApplication.class)
                .addJerseyRequestFilter(LoggingFilter.class).setJerseyTrace(true) // log all requests
//        .addJerseyRequestFilter(LoggingFilter.class) // log all requests
                //      .addHttpServerFilter(new WhitelistFilter("127.0.0.1","0:0:0:0:0:0:0:1%0")) // only allow local requests
                ;
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