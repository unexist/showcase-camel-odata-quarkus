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
public class ODataServletSelectIT extends ODataServletBaseIT {

    @Test
    public void shouldSelectFromEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(1)?$select=Title,Description")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.[\"ID\"]")
                .isAbsent();
    }

    @Test
    public void shouldSelectAllFromEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(1)?$select=*")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        final Object expectedObject = json(String.join(System.lineSeparator(),
                "{",
                "\"@odata.context\": \"${json-unit.ignore}\",",
                "\"ID\": \"${json-unit.any-number}\",",
                "\"Title\": \"${json-unit.any-string}\",",
                "\"Description\": \"${json-unit.any-string}\"",
                "}"));

        assertThatJson(jsonOut).isEqualTo(expectedObject);
    }

    @Test
    public void shouldSelectFromEntityCollection() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$select=Title,Description")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        final Object expected = json(String.join(System.lineSeparator(),
                "{",
                "\"Title\": \"${json-unit.any-string}\",",
                "\"Description\": \"${json-unit.any-string}\"",
                "}"));

        assertThatJson(jsonOut)
                .inPath("$.value.[*]")
                .isArray()
                .isNotEmpty()
                .allSatisfy(elem -> assertThatJson(elem).isEqualTo(expected));
    }

    @Test
    public void shouldSelectAllFromEntityCollection() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$select=*")
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
                .inPath("$.value.[*]")
                .isArray()
                .isNotEmpty()
                .allSatisfy(elem -> assertThatJson(elem).isEqualTo(expectedObject));
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

    @Test
    public void shouldExpandEntityCollection() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$expand=Task")
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

    @Test
    public void shouldExpandAllEntityCollection() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$expand=*")
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

    @Test
    public void shouldSelectAndExpandWithNestedSelect() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(1)?$select=Title&$expand=Task($select=Title)")
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
