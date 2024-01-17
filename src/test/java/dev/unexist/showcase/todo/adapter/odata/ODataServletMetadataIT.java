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
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.xmlunit.assertj.XmlAssert.assertThat;

@QuarkusTest
public class ODataServletMetadataIT extends ODataServletBaseIT {

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
}
