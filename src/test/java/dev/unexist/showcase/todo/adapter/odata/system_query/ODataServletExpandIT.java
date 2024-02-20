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

package dev.unexist.showcase.todo.adapter.odata.system_query;

import dev.unexist.showcase.todo.adapter.odata.ODataServletBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;

@QuarkusTest
public class ODataServletExpandIT extends ODataServletBaseIT {
    final Object expectedObject = json(String.join(System.lineSeparator(),
              "{",
              "\"ID\": \"${json-unit.any-number}\",",
              "\"TodoID\": \"${json-unit.any-number}\",",
              "\"Title\": \"${json-unit.any-string}\"",
              "}"));

    @Test
    public void shouldExpandEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(1)?$expand=Tasks")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.Tasks")
                    .isArray()
                    .isNotEmpty()
                    .allSatisfy(elem -> assertThatJson(elem).isEqualTo(expectedObject));
    }

    @Test
    public void shouldExpandAllEntity() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos(1)?$expand=*")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        assertThatJson(jsonOut)
                .inPath("$.Tasks")
                    .isArray()
                    .isNotEmpty()
                    .allSatisfy(elem -> assertThatJson(elem).isEqualTo(expectedObject));
    }

    @Test
    public void shouldExpandEntityCollection() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$expand=Tasks")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                .inPath("$.Tasks")
                    .isArray()
                    .isNotEmpty()
                    .allSatisfy(elem -> assertThatJson(elem).isEqualTo(expectedObject));
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

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                .inPath("$.Tasks")
                    .isArray()
                    .isNotEmpty()
                    .allSatisfy(elem -> assertThatJson(elem).isEqualTo(expectedObject));
    }
}
