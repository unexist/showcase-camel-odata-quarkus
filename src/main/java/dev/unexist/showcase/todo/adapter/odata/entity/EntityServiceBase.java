/**
 * @package Showcase-OData-Quarkus
 *
 * @file OData todo entity service base
 * @copyright 2024-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata.entity;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;

import java.util.function.Predicate;

abstract class EntityServiceBase<T> {
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

    public abstract EntityCollection getAllByPredicate(Predicate<T> filterBy);
}
