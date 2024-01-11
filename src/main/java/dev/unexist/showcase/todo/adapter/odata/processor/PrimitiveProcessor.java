/**
 * @package Showcase-OData-Quarkus
 *
 * @file OData todo primitive processor
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata.processor;

import dev.unexist.showcase.todo.adapter.odata.storage.EntityStorage;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.jboss.resteasy.spi.NotImplementedYetException;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class PrimitiveProcessor implements org.apache.olingo.server.api.processor.PrimitiveProcessor {
    private OData odata;
    private EntityStorage storage;
    private ServiceMetadata serviceMetadata;

    /**
     * Constructor
     *
     * @param  storage  A {@link EntityStorage} instance
     **/

    public PrimitiveProcessor(EntityStorage storage) {
        this.storage = storage;
    }

    /**
     * Init this object
     *
     * @param  odata            a {@link OData} instance
     * @param  serviceMetadata  A {@link ServiceMetadata} instance
     **/

    public void init(OData odata, ServiceMetadata serviceMetadata) {
        this.odata = odata;
        this.serviceMetadata = serviceMetadata;
    }

    @Override
    public void readPrimitive(ODataRequest request, ODataResponse response,
                              UriInfo uriInfo, ContentType contentType)
            throws ODataApplicationException, ODataLibraryException {

        /* 1. Retrieve info from URI */
        /* 1.1. retrieve the info about the requested entity set */
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        /* Note: only in our example we can rely that the first segment is the EntitySet */
        UriResourceEntitySet uriEntityset = (UriResourceEntitySet) resourceParts.get(0);
        EdmEntitySet edmEntitySet = uriEntityset.getEntitySet();
        /* The key for the entity */
        List<UriParameter> keyPredicates = uriEntityset.getKeyPredicates();

        /* 1.2. retrieve the requested (Edm) property */
        /* The last segment is the Property */
        UriResourceProperty uriProperty = (UriResourceProperty) resourceParts.get(resourceParts.size() -1);
        EdmProperty edmProperty = uriProperty.getProperty();
        String edmPropertyName = edmProperty.getName();
        /* in our example, we know we have only primitive types in our model */
        EdmPrimitiveType edmPropertyType = (EdmPrimitiveType) edmProperty.getType();

        // 2. Retrieve data from backend
        /* 2.1. Retrieve the entity data, for which the property has to be read */
        Entity entity = storage.readEntityData(edmEntitySet, keyPredicates);
        if (null == entity) { // Bad request
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        /* 2.2. retrieve the property data from the entity */
        Property property = entity.getProperty(edmPropertyName);
        if (null == property) {
            throw new ODataApplicationException("Property not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        /* 3. Serialize */
        Object value = property.getValue();

        if (null != value) {
            /* 3.1. Configure the serializer */
            ODataSerializer serializer = odata.createSerializer(contentType);

            ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).navOrPropertyPath(edmPropertyName).build();
            PrimitiveSerializerOptions options = PrimitiveSerializerOptions.with().contextURL(contextUrl).build();
            /* 3.2. Serialize */
            SerializerResult serializerResult = serializer.primitive(serviceMetadata, edmPropertyType, property, options);
            InputStream propertyStream = serializerResult.getContent();

            /* 4. Configure the response object */
            response.setContent(propertyStream);
            response.setStatusCode(HttpStatusCode.OK.getStatusCode());
            response.setHeader(HttpHeader.CONTENT_TYPE, contentType.toContentTypeString());
        } else {
            response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
        }
    }

    @Override
    public void updatePrimitive(ODataRequest request, ODataResponse response,
                                UriInfo uriInfo, ContentType contentType, ContentType contentType1)
            throws ODataApplicationException, ODataLibraryException {
        throw new NotImplementedYetException();
    }

    @Override
    public void deletePrimitive(ODataRequest request, ODataResponse response,UriInfo uriInfo)
            throws ODataApplicationException, ODataLibraryException {
        throw new NotImplementedYetException();
    }
}
