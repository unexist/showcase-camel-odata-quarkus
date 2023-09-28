/**
 * @package Showcase-OData-Quarkus
 *
 * @file Todo text fixture
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.unexist.showcase.todo.domain.todo.Todo;
import dev.unexist.showcase.todo.domain.todo.TodoFactory;

public class TodoFixture {
    private static final String DATE = "2021-05-07";

    public static Todo createTodo() {
        return TodoFactory.createTodoFromData(0,
                "string", "string", false,
                DATE, DATE);
    }

    public static String createEntityJSON() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();

        root.put("Title", "string");
        root.put("Description", "string");

        return mapper.writeValueAsString(root);
    }
}
