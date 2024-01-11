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

import dev.unexist.showcase.todo.domain.todo.Todo;
import dev.unexist.showcase.todo.domain.todo.TodoBase;
import dev.unexist.showcase.todo.domain.todo.TodoService;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import static dev.unexist.showcase.todo.adapter.odata.processor.EdmProvider.NAMESPACE;

@ApplicationScoped
public class TodoEntityFactory implements EntityFactoryBase {
    public static final String ET_NAME = "Todo";
    public static final String ES_NAME = "Todos";

    public static final FullQualifiedName ET_FQN = new FullQualifiedName(NAMESPACE, ET_NAME);

    @Inject
    TodoService todoService;

    public static CsdlEntityType createEntityType() {
        CsdlProperty id = new CsdlProperty()
                .setName("ID")
                .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
        CsdlProperty title = new CsdlProperty()
                .setName("Title")
                .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
        CsdlProperty  description = new CsdlProperty()
                .setName("Description")
                .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

        /* Navigational */
        CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                .setName(TaskEntityFactory.ET_NAME)
                .setType(TaskEntityFactory.ET_FQN)
                .setNullable(false)
                .setPartner(ET_NAME);

        List<CsdlNavigationProperty> navPropList = new ArrayList<>();

        navPropList.add(navProp);

        /* Create CsdlPropertyRef for Key element */
        CsdlPropertyRef propertyRef = new CsdlPropertyRef();

        propertyRef.setName("ID");

        /* Configure EntityType */
        CsdlEntityType entityType = new CsdlEntityType();

        entityType.setName(ET_NAME);
        entityType.setProperties(Arrays.asList(id, title, description));
        entityType.setKey(Collections.singletonList(propertyRef));
        entityType.setNavigationProperties(navPropList);

        return entityType;
    }

    /**
     * Create new entity
     *
     * @param  entity  A {@link Entity} to apply properties to
     *
     * @return Updated {@link Entity}
     **/

    public Entity createEntity(Entity entity) {
        TodoBase todoBase = new TodoBase();

        Optional<Todo> todo = this.todoService.create(todoBase);

        if (todo.isPresent()) {
            entity.getProperties().add(
                new Property(null, "ID",
                        ValueType.PRIMITIVE, todo.get().getId()));
        }

        return entity;
    }

    /**
     * Create new entity from given {@link Todo}
     *
     * @param  todo  A {@link Todo} to convert
     *
     * @return A newly created {@link Entity}
     **/

    public Entity createEntityFrom(Todo todo) {
        Entity entity = new Entity()
                .addProperty(new Property(null, "ID",
                        ValueType.PRIMITIVE, todo.getId()))
                .addProperty(new Property(null, "Title",
                        ValueType.PRIMITIVE, todo.getTitle()))
                .addProperty(new Property(null, "Description",
                        ValueType.PRIMITIVE, todo.getDescription()));

        entity.setId(createId(ES_NAME, todo.getId()));

        return entity;
    }

    /**
     * Get all entities
     *
     * @return A {@link EntityCollection} with all entries
     **/

    public EntityCollection getAll() {
        EntityCollection entityCollection = new EntityCollection();

        for (Todo todo : this.todoService.getAll()) {
            entityCollection.getEntities().add(createEntityFrom(todo));
        }

        return entityCollection;
    }

    /**
     * Delete entity based on given parameters
     *
     * @param  edmEntityType  A {@link EdmEntityType} to use
     * @param  keyParams      A list of URI parameters
     *
     * @throws ODataApplicationException
     **/

    public void deleteEntity(EdmEntityType edmEntityType, List<UriParameter> keyParams)
            throws ODataApplicationException {

        Entity todoEntity = getTodo(edmEntityType, keyParams);

        if (null == todoEntity) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        Integer existingID = (Integer)todoEntity.getProperty("ID").getValue();

        this.todoService.delete(existingID);
    }
}
