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

@QuarkusTest
public class ODataServletComplexQueriesIT extends ODataServletBaseIT {

    /* System Queries */

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
