/**
 * @package Showcase
 * @file
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata;

import dev.unexist.showcase.todo.domain.todo.Todo;
import dev.unexist.showcase.todo.domain.todo.TodoRepository;
import dev.unexist.showcase.todo.infrastructure.persistence.ListTodoRepository;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

public class TodoEntityStorage {

    TodoRepository repository;

    public TodoEntityStorage() {
        this.repository = new ListTodoRepository();
    }

    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) throws ODataApplicationException {

        // actually, this is only required if we have more than one Entity Sets
        if (TodoEdmProvider.ES_TODOS_NAME.equals(edmEntitySet.getName())) {
            return getTodos();
        }

        return null;
    }

    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException{

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (TodoEdmProvider.ET_TODO_NAME.equals(edmEntityType.getName())) {
            return getTodo(edmEntityType, keyParams);
        }

        return null;
    }

    private EntityCollection getTodos(){
        EntityCollection retEntitySet = new EntityCollection();

        for(Todo todo : this.repository.getAll()){
            retEntitySet.getEntities().add(createEntity(todo));
        }

        return retEntitySet;
    }

    private Entity createEntity(Todo todo) {
        Entity entity = new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, todo.getId()))
                .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, todo.getTitle()))
                .addProperty(new Property(null, "Description", ValueType.PRIMITIVE, todo.getDescription()));

        entity.setId(createId("Todos", 1));

        return entity;
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(String.format("%s(%s)", entitySetName, String.valueOf(id)));
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }

    private Entity getTodo(EdmEntityType edmEntityType, List<UriParameter> keyParams) throws ODataApplicationException {
        EntityCollection entitySet = getTodos();

        /*  generic approach  to find the requested entity */
        Entity requestedEntity = Util.findEntity(edmEntityType, entitySet, keyParams);

        if (requestedEntity == null) {
            // this variable is null if our data doesn't contain an entity for the requested key
            // Throw suitable exception
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return requestedEntity;
    }
}
