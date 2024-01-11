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

package dev.unexist.showcase.todo.adapter.odata;

import org.apache.olingo.commons.api.edm.EdmBindingTarget;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmNavigationProperty;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceNavigation;

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
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        if (edmBindingTarget instanceof EdmEntitySet) {
            navigationTargetEntitySet = (EdmEntitySet) edmBindingTarget;
        } else {
            throw new ODataApplicationException("Not supported.",
                    HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
        }

        return navigationTargetEntitySet;
    }
}
