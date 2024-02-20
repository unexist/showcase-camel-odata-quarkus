/**
 * @package Showcase-OData-Quarkus
 *
 * @file OData todo entity processor
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
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

public class EntityProcessor extends EntityProcessorBase
        implements org.apache.olingo.server.api.processor.EntityProcessor
{
    /**
     * Constructor
     *
     * @param  storage  A {@link EntityStorage} instance
     **/

    public EntityProcessor(EntityStorage storage) {
        this.storage = storage;
    }

    @Override
    public void readEntity(ODataRequest request, ODataResponse response,
                           UriInfo uriInfo, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException
    {
        EdmEntityType responseEdmEntityType = null;
        Entity responseEntity = null;
        EdmEntitySet responseEdmEntitySet = null;
        ExpandOption expandOption = null;

        /* 1. Retrieve the requested Entity: can be "normal" read operation, or navigation (to-one) */
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        int segmentCount = resourceParts.size();

        UriResource uriResource = resourceParts.get(0); // in our example, the first segment is the EntitySet

        if (!(uriResource instanceof UriResourceEntitySet)) {
          throw new ODataApplicationException("Only EntitySet is supported",
              HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();

        if (1 == segmentCount) {
            responseEdmEntityType = startEdmEntitySet.getEntityType();
            responseEdmEntitySet = startEdmEntitySet;

            /* 2. Retrieve the data from backend */
            List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();

            responseEntity = this.storage.readEntityData(startEdmEntitySet, keyPredicates);

            /* 3. Apply system query options */

            /* 3a. Handle $expand */
            expandOption = uriInfo.getExpandOption();

            if (null != expandOption) {
                expandEntities(expandOption, startEdmEntitySet, responseEntity);
            }
        } else if (segmentCount == 2) {
            UriResource navSegment = resourceParts.get(1);

            if (navSegment instanceof UriResourceNavigation) {
                UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) navSegment;
                EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                responseEdmEntityType = edmNavigationProperty.getType();

                if (!edmNavigationProperty.containsTarget()) {
                    responseEdmEntitySet = getNavigationTargetEntitySet(startEdmEntitySet,
                            edmNavigationProperty);
                  } else {
                    responseEdmEntitySet = startEdmEntitySet;
                  }

                List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                Entity sourceEntity = this.storage.readEntityData(startEdmEntitySet, keyPredicates);

                List<UriParameter> navKeyPredicates = uriResourceNavigation.getKeyPredicates();

                /* Retrieve related entities */
                if (navKeyPredicates.isEmpty()) {
                  responseEntity = this.storage.getRelatedEntity(sourceEntity,
                          responseEdmEntityType);
                } else {
                  responseEntity = this.storage.getRelatedEntity(sourceEntity,
                          responseEdmEntityType, navKeyPredicates);
                }
            }
        } else {
            throw new ODataApplicationException("Not supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        if (null == responseEntity) {
            throw new ODataApplicationException("Nothing found.",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        /* 3b. Handle $select */
        SelectOption selectOption = uriInfo.getSelectOption();

        String selectList = this.odata.createUriHelper().buildContextURLSelectList(
                responseEdmEntityType, expandOption, selectOption);

        /* 4. Create a serializer based on the requested format (json) */
        ContextURL contextUrl = null;

        if (isContNav(uriInfo)) {
            contextUrl = ContextURL.with()
                    .entitySetOrSingletonOrType(request.getRawODataPath())
                    .selectList(selectList)
                    .suffix(ContextURL.Suffix.ENTITY).build();
        } else {
            contextUrl = ContextURL.with()
                    .entitySet(responseEdmEntitySet)
                    .selectList(selectList)
                    .suffix(ContextURL.Suffix.ENTITY).build();
        }

        EntitySerializerOptions opts = EntitySerializerOptions.with()
                .contextURL(contextUrl)
                .select(selectOption)
                .expand(expandOption)
                .build();

        ODataSerializer serializer = this.odata.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entity(this.serviceMetadata,
            responseEdmEntityType, responseEntity, opts);

        /* 4. Configure the response object: set the body, headers and status code */
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void createEntity(ODataRequest request, ODataResponse response,
                             UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity type from the URI
        EdmEntitySet edmEntitySet = getEdmEntitySet(uriInfo);
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // 2. create the data in backend
        // 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        Entity requestEntity = result.getEntity();

        // 2.2 do the creation in backend, which returns the newly created entity
        Entity createdEntity = this.storage.createEntityData(edmEntitySet, requestEntity);

        // 3. serialize the response (we have to return the created entity)
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

        // expand and select currently not supported
        EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

        ODataSerializer serializer = this.odata.createSerializer(responseFormat);
        SerializerResult serializedResponse = serializer.entity(this.serviceMetadata,
                edmEntityType, createdEntity, options);

        //4. configure the response object
        response.setContent(serializedResponse.getContent());
        response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }

    @Override
    public void updateEntity(ODataRequest request, ODataResponse response,
                             UriInfo uriInfo, ContentType requestFormat, ContentType responseFormat)
            throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity set which belongs to the requested entity
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();

        // Note: only in our example we can assume that the first segment is the EntitySet
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // 2. update the data in backend
        // 2.1. retrieve the payload from the PUT request for the entity to be updated
        InputStream requestInputStream = request.getBody();
        ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
        DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
        Entity requestEntity = result.getEntity();

        // 2.2 do the modification in backend
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();

        // Note that this updateEntity()-method is invoked for both PUT or PATCH operations
        HttpMethod httpMethod = request.getMethod();
        this.storage.updateEntityData(edmEntitySet, keyPredicates, requestEntity, httpMethod);

        //3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    @Override
    public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
            throws ODataApplicationException, ODataLibraryException {

        // 1. Retrieve the entity set which belongs to the requested entity
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        // Note: only in our example we can assume that the first segment is the EntitySet
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // 2. delete the data in backend
        List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
        this.storage.deleteEntityData(edmEntitySet, keyPredicates);

        //3. configure the response object
        response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
    }

    public EdmEntitySet getEdmEntitySet(UriInfoResource uriInfo) throws ODataApplicationException {
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();

        // To get the entity set we have to interpret all URI segments
        if (!(resourcePaths.get(0) instanceof UriResourceEntitySet)) {
            throw new ODataApplicationException("Invalid resource type for first segment.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        UriResourceEntitySet uriResource = (UriResourceEntitySet) resourcePaths.get(0);

        return uriResource.getEntitySet();
    }
}
