/**
 * @package Showcase-OData-Quarkus
 *
 * @file
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter.odata;

import com.fasterxml.jackson.core.JsonProcessingException;
import dev.unexist.showcase.todo.adapter.TodoFixture;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

public class ODataServletEntityCRUDIT extends ODataServletBaseIT {

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
}
