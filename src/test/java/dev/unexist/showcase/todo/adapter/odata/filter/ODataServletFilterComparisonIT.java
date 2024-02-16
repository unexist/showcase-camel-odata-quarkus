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

package dev.unexist.showcase.todo.adapter.odata.filter;

import dev.unexist.showcase.todo.adapter.odata.ODataServletBaseIT;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.json;

@QuarkusTest
public class ODataServletFilterComparisonIT extends ODataServletBaseIT {

    @Test
    public void shouldFilterEqual() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$filter=ID eq 1")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                .inPath("$.value..[\"ID\"]")
                    .isArray()
                    .isEqualTo(json("[3,2,1]"));
    }


    @Test
    public void shouldFilterNotEqual() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$filter=ID ne 1")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                .inPath("$.value..[\"ID\"]")
                    .isArray()
                    .isEqualTo(json("[3,2,1]"));
    }

    @Test
    public void shouldFilterGreaterThan() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$filter=ID ge 1")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                .inPath("$.value..[\"ID\"]")
                    .isArray()
                    .isEqualTo(json("[3,2,1]"));
    }


    @Test
    public void shouldFilterLessThan() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$filter=ID le 2")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                .inPath("$.value..[\"ID\"]")
                    .isArray()
                    .isEqualTo(json("[3,2,1]"));
    }

    @Test
    public void shouldFilterUnary() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$filter=-ID eq -1")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                .inPath("$.value..[\"ID\"]")
                    .isArray()
                    .isEqualTo(json("[3,2,1]"));
    }

    @Test
    public void shouldFilterNegated() {
        String jsonOut = given()
                .when()
                    .accept(ContentType.JSON)
                    .get("/odata/Todos?$filter=not(ID eq 1)")
                .then()
                    .statusCode(200)
                .and()
                    .extract()
                    .asString();

        System.out.println(jsonOut);

        assertThatJson(jsonOut)
                .inPath("$.value..[\"ID\"]")
                    .isArray()
                    .isEqualTo(json("[3,2,1]"));
    }
}
