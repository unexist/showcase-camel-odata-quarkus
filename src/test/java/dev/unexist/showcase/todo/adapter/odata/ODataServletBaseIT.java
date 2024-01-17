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

import dev.unexist.showcase.todo.adapter.TaskFixture;
import dev.unexist.showcase.todo.adapter.TodoFixture;
import dev.unexist.showcase.todo.domain.CrudRepository;
import dev.unexist.showcase.todo.domain.task.Task;
import dev.unexist.showcase.todo.domain.todo.Todo;
import org.junit.jupiter.api.BeforeEach;

import javax.inject.Inject;

public class ODataServletBaseIT {

    @Inject
    CrudRepository<Todo> todoRepository;

    @Inject
    CrudRepository<Task> taskRepository;

    /* Init */

    @BeforeEach
    public void beforeEach() {
        this.todoRepository.clear();
        this.taskRepository.clear();

        this.todoRepository.add(TodoFixture.createTodo());
        this.todoRepository.add(TodoFixture.createTodo());
        this.todoRepository.add(TodoFixture.createTodo());

        this.taskRepository.add(TaskFixture.createTask(1));
        this.taskRepository.add(TaskFixture.createTask(1));
        this.taskRepository.add(TaskFixture.createTask(2));
    }
}
