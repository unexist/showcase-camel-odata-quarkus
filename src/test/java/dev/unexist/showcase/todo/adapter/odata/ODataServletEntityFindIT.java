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

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;

@QuarkusTest
public class ODataServletEntityFindIT extends ODataServletBaseIT {

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

        final Object expectedObject = json(String.join(System.lineSeparator(),
                "{",
                "\"ID\": \"${json-unit.any-number}\",",
                "\"Title\": \"${json-unit.any-string}\",",
                "\"Description\": \"${json-unit.any-string}\"",
                "}"));

        assertThatJson(jsonOut)
                 .inPath("$.value")
                 .isArray()
                 .isNotEmpty()
                .allSatisfy(elem -> assertThatJson(elem).isEqualTo(expectedObject));
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
}
