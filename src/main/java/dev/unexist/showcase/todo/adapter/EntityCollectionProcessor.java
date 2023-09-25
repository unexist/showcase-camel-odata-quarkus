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

import dev.unexist.showcase.todo.domain.todo.Todo;
import dev.unexist.showcase.todo.domain.todo.TodoRepository;
import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

@ApplicationScoped
public class EntityCollectionProcessor implements org.apache.olingo.server.api.processor.EntityCollectionProcessor {

    @Inject
    TodoRepository repository;

    private OData odata;
    private ServiceMetadata serviceMetadata;

    // our processor is initialized with the OData context object
    public void init(OData initOdata, ServiceMetadata initServiceMetadata) {
        this.odata = initOdata;
        this.serviceMetadata = initServiceMetadata;
    }

    // the only method that is declared in the EntityCollectionProcessor interface
    // this method is called, when the user fires a request to an EntitySet
    // in our example, the URL would be:
    // http://localhost:8080/ExampleService1/ExampleServlet1.svc/Products
    public void readEntityCollection(ODataRequest request, ODataResponse response,
                                     UriInfo uriInfo, ContentType responseFormat) throws SerializerException {

        // 1st we have retrieve the requested EntitySet from the uriInfo object (representation of the parsed service URI)
        List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
        UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet)resourcePaths.get(0);
        EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

        // 2nd: fetch the data from backend for this requested EntitySetName // it has to be delivered as EntitySet object
        EntityCollection entitySet = getData(edmEntitySet);

        // 3rd: create a serializer based on the requested format (json)
        ODataSerializer serializer = odata.createSerializer(responseFormat);

        // 4th: Now serialize the content: transform from the EntitySet object to InputStream
        EdmEntityType edmEntityType = edmEntitySet.getEntityType();
        ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

        final String id = request.getRawBaseUri() + "/" + edmEntitySet.getName();
        EntityCollectionSerializerOptions opts =
                EntityCollectionSerializerOptions.with().id(id).contextURL(contextUrl).build();
        SerializerResult serializedContent = serializer.entityCollection(serviceMetadata, edmEntityType, entitySet, opts);

        // Finally: configure the response object: set the body, headers and status code
        response.setContent(serializedContent.getContent());
        response.setStatusCode(HttpStatusCode.OK.getStatusCode());
        response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
    }


    /**
     * Helper method for providing some sample data
     *
     * @param edmEntitySet for which the data is requested
     * @return data of requested entity set
     */
    private EntityCollection getData(EdmEntitySet edmEntitySet) {
        EntityCollection todosCollection = new EntityCollection();

        if (EdmProvider.ES_PRODUCTS_NAME.equals(edmEntitySet.getName())) {
            List<Entity> todoList = todosCollection.getEntities();

            Iterable<Todo> todos = repository.getAll();

            for (Todo todo : todos) {
                Entity entity = new Entity()
                        .addProperty(new Property(null, "ID", ValueType.PRIMITIVE, todo.getId()))
                        .addProperty(new Property(null, "Name", ValueType.PRIMITIVE, todo.getTitle()));
                entity.setId(createId("Todos", 1));

                todoList.add(entity);
            }
        }

        return todosCollection;
    }

    private URI createId(String entitySetName, Object id) {
        try {
            return new URI(entitySetName + "(" + String.valueOf(id) + ")");
        } catch (URISyntaxException e) {
            throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
        }
    }
}
