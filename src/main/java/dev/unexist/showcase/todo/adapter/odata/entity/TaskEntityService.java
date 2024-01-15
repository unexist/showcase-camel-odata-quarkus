/**
 * @package Showcase-OData-Quarkus
 *
 * @file
 * @copyright 2024-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata.entity;

import dev.unexist.showcase.todo.domain.task.Task;
import dev.unexist.showcase.todo.domain.task.TaskBase;
import dev.unexist.showcase.todo.domain.task.TaskService;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static dev.unexist.showcase.todo.adapter.odata.processor.EdmProvider.NAMESPACE;

@ApplicationScoped
public class TaskEntityService extends EntityServiceBase<Task> {
    public static final String ET_NAME = "Task";
    public static final String ES_NAME = "Tasks";

    public static final FullQualifiedName ET_FQN = new FullQualifiedName(NAMESPACE, ET_NAME);

    @Inject
    TaskService taskService;

    static public CsdlEntityType createEntityType() {
        CsdlProperty id = new CsdlProperty()
                .setName("ID")
                .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
        CsdlProperty todoId = new CsdlProperty()
                .setName("TodoID")
                .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
        CsdlProperty title = new CsdlProperty()
                .setName("Title")
                .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

        /* Navigational */
        CsdlNavigationProperty navProp = new CsdlNavigationProperty()
                .setName(TodoEntityService.ET_NAME)
                .setType(TodoEntityService.ET_FQN)
                .setNullable(false)
                .setPartner(ES_NAME);

        List<CsdlNavigationProperty> navPropList = new ArrayList<>();

        navPropList.add(navProp);

        /* Create CsdlPropertyRef for Key element */
        CsdlPropertyRef propertyRef = new CsdlPropertyRef();

        propertyRef.setName("ID");

        /* Configure EntityType */
        CsdlEntityType entityType = new CsdlEntityType();

        entityType.setName(ET_NAME);
        entityType.setProperties(Arrays.asList(id, todoId, title));
        entityType.setKey(Collections.singletonList(propertyRef));
        entityType.setNavigationProperties(navPropList);

        return entityType;
    }

    /**
     * Create new entity from given {@link Task}
     *
     * @param  task  A {@link Task} to convert
     *
     * @return A newly created {@link Entity}
     **/

    public Entity createEntityFrom(Task task) {
        Entity entity = new Entity()
                .addProperty(new Property(null, "ID",
                        ValueType.PRIMITIVE, task.getId()))
                .addProperty(new Property(null, "TodoID",
                        ValueType.PRIMITIVE, task.getTodoId()))
                .addProperty(new Property(null, "Title",
                        ValueType.PRIMITIVE, task.getTitle()));

        entity.setId(createId(ES_NAME, task.getId()));

        return entity;
    }

    /**
     * Create new entity
     *
     * @param  entity  A {@link Entity} to apply properties to
     *
     * @return Updated {@link Entity}
     **/

    public Entity createEntity(Entity entity) {
        Objects.requireNonNull(entity, "Entity not found");

        TaskBase taskBase = new TaskBase();

        Optional<Task> task = this.taskService.create(taskBase);

        if (task.isPresent()) {
            entity.getProperties().add(
                new Property(null, "ID",
                        ValueType.PRIMITIVE, task.get().getId()));
            entity.getProperties().add(
                    new Property(null, "TodoID",
                            ValueType.PRIMITIVE, task.get().getId()));
        }

        return entity;
    }

    /**
     * Update entity based on given parameters
     *
     * @param  entity  A {@link Entity} to apply properties to
     **/

    public void updateEntity(Entity entity) {
        Objects.requireNonNull(entity, "Entity not found");
    }

    /**
     * Delete entity based on given parameters
     *
     * @param  entity  A {@link Entity} to apply properties to
     **/

    public void deleteEntity(Entity entity) {
        Objects.requireNonNull(entity, "Entity not found");

        Integer existingID = (Integer)entity.getProperty("ID").getValue();

        this.taskService.delete(existingID);
    }

    /**
     * Get all entities
     *
     * @return A {@link EntityCollection} with all entries
     **/

    public EntityCollection getAll() {
        EntityCollection entityCollection = new EntityCollection();

        for (Task task : this.taskService.getAll()) {
            entityCollection.getEntities().add(createEntityFrom(task));
        }

        return entityCollection;
    }

    /**
     * Find all {@link Task} entries by given {@link Predicate}
     *
     * @param  filterBy  A {@link Predicate} to use
     *
     * @return A {@link EntityCollection} of all {@link Task}; might be empty
     **/

    public EntityCollection getAllByPredicate(Predicate<Task> filterBy) {
        EntityCollection collection = new EntityCollection();

        collection.getEntities().addAll(
                this.taskService.findAllByPredicate(filterBy).stream()
                        .map(this::createEntityFrom)
                        .collect(Collectors.toList()));

        return collection;
    }
}
