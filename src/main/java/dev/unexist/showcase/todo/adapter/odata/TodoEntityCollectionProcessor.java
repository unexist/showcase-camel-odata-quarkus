/**
 * @package Showcase-OData-Quarkus
 *
 * @file Todo OData entity collection processor
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import java.util.List;

public class TodoEntityCollectionProcessor implements org.apache.olingo.server.api.processor.EntityCollectionProcessor {

    private TodoEntityStorage storage;
    private OData odata;
    private ServiceMetadata serviceMetadata;

    public TodoEntityCollectionProcessor(TodoEntityStorage storage) {
        this.storage = storage;
    }

    // our processor is initialized with the OData context object
    public void init(OData initOdata, ServiceMetadata initServiceMetadata) {
        this.odata = initOdata;
        this.serviceMetadata = initServiceMetadata;
    }

    // the only method that is declared in the TodoEntityCollectionProcessor interface
    // this method is called, when the user fires a request to an EntitySet
    // in our example, the URL would be:
    // http://localhost:8080/ExampleService1/ExampleServlet1.svc/Products
    public void readEntityCollection(ODataRequest request, ODataResponse response,
                                     UriInfo uriInfo, ContentType requestFormat)
            throws SerializerException, ODataApplicationException {

        // 1st retrieve the requested EntitySet from the uriInfo (representation of the parsed URI)
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // in our example, the first segment is the EntitySet
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // 2nd: fetch the data from backend for this requested EntitySetName and deliver as EntitySet
        EntityCollection entityCollection = storage.readEntitySetData(edmEntitySet);

        // 3rd: create a serializer based on the requested format (json)
        ODataSerializer serializer = odata.createSerializer(requestFormat);

        // 4th: Now serialize the content: transform from the EntitySet object to InputStream
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

        final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
        EntityCollectionSerializerOptions opts =
                EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl).build();
        SerializerResult serializedContent = serializer.entityCollection(serviceMetadata,
                edmEntityType, entityCollection, opts);

        // Finally: configure the response object: set the body, headers and status code
        response.setContent(serializedContent.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, requestFormat.toContentTypeString());
    }
}
