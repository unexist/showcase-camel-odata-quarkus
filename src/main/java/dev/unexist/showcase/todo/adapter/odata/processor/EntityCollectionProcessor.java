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

package dev.unexist.showcase.todo.adapter.odata.processor;

import dev.unexist.showcase.todo.adapter.odata.storage.EntityStorage;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmProperty;
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
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.ExpressionVisitException;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import java.util.Iterator;
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
        EntityCollection entityCollection = null;
        ExpandOption expandOption = null;

        /* 1. Retrieve the requested EntitySet from the uriInfo (representation of the parsed URI) */
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();
        int segmentCount = resourceParts.size();

        UriResource uriResource = resourceParts.get(0);
        if (!(uriResource instanceof UriResourceEntitySet)) {
          throw new ODataApplicationException("Only EntitySet is supported",
              HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) uriResource;
        EdmEntitySet startEdmEntitySet = uriResourceEntitySet.getEntitySet();

        if (1 == segmentCount) {
            responseEdmEntitySet = startEdmEntitySet;

            /* 2. Fetch the data from backend */
            entityCollection = this.storage.readEntitySetData(startEdmEntitySet);
        } else if (2 == segmentCount) {
            UriResource lastSegment = resourceParts.get(1);

            if (lastSegment instanceof UriResourceNavigation) {
                UriResourceNavigation uriResourceNavigation = (UriResourceNavigation) lastSegment;
                EdmNavigationProperty edmNavigationProperty = uriResourceNavigation.getProperty();
                EdmEntityType targetEntityType = edmNavigationProperty.getType();

                if (!edmNavigationProperty.containsTarget()) {
                    responseEdmEntitySet = getNavigationTargetEntitySet(startEdmEntitySet,
                            edmNavigationProperty);
                } else {
                    responseEdmEntitySet = startEdmEntitySet;
                    responseEdmEntityType = targetEntityType;
                }

                /* 2. Fetch the data from backend */
                List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
                Entity sourceEntity = this.storage.readEntityData(startEdmEntitySet, keyPredicates);

                if (null == sourceEntity) {
                    throw new ODataApplicationException("Entity not found.",
                            HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
                }

                entityCollection = this.storage.getRelatedEntityCollection(sourceEntity,
                        targetEntityType);
            }
        } else {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        /* 3. Apply system query options */
        List<Entity> entityList = entityCollection.getEntities();

        /* 3a. Handle $filter */
        FilterOption filterOption = uriInfo.getFilterOption();

        if (null != filterOption) {
            try {
                Iterator<Entity> entityIterator = entityList.iterator();

                while (entityIterator.hasNext()) {
                    Entity currentEntity = entityIterator.next();
                    Expression filterExpression = filterOption.getExpression();
                    FilterExpressionVisitor expressionVisitor = new FilterExpressionVisitor(currentEntity);

                    Object visitorResult = filterExpression.accept(expressionVisitor);

                    if (visitorResult instanceof Boolean) {
                        if (!Boolean.TRUE.equals(visitorResult)) {
                            entityIterator.remove();
                        }
                    } else {
                        throw new ODataApplicationException("A filter expression must evaluate to type Edm.Boolean",
                                HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
                    }
                }
            } catch (ExpressionVisitException e) {
                throw new ODataApplicationException("Exception in filter evaluation",
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH);
            }
        }

        /* 3b. Handle $orderby */
        OrderByOption orderByOption = uriInfo.getOrderByOption();

        if (null != orderByOption) {
            List<OrderByItem> orderItemList = orderByOption.getOrders();
            final OrderByItem orderByItem = orderItemList.get(0); // in our example we support only one

            Expression expression = orderByItem.getExpression();

            if (expression instanceof Member) {
                UriInfoResource expResourcePath = ((Member)expression).getResourcePath();
                UriResource expUriResource = expResourcePath.getUriResourceParts().get(0);

                if (expUriResource instanceof UriResourcePrimitiveProperty) {
                    EdmProperty edmProperty = ((UriResourcePrimitiveProperty)expUriResource).getProperty();
                    final String sortPropertyName = edmProperty.getName();

                    /* Delegate the sorting to the native sorter of Integer and String */
                    entityList.sort((entity1, entity2) -> {
                        int compareResult = 0;

                        if (sortPropertyName.equals("ID")) {
                            Integer integer1 = (Integer) entity1.getProperty(
                                    sortPropertyName).getValue();
                            Integer integer2 = (Integer) entity2.getProperty(
                                    sortPropertyName).getValue();

                            compareResult = integer1.compareTo(integer2);
                        } else {
                            String propertyValue1 = String.valueOf(
                                    entity1.getProperty(sortPropertyName).getValue());
                            String propertyValue2 = String.valueOf(
                                    entity2.getProperty(sortPropertyName).getValue());

                            compareResult = propertyValue1.compareTo(propertyValue2);
                        }

                        /* If 'desc' is specified in the URI, change the order of the list */
                        if (orderByItem.isDescending()) {
                            return -compareResult;
                        }

                        return compareResult;
                    });
                }
            }
        }

        /* 3c. Handle $select */
        SelectOption selectOption = uriInfo.getSelectOption();

        responseEntityCollection = new EntityCollection();

        /* 3d. Handle $count */
        CountOption countOption = uriInfo.getCountOption();

        if (null != countOption) {
            boolean isCount = countOption.getValue();

            if (isCount) {
                responseEntityCollection.setCount(entityList.size());
            }
        }

        /* 3e. Handle $skip */
        SkipOption skipOption = uriInfo.getSkipOption();

        if (null != skipOption) {
            int skipNumber = skipOption.getValue();

            if (0 <= skipNumber) {
                if(skipNumber <= entityList.size()) {
                    entityList = entityList.subList(skipNumber, entityList.size());
                } else {
                    entityList.clear();
                }
            } else {
                throw new ODataApplicationException("Invalid value for $skip",
                        HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
        }

        /* 3f. Handle $top */
        TopOption topOption = uriInfo.getTopOption();

        if (topOption != null) {
            int topNumber = topOption.getValue();

            if (0 <= topNumber) {
                if(topNumber <= entityList.size()) {
                    entityList = entityList.subList(0, topNumber);
                }
            } else {
                throw new ODataApplicationException("Invalid value for $top",
                        HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
            }
        }

        /* Update collection */
        responseEntityCollection.getEntities().addAll(entityList);

        /* 4. Create a serializer based on the requested format (json) */
        ContextURL contextUrl = null;
        EdmEntityType edmEntityType = null;

        if (isContNav(uriInfo)) {
            edmEntityType = responseEdmEntityType;

            String selectList = this.odata.createUriHelper().buildContextURLSelectList(
                    edmEntityType, expandOption, selectOption);

            contextUrl = ContextURL.with()
                    .entitySetOrSingletonOrType(request.getRawODataPath())
                    .selectList(selectList)
                    .build();
        } else {
            edmEntityType = responseEdmEntitySet.getEntityType();

            String selectList = this.odata.createUriHelper().buildContextURLSelectList(
                    edmEntityType, expandOption, selectOption);

            contextUrl = ContextURL.with()
                    .entitySet(responseEdmEntitySet)
                    .selectList(selectList)
                    .build();
        }

        final String id = request.getRawBaseUri() + "/" + responseEdmEntitySet.getName();
        EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with()
                .contextURL(contextUrl)
                .id(id)
                .count(countOption)
                .select(selectOption)
                .expand(expandOption)
                .build();

        ODataSerializer serializer = this.odata.createSerializer(responseFormat);
        SerializerResult serializerResult = serializer.entityCollection(this.serviceMetadata,
                edmEntityType, responseEntityCollection, opts);

        /* 5. Configure the response object: set the body, headers and status code */
        response.setContent(serializerResult.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }
}
