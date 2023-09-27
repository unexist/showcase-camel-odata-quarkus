/**
 * @package Showcase
 * @file
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 *         This program can be distributed under the terms of the Apache License v2.0.
 *         See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.application;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

@QuarkusTest
public class ODataServletTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(ODataServletTest.class);

    @Test
    public void shouldGetOverview() {
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
                    .containsEntry("@odata.context", "$metadata")
                .node("value")
                    .isArray()
                    .hasSize(1);
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
}
