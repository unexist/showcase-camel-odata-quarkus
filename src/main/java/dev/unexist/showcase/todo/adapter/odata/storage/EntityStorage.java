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

package dev.unexist.showcase.todo.adapter.odata.storage;

import dev.unexist.showcase.todo.adapter.odata.entity.TaskEntityService;
import dev.unexist.showcase.todo.adapter.odata.entity.TodoEntityService;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class EntityStorage {

    @Inject
    TodoEntityService todoEntityService;

    @Inject
    TaskEntityService taskEntityService;

    /**
     * Read data from an entity collection
     *
     * @param  edmEntitySet  A {@link EdmEntitySet} to use
     *
     * @return Either found {@link EntityCollection} on success; otherwise {@code null}
     **/

    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet) {
        EntityCollection retVal = null;

        if (TodoEntityService.ES_NAME.equals(edmEntitySet.getName())) {
            retVal = this.todoEntityService.getAll();
        } else if (TaskEntityService.ES_NAME.equals(edmEntitySet.getName())) {
            retVal = this.taskEntityService.getAll();
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

        if (TodoEntityService.ET_NAME.equals(edmEntityType.getName())) {
            retVal = this.todoEntityService.createEntity(requestEntity);
        } else if (TaskEntityService.ET_NAME.equals(edmEntityType.getName())) {
            retVal = this.taskEntityService.createEntity(requestEntity);
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
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        /* Try to find entity */
        Entity retVal = getEntity(edmEntityType, keyParams);

        if (null == retVal) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return retVal;
    }

    /**
     * Update entity based on given update entity
     *
     * @param  edmEntitySet   A {@link EdmEntitySet} to use
     * @param  keyParams      A list of URI parameters
     * @param  updateEntity   A {@link Entity} to update
     * @param  httpMethod     A {@link HttpMethod} for this call
     *
     * @throws ODataApplicationException
     */

    public void updateEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams,
                                 Entity updateEntity, HttpMethod httpMethod)
            throws ODataApplicationException
    {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        /* Try to find entity */
        Entity foundEntity = getEntity(edmEntityType, keyParams);

        if (null == foundEntity) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        /* Loop over all properties and replace the values with the values of the given payload */
        List<Property> existingProperties = foundEntity.getProperties();

        for (Property existingProp : existingProperties) {
            String propName = existingProp.getName();

            /* Ignore the key properties, they aren't updatable */
            if (isKey(edmEntityType, propName)) {
                continue;
            }

            Property updateProperty = foundEntity.getProperty(propName);

            if (null == updateProperty) {
                /* If a property has NOT been added to the request payload depending on the
                HttpMethod, our behavior is different */
                if (httpMethod.equals(HttpMethod.PATCH)) {
                    /* As of the OData spec, in case of PATCH, the existing property is not touched */
                    continue;
                } else if (httpMethod.equals(HttpMethod.PUT)) {
                    existingProp.setValue(existingProp.getValueType(), null);

                    continue;
                }
            }

            existingProp.setValue(existingProp.getValueType(),
                    updateProperty.getValue());
        }

        /* Finally update entity */
        if (TodoEntityService.ET_NAME.equals(edmEntityType.getName())) {
            this.taskEntityService.updateEntity(foundEntity);
        } else if (TaskEntityService.ET_NAME.equals(edmEntityType.getName())) {
            this.taskEntityService.updateEntity(foundEntity);
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
            throws ODataApplicationException
    {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        /* Try to find entity */
        Entity foundEntity = getEntity(edmEntityType, keyParams);

        if (TodoEntityService.ET_NAME.equals(edmEntityType.getName())) {
            this.todoEntityService.deleteEntity(foundEntity);
        } else if (TaskEntityService.ET_NAME.equals(edmEntityType.getName())) {
            this.taskEntityService.deleteEntity(foundEntity);
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

        if (TodoEntityService.ET_FQN.getFullQualifiedNameAsString().equals(sourceEntityFqn)
                && relatedEntityFqn.equals(TaskEntityService.ET_FQN))
        {
            int todoId = (Integer) sourceEntity.getProperty("ID").getValue();

            navigationTargetEntityCollection.setId(createId(sourceEntity,
                    "ID", TaskEntityService.NAV_NAME));
            navigationTargetEntityCollection.getEntities().addAll(
                    this.taskEntityService.getAllByPredicate(t -> t.getTodoId() == todoId)
                            .getEntities());
        } else if (TaskEntityService.ET_FQN.getFullQualifiedNameAsString().equals(sourceEntityFqn)
                && relatedEntityFqn.equals(TodoEntityService.ET_FQN))
        {
            int todoId = (Integer) sourceEntity.getProperty("TodoID").getValue();

            navigationTargetEntityCollection.setId(createId(sourceEntity,
                    "ID", TodoEntityService.NAV_NAME));
            navigationTargetEntityCollection.getEntities().addAll(
                    this.todoEntityService.getAllByPredicate(t -> t.getId() == todoId)
                            .getEntities());
        }

        return navigationTargetEntityCollection;
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

    private Entity getEntity(EdmEntityType edmEntityType, List<UriParameter> keyParams)
            throws ODataApplicationException
    {
        /* FIXME: Try to find entity in a wasteful way */
        Entity foundEntity = null;

        if (TodoEntityService.ET_NAME.equals(edmEntityType.getName())) {
            foundEntity = findEntity(edmEntityType, this.todoEntityService.getAll(), keyParams);
        } else if (TaskEntityService.ET_NAME.equals(edmEntityType.getName())) {
            foundEntity = findEntity(edmEntityType, this.taskEntityService.getAll(), keyParams);
        }

        if (null == foundEntity) {
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return foundEntity;
    }

    /**
     * Find entity based on given parameters
     *
     * @param  edmEntityType  A {@link EdmEntityType} to use
     * @param  entitySet      A {@link EntityCollection} to use
     * @param  keyParams      A list of URI parameters
     *
     * @return Either found {@link Entity}; otherwise {@code null}
     * @throws ODataApplicationException
     **/

    private Entity findEntity(EdmEntityType edmEntityType, EntityCollection entitySet,
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
            throws ODataApplicationException
    {
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

            /* Now we need to compare the valueObject with the keyText String */
            try {
                String valueAsString = edmPrimitiveType.valueToString(valueObject, isNullable, maxLength,
                        precision, scale, isUnicode);

                if (valueAsString == null || !valueAsString.equals(keyText)) {
                    return false;
                }
            } catch (EdmPrimitiveTypeException e) {
                throw new ODataApplicationException("Failed to retrieve String value.",
                        HttpStatusCode.INTERNAL_SERVER_ERROR.getStatusCode(), Locale.ENGLISH, e);
            }
        }

        return true;
    }

    /**
     * Create an ID from given values
     *
     * @param  entity          A #{@link Entity} this uri is for
     * @param  idPropertyName  Name of the ID property
     *
     * @return A newly created {@link URI}
     **/

    public static URI createId(Entity entity, String idPropertyName) {
        return createId(entity, idPropertyName, null);
    }

    public static URI createId(Entity entity, String idPropertyName, String navigationName) {
        try {
            final Property property = entity.getProperty(idPropertyName);

            StringBuilder sb = new StringBuilder(getEntitySetName(entity))
                    .append("(")
                    .append(property.asPrimitive())
                    .append(")");

            if(null != navigationName) {
                sb.append("/").append(navigationName);
            }

            return new URI(sb.toString());
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create (Atom) id for entity: " + entity, e);
        }
    }

    public static String getEntitySetName(Entity entity) {
        if(TodoEntityService.ET_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
            return TodoEntityService.ES_NAME;
        } else if(TaskEntityService.ET_FQN.getFullQualifiedNameAsString().equals(entity.getType())) {
            return TaskEntityService.ES_NAME;
        }

        return entity.getType();
    }
}
