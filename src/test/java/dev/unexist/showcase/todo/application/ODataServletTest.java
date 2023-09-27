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
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class ODataServletTest {

    @Test
    public void shouldGetMetadata() {
        String jsonOut = given()
                .when().get("/odata/$metadata")
                .then()
                .statusCode(200)
                .and()
                .extract()
                .asString();

    }
}
