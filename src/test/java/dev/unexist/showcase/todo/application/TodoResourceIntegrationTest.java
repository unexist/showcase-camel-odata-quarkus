/**
 * @package Showcase-Hadoop-CDC-Quarkus
 *
 * @file Stupid integration test
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.application;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;

@QuarkusTest
public class TodoResourceIntegrationTest {

    @Test
    public void shouldGetEmptyList() {
        given()
          .when().get("/todo")
          .then()
             .statusCode(204);
    }

    @Test
    public void shouldCreateNewTodo() {
        given()
                .when()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(TodoFixture.createTodo())
                    .post("/todo")
                .then()
                    .statusCode(201);
    }

    @Test
    public void shouldNotFindAnything() {
        given()
                .when()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(TodoFixture.createTodo())
                    .post("/todo/11")
                .then()
                    .statusCode(404);
    }

    @Test
    public void shouldCreateNewTask() {
        given()
                .when()
                    .accept(ContentType.JSON)
                    .contentType(ContentType.JSON)
                    .body(TaskFixture.createTask())
                    .post("/task")
                .then()
                    .statusCode(201);
    }
}