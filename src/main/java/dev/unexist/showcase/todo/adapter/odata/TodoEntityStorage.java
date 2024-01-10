/**
 * @package Showcase-OData-Quarkus
 *
 * @file OData todo entity storage
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata;

import dev.unexist.showcase.todo.domain.CrudRepository;
import dev.unexist.showcase.todo.domain.task.Task;
import dev.unexist.showcase.todo.domain.todo.Todo;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmKeyPropertyRef;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeException;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.edm.EdmType;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class TodoEntityStorage {
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoEntityStorage.class);

    @Inject
    CrudRepository<Todo> todoRepository;

    @Inject
    CrudRepository<Task> taskRepository;

    /**
     * Read data from an entity collection
     *
     * @param  edmEntitySet  A {@link EdmEntitySet} to use
     *
     * @return Either found {@link EntityCollection} on success; otherwise {@code null}
     *
     * @throws ODataApplicationException
     **/

    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) {
        EntityCollection retVal = null;

        LOGGER.info(String.format("entity=%s", edmEntitySet.getName()));

        if (TodoEdmProvider.ES_TODOS_NAME.equals(edmEntitySet.getName())) {
            retVal = getTodos();
        } else if (TodoEdmProvider.ES_TASK_NAME.equals(edmEntitySet.getName())) {
            retVal = getTasks();
        }

        return retVal;
    }

    /**
     * Read data from an entity
     *
     * @param  edmEntitySet  A {@link EdmEntitySet} to use
     * @param  keyParams     A list of URI parameters
     *
     * @return Either found {@link Entity} on success; otherwise {@code null}
     *
     * @throws ODataApplicationException
     */

    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams)
            throws ODataApplicationException
    {
        Entity retVal = null;
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        if (TodoEdmProvider.ET_TODO_NAME.equals(edmEntityType.getName())) {
            retVal = getTodo(edmEntityType, keyParams);
        } else if (TodoEdmProvider.ET_TASK_NAME.equals(edmEntityType.getName())) {
            retVal = getTask(edmEntityType, keyParams);
        }

        return retVal;
    }

    /**
     * Create new entity from request entity
     *
     * @param  edmEntitySet   A {@link EdmEntitySet} to use
     * @param  requestEntity  A {@link Entity} to update
     *
     * @return Either updated {@link Entity} on success; otherwise {@code null}
     */

    public Entity createEntityData(EdmEntitySet edmEntitySet, Entity requestEntity) {
        Entity retVal = null;
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        if (TodoEdmProvider.ET_TODO_NAME.equals(edmEntityType.getName())) {
            retVal = createTodo(edmEntityType, requestEntity);
        } else if (TodoEdmProvider.ET_TASK_NAME.equals(edmEntityType.getName())) {
            retVal = getTask(edmEntityType, requestEntity);
        }

        return retVal;
    }

    /**
     * Update entity based on given update entity
     *
     * @param  edmEntitySet   A {@link EdmEntitySet} to use
     * @param  keyParams      A list of URI parameters
     * @param  updateEntity   A {@link Entity} to update
     * @param  httpMethod
     *
     * @throws ODataApplicationException
     */

    public void updateEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams,
                                 Entity updateEntity, HttpMethod httpMethod)
            throws ODataApplicationException
    {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        if (TodoEdmProvider.ET_TODO_NAME.equals(edmEntityType.getName())) {
            updateTodo(edmEntityType, keyParams, updateEntity, httpMethod);
        } else if (TodoEdmProvider.ET_TASK_NAME.equals(edmEntityType.getName())) {
            updateTask(edmEntityType, keyParams, updateEntity, httpMethod);
        }
    }

    /**
     * Delete entity based on given data
     *
     * @param  edmEntitySet   A {@link EdmEntitySet} to use
     * @param  keyParams      A list of URI parameters
     *
     * @throws ODataApplicationException
     **/

    public void deleteEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams)
            throws ODataApplicationException {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        if (TodoEdmProvider.ET_TODO_NAME.equals(edmEntityType.getName())) {
            deleteTodo(edmEntityType, keyParams);
        } else if (TodoEdmProvider.ET_TASK_NAME.equals(edmEntityType.getName())) {
            deleteTask(edmEntityType, keyParams);
        }
    }

    public Entity getRelatedEntity(Entity entity, EdmEntityType relatedEntityType) {
        EntityCollection collection = getRelatedEntityCollection(entity, relatedEntityType);

        if (collection.getEntities().isEmpty()) {
            return null;
        }

        return collection.getEntities().get(0);
    }

    public Entity getRelatedEntity(Entity entity, EdmEntityType relatedEntityType,
                                   List<UriParameter> keyPredicates)
            throws ODataApplicationException
    {
        EntityCollection relatedEntities = getRelatedEntityCollection(entity, relatedEntityType);

        return findEntity(relatedEntityType, relatedEntities, keyPredicates);
    }

    public EntityCollection getRelatedEntityCollection(Entity sourceEntity, EdmEntityType targetEntityType) {
        EntityCollection navigationTargetEntityCollection = new EntityCollection();
        FullQualifiedName relatedEntityFqn = targetEntityType.getFullQualifiedName();
        String sourceEntityFqn = sourceEntity.getType();

        if (sourceEntityFqn.equals(TodoEdmProvider.ET_TODO_FQN.getFullQualifiedNameAsString())
                && relatedEntityFqn.equals(TodoEdmProvider.ET_TASK_FQN))
        {
            int todoId = (Integer) sourceEntity.getProperty("ID").getValue();

            for (Task task : this.taskRepository.findAllByPredicate(t -> t.getTodoId() == todoId)) {
                navigationTargetEntityCollection.getEntities().add(createTaskEntity(task));
            }
        } else if (sourceEntityFqn.equals(TodoEdmProvider.ET_TASK_FQN.getFullQualifiedNameAsString())
                && relatedEntityFqn.equals(TodoEdmProvider.ET_TODO_FQN))
        {
            int todoId = (Integer) sourceEntity.getProperty("TodoID").getValue();

             for (Todo todo : this.todoRepository.findAllByPredicate(t -> t.getId() == todoId)) {
                 navigationTargetEntityCollection.getEntities().add(createTodoEntity(todo));
             }
        }

        if (navigationTargetEntityCollection.getEntities().isEmpty()) {
          return null;
        }

        return navigationTargetEntityCollection;
    }

    /**
     * Get all entities
     *
     * @return A {@link EntityCollection} with all entries
     **/

    private EntityCollection getTodos() {
        EntityCollection entityCollection = new EntityCollection();

        for (Todo todo : this.todoRepository.getAll()) {
            entityCollection.getEntities().add(createTodoEntity(todo));
        }

        return entityCollection;
    }

    /**
     * Get a single entity based on given data
     *
     * @param  edmEntityType  A {@link EdmEntityType} to use
     * @param  keyParams      A list of URI parameters
     *
     * @return Either found {@link Entity}; otherwise {@code null}
     *
     * @throws ODataApplicationException
     **/

    private Entity getTodo(EdmEntityType edmEntityType, List<UriParameter> keyParams)
            throws ODataApplicationException
    {
        Entity requestedEntity = findEntity(edmEntityType, keyParams);

        if (null == requestedEntity) {
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return requestedEntity;
    }

    /**
     * Create new entity
     *
     * @param  edmEntityType  A {@link EdmEntityType} to use
     * @param  entity         A {@link Entity} to apply properties to
     *
     * @return Updated {@link Entity}
     **/

    private Entity createTodo(EdmEntityType edmEntityType, Entity entity) {
        Todo todo = new Todo();

        this.todoRepository.add(todo);

        entity.getProperties().add(
                new Property(null, "ID", ValueType.PRIMITIVE, todo.getId()));

        return entity;
    }

    /**
     * Update entity based on given parameters
     *
     * @param  edmEntityType  A {@link EdmEntityType} to use
     * @param  keyParams      A list of URI parameters
     * @param  entity         A {@link Property} to update
     * @param  httpMethod     {@link HttpMethod} used for the update call
     *
     * @throws ODataApplicationException
     **/

    private void updateTodo(EdmEntityType edmEntityType, List<UriParameter> keyParams,
                            Entity entity, HttpMethod httpMethod)
            throws ODataApplicationException
    {
        Entity productEntity = getTodo(edmEntityType, keyParams);

        if (null == productEntity) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        /* Loop over all properties and replace the values with the values of the given payload */
        List<Property> existingProperties = productEntity.getProperties();

        for (Property existingProp : existingProperties) {
            String propName = existingProp.getName();

            /* Ignore the key properties, they aren't updateable */
            if (isKey(edmEntityType, propName)) {
                continue;
            }

            Property updateProperty = entity.getProperty(propName);
            if (null == updateProperty) {
                /* If a property has NOT been added to the request payload depending on the HttpMethod, our behavior is different */
                if (httpMethod.equals(HttpMethod.PATCH)) {
                    /* As of the OData spec, in case of PATCH, the existing property is not touched */
                    continue;
                } else if (httpMethod.equals(HttpMethod.PUT)) {
                    existingProp.setValue(existingProp.getValueType(), null);

                    continue;
                }
            }

            existingProp.setValue(existingProp.getValueType(), updateProperty.getValue());
        }
    }

    /**
     * Delete entity based on given parameters
     *
     * @param  edmEntityType  A {@link EdmEntityType} to use
     * @param  keyParams      A list of URI parameters
     *
     * @throws ODataApplicationException
     **/

    private void deleteTodo(EdmEntityType edmEntityType, List<UriParameter> keyParams)
            throws ODataApplicationException {

        Entity todoEntity = getTodo(edmEntityType, keyParams);

        if (null == todoEntity) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        Integer existingID = (Integer)todoEntity.getProperty("ID").getValue();

        this.todoRepository.deleteById(existingID);
    }

    /**
     * Find entity based on given parameters
     *
     * @param  edmEntityType  A {@link EdmEntityType} to use
     * @param  entitySet      A {@link EntityCollection} to use
     * @param  keyParams      A list of URI parameters
     *
     * @return Either found {@ink Entity}; otherwise {@code null}
     * @throws ODataApplicationException
     **/

    public Entity findEntity(EdmEntityType edmEntityType, EntityCollection entitySet,
                             List<UriParameter> keyParams)
            throws ODataApplicationException
    {
        Entity retVal = null;
        List<Entity> entityList = entitySet.getEntities();

        for (Entity entity : entityList) {
            boolean foundEntity = entityMatchesAllKeys(edmEntityType, entity, keyParams);

            if (foundEntity) {
                retVal = entity;
                break;
            }
        }

        return retVal;
    }

    /**
     *  Check whether given property is a reference key
     *
     * @param  edmEntityType  A {@link EdmEntityType} to use
     * @param  propertyName   Property name to check
     *
     * @return Either {@code true} if the key is a ref key; otherwise {@code false}
     **/

    private boolean isKey(EdmEntityType edmEntityType, String propertyName) {
        boolean retVal = false;

        List<EdmKeyPropertyRef> keyPropertyRefs = edmEntityType.getKeyPropertyRefs();

        for (EdmKeyPropertyRef propRef : keyPropertyRefs) {
            String keyPropertyName = propRef.getName();

            if (keyPropertyName.equals(propertyName)) {
                retVal = true;

                break;
            }
        }

        return retVal;
    }

    /**
     * Create new entity from given {@link Todo}
     *
     * @param  todo  A {@link Todo} to convert
     *
     * @return A newly created {@link Entity}
     **/

    private Entity createTodoEntity(Todo todo) {
        Entity entity = new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, todo.getId()))
                .addProperty(new Property(null, "Title", ValueType.PRIMITIVE, todo.getTitle()))
                .addProperty(new Property(null, "Description", ValueType.PRIMITIVE, todo.getDescription()));

        entity.setId(createId("Todos", todo.getId()));

        return entity;
    }

    /**
     * Create new entity from given {@link Task}
     *
     * @param  task  A {@link Task} to convert
     *
     * @return A newly created {@link Entity}
     **/

    private Entity createTaskEntity(Task task) {
        Entity entity = new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, task.getId()))
                .addProperty(new Property(null, "TodoID", ValueType.PRIMITIVE, task.getTodoId()))
                .addProperty(new Property(null, "Title", ValueType.PRIMITIVE, task.getTitle()));

        entity.setId(createId("Tasks", task.getId()));

        return entity;
    }

    /**
     * Create an ID from given values
     *
     * @param  entitySetName   Name for the entity set part in the ID
     * @param  id              ID part of the created URI
     *
     * @return A newly created {@link URI}
     **/

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(String.format("%s(%s)", entitySetName, id));
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }

    /**
     * Match all given keys
     *
     * @param  edmEntityType  A {@link EdmEntityType} to use
     * @param  rt_entity      A {@link Entity} to return
     * @param  keyParams      A list of URI parameters
     *
     * @return Either {@code true} if the entity matches; otherwise {@code false}
     * @throws ODataApplicationException
     **/

    public boolean entityMatchesAllKeys(EdmEntityType edmEntityType, Entity rt_entity, List<UriParameter> keyParams)
            throws ODataApplicationException {
        for (final UriParameter key : keyParams) {
            String keyName = key.getName();
            String keyText = key.getText();

            /* We need this info for the comparison below */
            EdmProperty edmKeyProperty = (EdmProperty) edmEntityType.getProperty(keyName);

            Boolean isNullable = edmKeyProperty.isNullable();
            Integer maxLength = edmKeyProperty.getMaxLength();
            Integer precision = edmKeyProperty.getPrecision();
            Boolean isUnicode = edmKeyProperty.isUnicode();
            Integer scale = edmKeyProperty.getScale();

            /* get the EdmType in order to compare */
            EdmType edmType = edmKeyProperty.getType();
            EdmPrimitiveType edmPrimitiveType = (EdmPrimitiveType) edmType;

            /* Runtime data: the value of the current entity */
            Object valueObject = rt_entity.getProperty(keyName).getValue(); // null-check is done in FWK

            /* Now need to compare the valueObject with the keyText String */
            String valueAsString = null;
            try {
                valueAsString = edmPrimitiveType.valueToString(valueObject, isNullable, maxLength,
                        precision, scale, isUnicode);
            } catch (EdmPrimitiveTypeException e) {
                throw new ODataApplicationException("Failed to retrieve String value",
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
            }

            if (valueAsString == null || !valueAsString.equals(keyText)) {
                return false;
            }
        }

        return true;
    }
}
