/**
 * @package Showcase-OData-Quarkus
 *
 * @file Todo OData EDM provider
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata.processor;

import dev.unexist.showcase.todo.adapter.odata.entity.TaskEntityFactory;
import dev.unexist.showcase.todo.adapter.odata.entity.TodoEntityFactory;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlNavigationPropertyBinding;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;

import java.util.ArrayList;
import java.util.List;

public class EdmProvider extends CsdlAbstractEdmProvider {
    public static final String NAMESPACE = "OData.Todo";
    public static final String CONTAINER_NAME = "Container";

    public static final FullQualifiedName CONTAINER = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    @Override
    public List<CsdlSchema> getSchemas() {
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(NAMESPACE);

        List<CsdlEntityType> entityTypes = new ArrayList<>();
        entityTypes.add(TodoEntityFactory.createEntityType());
        entityTypes.add(TaskEntityFactory.createEntityType());

        schema.setEntityTypes(entityTypes);
        schema.setEntityContainer(getEntityContainer());

        List<CsdlSchema> schemas = new ArrayList<>();
        schemas.add(schema);

        return schemas;
    }

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) {
        CsdlEntityType retVal = null;

        if (TodoEntityFactory.ET_FQN.equals(entityTypeName)) {
            retVal = TodoEntityFactory.createEntityType();
        } else if (TaskEntityFactory.ET_FQN.equals(entityTypeName)) {
            retVal = TaskEntityFactory.createEntityType();
        }

        return retVal;
    }

    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) {
        CsdlEntitySet retVal = null;

        if (CONTAINER.equals(entityContainer)) {
            if (TodoEntityFactory.ES_NAME.equals(entitySetName)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();

                entitySet.setName(TodoEntityFactory.ES_NAME);
                entitySet.setType(TodoEntityFactory.ET_FQN);

                /* Navigational */
                CsdlNavigationPropertyBinding navPropBinding = new CsdlNavigationPropertyBinding();

                navPropBinding.setPath(TaskEntityFactory.ET_NAME);
                navPropBinding.setTarget(TaskEntityFactory.ES_NAME);

                List<CsdlNavigationPropertyBinding> navPropBindingList = new ArrayList<>();

                navPropBindingList.add(navPropBinding);

                entitySet.setNavigationPropertyBindings(navPropBindingList);

                retVal = entitySet;
            }
        }

        return retVal;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() {
        List<CsdlEntitySet> entitySets = new ArrayList<>();
        entitySets.add(getEntitySet(CONTAINER, TodoEntityFactory.ES_NAME));

        CsdlEntityContainer entityContainer = new CsdlEntityContainer();

        entityContainer.setName(CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;
    }

    @Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) {
        CsdlEntityContainerInfo retVal = null;

        // This method is invoked when displaying the service document */
        if (null == entityContainerName || CONTAINER.equals(entityContainerName)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(CONTAINER);

            retVal = entityContainerInfo;
        }

        return retVal;
    }
}
