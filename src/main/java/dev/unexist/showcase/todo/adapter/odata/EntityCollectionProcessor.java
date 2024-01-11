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
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

import java.util.List;
import java.util.Locale;

public class EntityCollectionProcessor extends EntityProcessorBase
        implements org.apache.olingo.server.api.processor.EntityCollectionProcessor
{
    /**
     * Constructor
     *
     * @param  storage  A {@link EntityStorage} instance
     **/

    public EntityCollectionProcessor(EntityStorage storage) {
        this.storage = storage;
    }

    public void readEntityCollection(ODataRequest request, ODataResponse response,
                                     UriInfo uriInfo, ContentType responseFormat)
            throws SerializerException, ODataApplicationException
    {
        EdmEntitySet responseEdmEntitySet = null;
        EntityCollection responseEntityCollection = null;
        EdmEntityType responseEdmEntityType = null;

        /* 1. retrieve the requested EntitySet from the uriInfo (representation of the parsed URI) */
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        int segmentCount = resourceParts.size();

        UriResource uriResource = resourceParts.get(0);
        if (!(uriResource instanceof UriResourceEntitySet)) {
          throw new ODataApplicationException("Only EntitySet is supported",
              HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();

        if (1 == segmentCount) {
            responseEdmEntitySet = startEdmEntitySet;
            responseEntityCollection = storage.readEntitySetData(startEdmEntitySet);
        } else if (2 == segmentCount) {
            UriResource lastSegment = resourceParts.get(1);

            if (lastSegment instanceof UriResourceNavigation) {
                UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
                EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                EdmEntityType targetEntityType = edmNavigationProperty.getType();

                if (!edmNavigationProperty.containsTarget()) {
                    responseEdmEntitySet = getNavigationTargetEntitySet(startEdmEntitySet, edmNavigationProperty);
                } else {
                    responseEdmEntitySet = startEdmEntitySet;
                    responseEdmEntityType = targetEntityType;
                }

                // 2. fetch the data from backend
                // first fetch the entity where the first segment of the URI points to
                List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                Entity sourceEntity = storage.readEntityData(startEdmEntitySet, keyPredicates);

                if (null == sourceEntity) {
                  throw new ODataApplicationException("Entity not found.",
                      HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
                }
                // then fetch the entity collection where the entity navigates to
                // note: we don't need to check uriResourceNavigation.isCollection(),
                // because we are the EntityCollectionProcessor
                responseEntityCollection = storage.getRelatedEntityCollection(sourceEntity, targetEntityType);

            }
        } else {
            throw new ODataApplicationException("Not supported",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        ContextURL contextUrl = null;
        EdmEntityType edmEntityType = null;

        /* 3. Create a serializer based on the requested format (json) */
        if (isContNav(uriInfo)) {
          contextUrl = ContextURL.with().entitySetOrSingletonOrType(request.getRawODataPath()).build();
          edmEntityType = responseEdmEntityType;
        } else {
          contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).build();
          edmEntityType = responseEdmEntitySet.getEntityType();
        }

        final String id = request.getRawBaseUri() + "/" + responseEdmEntitySet.getName();
        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
            .contextURL(contextUrl).id(id).build();

        ODataSerializer serializer = odata.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entityCollection(this.serviceMetadata,
                edmEntityType, responseEntityCollection, opts);

        /* 4. Configure the response object: set the body, headers and status code */
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
}
