/**
 * @package Showcase-showcase-odata-quarkus
 * @file
 * @copyright 2024-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 *         This program can be distributed under the terms of the Apache License v2.0.
 *         See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata.entity;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

interface EntityFactoryBase<T> {
    static CsdlEntityType createEntityType() {
        throw new ODataRuntimeException("Unable to create entity type");
    }

    default Entity createEntityFrom(T t) {
        throw new ODataRuntimeException("Unable to create entity");
    }

    default Entity createEntity(EdmEntityType edmEntityType, Entity entity) {
        throw new ODataRuntimeException("Unable to create entity");
    }

    void updateEntity(EdmEntityType edmEntityType, Entity entity);

    void deleteEntity(EdmEntityType edmEntityType,
                      List<UriParameter> keyParams) throws ODataApplicationException;

    EntityCollection getAll();

    /**
     * Create an ID from given values
     *
     * @param  entitySetName   Name for the entity set part in the ID
     * @param  id              ID part of the created URI
     *
     * @return A newly created {@link URI}
     **/

    default URI createId(String entitySetName, Object id) {
        try {
            return new URI(String.format("%s(%s)", entitySetName, id));
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}
