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
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import java.net.URI;
import java.net.URISyntaxException;

abstract class EntityFactoryBase<T> {
    public static CsdlEntityType createEntityType() {
        throw new ODataRuntimeException("Unable to create entity type");
    }

    public Entity createEntityFrom(T t) {
        throw new ODataRuntimeException("Unable to create entity");
    }

    public Entity createEntity(Entity entity) {
        throw new ODataRuntimeException("Unable to create entity");
    }

    public abstract void updateEntity(Entity entity);

    public abstract void deleteEntity(Entity entity);

    public abstract EntityCollection getAll();

    /**
     * Create an ID from given values
     *
     * @param  entitySetName   Name for the entity set part in the ID
     * @param  id              ID part of the created URI
     *
     * @return A newly created {@link URI}
     **/

    static URI createId(String entitySetName, Object id) {
        try {
            return new URI(String.format("%s(%s)", entitySetName, id));
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}
