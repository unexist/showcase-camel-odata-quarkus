/**
 * @package Showcase-OData-Quarkus
 *
 * @file Todo OData resource
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ApplicationScoped
public class EdmProvider extends CsdlAbstractEdmProvider {
    public static final String NAMESPACE = "OData.Todo";
    public static final String CONTAINER_NAME = "Container";
    public static final String ET_PRODUCT_NAME = "Todo";
    public static final String ES_PRODUCTS_NAME = "Todos";

    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);
    public static final FullQualifiedName ET_PRODUCT_FQN = new FullQualifiedName(NAMESPACE, ET_PRODUCT_NAME);

    @Override
    public List<CsdlSchema> getSchemas() {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        List<CsdlEntityType> entityTypes = new ArrayList<>();
        entityTypes.add(getEntityType(ET_PRODUCT_FQN));

        schema.setEntityTypes(entityTypes);
        schema.setEntityContainer(getEntityContainer());

        List<CsdlSchema> schemas = new ArrayList<>();
        schemas.add(schema);

        return schemas;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
        CsdlEntityType retVal = null;

        // this method is called for one of the EntityTypes that are configured in the Schema
        if (entityTypeName.equals(ET_PRODUCT_FQN)) {
            CsdlProperty id = new CsdlProperty()
                    .setName("ID")
                    .setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty name = new CsdlProperty()
                    .setName("Name")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty  description = new CsdlProperty()
                    .setName("Description")
                    .setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // create CsdlPropertyRef for Key element
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("ID");

            // configure EntityType
            CsdlEntityType entityType = new CsdlEntityType();

            entityType.setName(ET_PRODUCT_NAME);
            entityType.setProperties(Arrays.asList(id, name, description));
            entityType.setKey(Collections.singletonList(propertyRef));

            retVal = entityType;
        }

        return retVal;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {
        CsdlEntitySet retVal = null;

        if (entityContainer.equals(CONTAINER)) {
            if (entitySetName.equals(ES_PRODUCTS_NAME)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(ES_PRODUCTS_NAME);
                entitySet.setType(ET_PRODUCT_FQN);

                retVal = entitySet;
            }
        }

        return retVal;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
        List<CsdlEntitySet> entitySets = new ArrayList<>();
        entitySets.add(getEntitySet(CONTAINER, ES_PRODUCTS_NAME));

        CsdlEntityContainer entityContainer = new CsdlEntityContainer();

        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {
        CsdlEntityContainerInfo retVal = null;

        // This method is invoked when displaying the service document at e.g. http://localhost:8080/DemoService/DemoService.svc
        if (null == entityContainerName || entityContainerName.equals(CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);

            retVal = entityContainerInfo;
        }

        return retVal;
    }
}
