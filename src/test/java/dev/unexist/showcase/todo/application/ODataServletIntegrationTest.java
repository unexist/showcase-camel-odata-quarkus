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
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.xmlunit.assertj.XmlAssert.assertThat;

@QuarkusTest
public class ODataServletIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ODataServletIntegrationTest.class);

    @Test
    public void shouldGetOverviewAsXML() {
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
    public void shouldGetOverviewAsJSON() {
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

    @Test
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
                    .contains("OData.Todo.Todo");
    }

    @Test
    public void shouldGetEmptyList() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.[\"value\"]")
                    .isArray()
                    .isEmpty();
    }

    @Test
    public void shouldNotFindAnything() {
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
    public void shouldFindOnlySingleMatch() {
        createTodo();

        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(1)")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.[\"ID\"]")
                .isEqualTo(1);
    }

    @Test
    public void shouldFindMatchByKey() {
        createTodo();

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


    @Test
    public void shouldGetAttributeID() {
        createTodo();

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
    public void shouldCreateNewEntity() throws JsonProcessingException {
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
    public void shouldUpdateAllProperties() throws JsonProcessingException {
        createTodo();

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
    public void shouldUpdateSingleProperty() throws JsonProcessingException {
        createTodo();

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
    public void shouldDeleteEntity() {
        createTodo();

        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .delete("/odata/Todos(1)")
                .then()
                    .statusCode(204)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .isEqualTo("");
    }

    @Test
    public void shouldGetNavigationEntities() {
        createTodo();

        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .delete("/odata/Todos(1)/Tasks")
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

    /**
     * Create an entry via REST
     **/

    private static void createTodo() {
        given()
                .when()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(TodoFixture.createTodo())
                    .post("/todo")
                .then()
                    .statusCode(201);
    }
}