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

package dev.unexist.showcase.todo.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.unexist.showcase.todo.domain.task.Task;
import dev.unexist.showcase.todo.domain.task.TaskFactory;

public class TaskFixture {
    public static Task createTask() {
        return TaskFactory.fromData(0, 0,
                "Task string", "Task string", false);
    }

    public static Task createTask(int todoId) {
        return TaskFactory.fromData(0, todoId,
                "Task string", "Task string", false);
    }

    public static String createEntityJSON() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();

        root.put("Title", "Task string");
        root.put("Description", "Task string");

        return mapper.writeValueAsString(root);
    }
}
