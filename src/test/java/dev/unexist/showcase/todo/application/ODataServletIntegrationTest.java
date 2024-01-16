/**
 * @package Showcase
 * @file
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.unexist.showcase.todo.domain.CrudRepository;
import dev.unexist.showcase.todo.domain.task.Task;
import dev.unexist.showcase.todo.domain.todo.Todo;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.xmlunit.assertj.XmlAssert.assertThat;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ODataServletIntegrationTest {

    @Inject
    CrudRepository<Todo> todoRepository;

    @Inject
    CrudRepository<Task> taskRepository;

    /* Init */

    @BeforeEach
    public void shouldCreateDataViaRest() {
        this.todoRepository.clear();
        this.taskRepository.clear();

        this.todoRepository.add(TodoFixture.createTodo());
        this.todoRepository.add(TodoFixture.createTodo());
        this.todoRepository.add(TodoFixture.createTodo());

        this.taskRepository.add(TaskFixture.createTask(1));
        this.taskRepository.add(TaskFixture.createTask(1));
        this.taskRepository.add(TaskFixture.createTask(2));
    }

    /* Service documents */

    @Test
    @Order(1)
    public void shouldGetServiceDocumentAsXML() {
        String xmlOut = given()
                .when()
                    .accept(ContentType.XML)
                    .get("/odata/")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThat(xmlOut)
                .withNamespaceContext(Map.of("metadata", "http://docs.oasis-open.org/odata/ns/metadata"))
                .valueByXPath("//@metadata:context")
                .isEqualTo("$metadata");
    }

    @Test
    @Order(2)
    public void shouldGetServiceDocumentAsJSON() {
        String jsonOut = given()
                .when()
                  .accept(ContentType.JSON)
                    .get("/odata/")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .isObject()
                    .containsEntry("@odata.context", "$metadata");
    }

    /* Metadata */

    @Test
    @Order(3)
    public void shouldGetMetadata() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/$metadata")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.[\"OData.Todo\"]..[\"$Type\"]")
                    .isArray()
                    .containsAll(Arrays.asList("OData.Todo.Todo", "OData.Todo.Task"));
    }

    /* Find */

    @Test
    public void shouldNotFindEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Products")
                .then()
                    .statusCode(404)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.[\"error\"].message")
                    .isString()
                    .startsWith("Cannot find EntitySet");
    }

    @Test
    public void shouldNotFindAnything() {
        given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(99)")
                .then()
                    .statusCode(404);
    }

    @Test
    public void shouldFindAll() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                 .inPath("$.value")
                 .isArray()
                 .isNotEmpty();
    }

    @Test
    public void shouldFindMatchByKey() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(ID=1)")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.[\"ID\"]")
                    .isEqualTo(1);
    }

    /* CRUD Todo */

    @Test
    public void shouldCreateTodoEntity() throws JsonProcessingException {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(TodoFixture.createEntityJSON())
                    .post("/odata/Todos")
                .then()
                    .statusCode(201)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .isObject()
                .containsEntry("@odata.context", "$metadata#Todos");
    }

    @Test
    public void shouldGetAttributeID() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(ID=1)/ID")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.value")
                    .isEqualTo(1);
    }

    @Test
    public void shouldUpdateSingleProperty() throws JsonProcessingException {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(TodoFixture.createEntityJSON())
                    .put("/odata/Todos(1)")
                .then()
                    .statusCode(204)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .isEqualTo("");
    }

    @Test
    public void shouldUpdateAllProperties() throws JsonProcessingException {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(TodoFixture.createEntityJSON())
                    .patch("/odata/Todos(1)")
                .then()
                    .statusCode(204)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .isEqualTo("");
    }

    @Test
    @Order(3)
    public void shouldDeleteEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .delete("/odata/Todos(3)")
                .then()
                    .statusCode(204)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .isEqualTo("");
    }

    /* Navigational */

    @Test
    public void shouldGetNavigationEntities() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(1)/Tasks")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.value")
                .isArray()
                .isNotEmpty();
    }

    @Test
    public void shouldGetNavigationEntityByKey() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(1)/Tasks(1)")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.[\"ID\"]")
                .isEqualTo(1);
    }

    /* System Queries */

    @Test
    public void shouldCountEntities() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$count=true")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .isObject()
                    .containsEntry("@odata.count", 3);
    }

    @Test
    public void shouldGetTopTwoEntities() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$top=2")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.value")
                .isArray()
                .hasSize(2);
    }

    @Test
    public void shouldSkipFirstEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$skip=1")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.value")
                   .isArray()
                    .isNotEmpty();
    }

    @Test
    public void shouldSelectFromEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$select=Title,Description")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                .inPath("$.value.[\"ID\"]")
                .isNull();
    }

    @Test
    public void shouldSelectAllFromEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$select=*")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.value")
                    .isArray()
                    .isNotEmpty();
    }

    @Test
    public void shouldExpandEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(1)?$expand=Task")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.Task")
                    .isObject()
                    .isNotEmpty();
    }
}
