/**
 * @package Showcase-OData-Quarkus
 *
 * @file OData todo entity processor base
 * @copyright 2024-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata.processor;

import dev.unexist.showcase.todo.adapter.odata.entity.TaskEntityService;
import dev.unexist.showcase.todo.adapter.odata.storage.EntityStorage;
import org.apache.olingo.commons.api.Constants;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Link;
import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmElement;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.edm.EdmNavigationPropertyBinding;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceNavigation;
import org.apache.olingo.server.api.uri.queryoption.ExpandItem;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;

import java.util.List;
import java.util.Locale;

abstract public class EntityProcessorBase {
    protected EntityStorage storage;
    protected ServiceMetadata serviceMetadata;
    protected OData odata;

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

    protected boolean isContNav(UriInfo uriInfo) {
        List<UriResource> resourceParts = uriInfo.getUriResourceParts();

        for (UriResource resourcePart : resourceParts) {
            if (resourcePart instanceof UriResourceNavigation) {
                UriResourceNavigation navResource = (UriResourceNavigation) resourcePart;

                if (navResource.getProperty().containsTarget()) {
                    return true;
                }
            }
        }

        return false;
    }

    protected EdmEntitySet getNavigationTargetEntitySet(EdmEntitySet startEdmEntitySet,
                                                        EdmNavigationProperty edmNavigationProperty)
            throws ODataApplicationException {

        EdmEntitySet navigationTargetEntitySet = null;

        String navPropName = edmNavigationProperty.getName();
        EdmBindingTarget edmBindingTarget = startEdmEntitySet.getRelatedBindingTarget(navPropName);

        if (null == edmBindingTarget) {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        if (edmBindingTarget instanceof EdmEntitySet) {
            navigationTargetEntitySet = (EdmEntitySet) edmBindingTarget;
        } else {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
        }

        return navigationTargetEntitySet;
    }

    protected void expandEntities(ExpandOption expandOption, EdmEntitySet startEdmEntitySet, Entity responseEntity) {
        if (null != expandOption) {
            EdmNavigationProperty edmNavigationProperty = null;
            ExpandItem expandItem = expandOption.getExpandItems().get(0);

            if (expandItem.isStar()) {
                List<EdmNavigationPropertyBinding> bindings =
                        startEdmEntitySet.getNavigationPropertyBindings();

                if (!bindings.isEmpty()) {
                    /* Lookup bindings */
                    for (EdmNavigationPropertyBinding binding : bindings) {
                        EdmElement property = startEdmEntitySet.getEntityType()
                                .getProperty(binding.getPath());

                        EdmElement property2 = startEdmEntitySet.getEntityType()
                                .getProperty(TaskEntityService.ES_NAME);

                        if(property instanceof EdmNavigationProperty) {
                            edmNavigationProperty = (EdmNavigationProperty) property;

                            break;
                        }
                    }
                }
            } else {
                UriResource expandUriResource = expandItem.getResourcePath()
                        .getUriResourceParts().get(0);

                if(expandUriResource instanceof UriResourceNavigation) {
                    edmNavigationProperty = ((UriResourceNavigation) expandUriResource).getProperty();
                }
            }

            if (null != edmNavigationProperty) {
                String navPropName = edmNavigationProperty.getName();
                EdmEntityType expandEdmEntityType = edmNavigationProperty.getType();

                /* Build the inline data */
                Link link = new Link();

                link.setTitle(navPropName);
                link.setType(Constants.ENTITY_NAVIGATION_LINK_TYPE);
                link.setRel(Constants.NS_ASSOCIATION_LINK_REL + navPropName);

                if (edmNavigationProperty.isCollection()) {
                    EntityCollection expandEntityCollection = this.storage.getRelatedEntityCollection(
                            responseEntity, expandEdmEntityType);

                    link.setInlineEntitySet(expandEntityCollection);
                    link.setHref(expandEntityCollection.getId().toASCIIString());
                } else {
                    Entity expandEntity = this.storage.getRelatedEntity(
                            responseEntity, expandEdmEntityType);

                    link.setInlineEntity(expandEntity);
                    link.setHref(expandEntity.getId().toASCIIString());
                }

                responseEntity.getNavigationLinks().add(link);
            }
        }
    }
}
