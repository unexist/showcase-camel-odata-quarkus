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

import dev.unexist.showcase.todo.domain.todo.Todo;
import dev.unexist.showcase.todo.domain.todo.TodoRepository;
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
    TodoRepository repository;

    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet)
            throws ODataApplicationException {
        LOGGER.info(String.format("entity=%s", edmEntitySet.getName()));

        if (TodoEdmProvider.ES_TODOS_NAME.equals(edmEntitySet.getName())) {
            return getTodos();
        }

        return null;
    }

    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams)
            throws ODataApplicationException {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        if (TodoEdmProvider.ET_TODO_NAME.equals(edmEntityType.getName())) {
            return getTodo(edmEntityType, keyParams);
        }

        return null;
    }

    public Entity createEntityData(EdmEntitySet edmEntitySet, Entity requestEntity) {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(TodoEdmProvider.ET_TODO_NAME)) {
            return createTodo(edmEntityType, requestEntity);
        }

        return null;
    }

    public void updateEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams,
                                 Entity updateEntity, HttpMethod httpMethod)
            throws ODataApplicationException {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(TodoEdmProvider.ET_TODO_NAME)) {
            updateTodo(edmEntityType, keyParams, updateEntity, httpMethod);
        }
    }

    public void deleteEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams)
            throws ODataApplicationException {
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
        if (edmEntityType.getName().equals(TodoEdmProvider.ET_TODO_NAME)) {
            deleteTodo(edmEntityType, keyParams);
        }
    }

    private EntityCollection getTodos() {
        EntityCollection entityCollection = new EntityCollection();

        for (Todo todo : this.repository.getAll()) {
            entityCollection.getEntities().add(createEntity(todo));
        }

        return entityCollection;
    }

    private Entity getTodo(EdmEntityType edmEntityType, List<UriParameter> keyParams) throws ODataApplicationException {
        /*  generic approach  to find the requested entity */
        Entity requestedEntity = findTodo(edmEntityType, keyParams);

        if (null == requestedEntity) {
            throw new ODataApplicationException("Entity for requested key doesn't exist",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        return requestedEntity;
    }

    private Entity createTodo(EdmEntityType edmEntityType, Entity entity) {

        Todo todo = new Todo();

        this.repository.add(todo);

        entity.getProperties().add(new Property(null, "ID", ValueType.PRIMITIVE, todo.getId()));

        return entity;

    }

    private void updateTodo(EdmEntityType edmEntityType, List<UriParameter> keyParams,
                            Entity entity, HttpMethod httpMethod) throws ODataApplicationException {

        Entity productEntity = getTodo(edmEntityType, keyParams);
        if (null == productEntity) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        // loop over all properties and replace the values with the values of the given payload
        // Note: ignoring ComplexType, as we don't have it in our odata model
        List<Property> existingProperties = productEntity.getProperties();
        for (Property existingProp : existingProperties) {
            String propName = existingProp.getName();

            // ignore the key properties, they aren't updateable
            if (isKey(edmEntityType, propName)) {
                continue;
            }

            Property updateProperty = entity.getProperty(propName);
            // the request payload might not consider ALL properties, so it can be null
            if (null == updateProperty) {
                // if a property has NOT been added to the request payload
                // depending on the HttpMethod, our behavior is different
                if (httpMethod.equals(HttpMethod.PATCH)) {
                    // as of the OData spec, in case of PATCH, the existing property is not touched
                    continue; // do nothing
                } else if (httpMethod.equals(HttpMethod.PUT)) {
                    // as of the OData spec, in case of PUT, the existing property is set to null (or to default value)
                    existingProp.setValue(existingProp.getValueType(), null);

                    continue;
                }
            }

            // change the value of the properties
            existingProp.setValue(existingProp.getValueType(), updateProperty.getValue());
        }
    }

    private void deleteTodo(EdmEntityType edmEntityType, List<UriParameter> keyParams)
            throws ODataApplicationException {

        Entity todoEntity = getTodo(edmEntityType, keyParams);
        if (null == todoEntity) {
            throw new ODataApplicationException("Entity not found",
                    HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
        }

        Integer existingID = (Integer)todoEntity.getProperty("ID").getValue();

        this.repository.deleteById(existingID);
    }

    public Entity findTodo(EdmEntityType edmEntityType, List<UriParameter> keyParams)
            throws ODataApplicationException {
        Entity retVal = null;

        for (Todo todo : this.repository.getAll()) {
            Entity todoEntity = createEntity(todo);

            if (entityMatchesAllKeys(edmEntityType, todoEntity, keyParams)) {
                retVal = todoEntity;

                break;
            }
        }

        return retVal;
    }

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

    private Entity createEntity(Todo todo) {
        Entity entity = new Entity()
                .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, todo.getId()))
                .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, todo.getTitle()))
                .addProperty(new Property(null, "Description", ValueType.PRIMITIVE, todo.getDescription()));

        entity.setId(createId("Todos", todo.getId()));

        return entity;
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(String.format("%s(%s)", entitySetName, id));
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }

    public boolean entityMatchesAllKeys(EdmEntityType edmEntityType, Entity rt_entity, List<UriParameter> keyParams)
            throws ODataApplicationException {
        for (final UriParameter key : keyParams) {
            String keyName = key.getName();
            String keyText = key.getText();

            // Edm: we need this info for the comparison below
            EdmProperty edmKeyProperty = (EdmProperty) edmEntityType.getProperty(keyName);

            Boolean isNullable = edmKeyProperty.isNullable();
            Integer maxLength = edmKeyProperty.getMaxLength();
            Integer precision = edmKeyProperty.getPrecision();
            Boolean isUnicode = edmKeyProperty.isUnicode();
            Integer scale = edmKeyProperty.getScale();

            // get the EdmType in order to compare
            EdmType edmType = edmKeyProperty.getType();
            EdmPrimitiveType edmPrimitiveType = (EdmPrimitiveType) edmType;

            // Runtime data: the value of the current entity
            Object valueObject = rt_entity.getProperty(keyName).getValue(); // null-check is done in FWK

            // now need to compare the valueObject with the keyText String
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
