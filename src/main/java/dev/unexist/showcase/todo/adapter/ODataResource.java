/**
 * @package Showcase-OData-Quarkus
 *
 * @file Todo OData resource
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter;

import org.apache.commons.io.IOUtils;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHandler;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.core.ODataHandlerException;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

@Path("/odata")
public class ODataResource {
    private static final String URI = "odata/";

    @Inject
    EdmProvider edmProvider;

    @Inject
    EntityCollectionProcessor entityCollectionProcessor;

    private int lastSplit = 0;

    @GET
    @Operation(summary = "Handle OData requests")
    @Tag(name = "Todo")
    public Response handleAll(@Context HttpServletRequest httpRequest)
            throws IOException, ODataHandlerException, SerializerException {
        OData odata = OData.newInstance();
        ServiceMetadata edm = odata.createServiceMetadata(edmProvider,
                new ArrayList<>());

        ODataHandler handler = odata.createHandler(edm);
        handler.register(entityCollectionProcessor);

        ODataResponse oDataResponse = handler.process(createOdataRequest(httpRequest, lastSplit));

        String responseStr = IOUtils.toString(oDataResponse.getContent(), StandardCharsets.UTF_8);

        /* Assemble response */
        Response.ResponseBuilder builder = Response.status(oDataResponse.getStatusCode());

        builder.entity(Entity.text(responseStr));

        for (String key : oDataResponse.getAllHeaders().keySet()) {
            builder.header(key, oDataResponse.getHeader(key));
        }

        return builder.build();
    }

    private ODataRequest createOdataRequest(final HttpServletRequest request, final int split)
            throws ODataHandlerException, SerializerException {
        try {
            ODataRequest odRequest = new ODataRequest();

            odRequest.setBody(request.getInputStream());

            extractHeaders(odRequest, request);
            extractMethod(odRequest, request);
            extractUri(odRequest, request, split);

            return odRequest;
        } catch (final IOException e) {
            throw new SerializerException("An I/O exception occurred.", e,
                    SerializerException.MessageKeys.IO_EXCEPTION);
        }
    }

    private void extractHeaders(final ODataRequest odRequest, final HttpServletRequest httpRequest) {
        for (Enumeration<?> headerNames = httpRequest.getHeaderNames(); headerNames .hasMoreElements();) {
            String headerName = (String)headerNames.nextElement();
            List<String> headerValues = new ArrayList<>();

            for (Enumeration<?> headers = httpRequest.getHeaders(headerName); headers
                    .hasMoreElements();) {
                String value = (String)headers.nextElement();
                headerValues.add(value);
            }

            odRequest.addHeader(headerName, headerValues);
        }
    }

    private void extractMethod(final ODataRequest odRequest,
                               final HttpServletRequest httpRequest)
            throws ODataHandlerException {
        try {
            HttpMethod httpRequestMethod = HttpMethod.valueOf(httpRequest
                    .getMethod());

            if (HttpMethod.POST == httpRequestMethod) {
                String xHttpMethod = httpRequest
                        .getHeader(HttpHeader.X_HTTP_METHOD);
                String xHttpMethodOverride = httpRequest
                        .getHeader(HttpHeader.X_HTTP_METHOD_OVERRIDE);

                if (xHttpMethod == null && xHttpMethodOverride == null) {
                    odRequest.setMethod(httpRequestMethod);
                } else if (xHttpMethod == null) {
                    odRequest
                            .setMethod(HttpMethod.valueOf(xHttpMethodOverride));
                } else if (xHttpMethodOverride == null) {
                    odRequest.setMethod(HttpMethod.valueOf(xHttpMethod));
                } else {
                    if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
                        throw new ODataHandlerException(
                                "Ambiguous X-HTTP-Methods",
                                ODataHandlerException.MessageKeys.AMBIGUOUS_XHTTP_METHOD,
                                xHttpMethod, xHttpMethodOverride);
                    }
                    odRequest.setMethod(HttpMethod.valueOf(xHttpMethod));
                }
            } else {
                odRequest.setMethod(httpRequestMethod);
            }
        } catch (IllegalArgumentException e) {
            throw new ODataHandlerException("Invalid HTTP method"
                    + httpRequest.getMethod(),
                    ODataHandlerException.MessageKeys.INVALID_HTTP_METHOD,
                    httpRequest.getMethod());
        }
    }

    @SuppressWarnings("checkstyle:methodlength")
    private void extractUri(final ODataRequest odRequest,
                            final HttpServletRequest httpRequest, int split) {
        String rawRequestUri = httpRequest.getRequestURL().toString();

        String rawOdataPath;
        if (!"".equals(httpRequest.getServletPath())) {
            int beginIndex;

            beginIndex = rawRequestUri.indexOf(URI);
            beginIndex += URI.length();
            rawOdataPath = rawRequestUri.substring(beginIndex);
        } else if (!"".equals(httpRequest.getContextPath())) {
            int beginIndex;

            beginIndex = rawRequestUri.indexOf(httpRequest.getContextPath());
            beginIndex += httpRequest.getContextPath().length();
            rawOdataPath = rawRequestUri.substring(beginIndex);
        } else {
            rawOdataPath = httpRequest.getRequestURI();
        }

        String rawServiceResolutionUri;
        if (0 < split) {
            rawServiceResolutionUri = rawOdataPath;

            for (int i = 0; i < split; i++) {
                int e = rawOdataPath.indexOf("/", 1);

                if (-1 == e) {
                    rawOdataPath = "";
                } else {
                    rawOdataPath = rawOdataPath.substring(e);
                }
            }

            int end = rawServiceResolutionUri.length() - rawOdataPath.length();

            rawServiceResolutionUri = rawServiceResolutionUri.substring(0, end);
        } else {
            rawServiceResolutionUri = null;
        }

        String rawBaseUri = rawRequestUri.substring(0, rawRequestUri.length()
                - rawOdataPath.length());

        odRequest.setRawQueryPath(httpRequest.getQueryString());
        odRequest.setRawRequestUri(rawRequestUri
                + (httpRequest.getQueryString() == null ? "" : "?"
                + httpRequest.getQueryString()));

        odRequest.setRawODataPath(rawOdataPath);
        odRequest.setRawBaseUri(rawBaseUri);
        odRequest.setRawServiceResolutionUri(rawServiceResolutionUri);
    }
}
