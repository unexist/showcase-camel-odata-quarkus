/**
 * @package Showcase
 * @file
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 *         This program can be distributed under the terms of the Apache License v2.0.
 *         See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.domain.task;

public class Task extends TaskBase {
    private int id;
    private int todoId;

    /**
     * Constructor
     **/

    public Task() {
    }

    /**
     * Constructor
     *
     * @param  base  Base entry
     **/

    public Task(final TaskBase base) {
        this.update(base);
    }

    /**
     * Update values from base
     *
     * @param  base  Task base class
     **/

    public void update(final TaskBase base) {
        this.setTitle(base.getTitle());
        this.setDescription(base.getDescription());
        this.setDone(base.getDone());
    }

    /**
     * Get id of entry
     *
     * @return Id of the entry
     **/

    public int getId() {
        return this.id;
    }

    /**
     * Set id of entry
     *
     * @param  id  Id of the entry
     **/

    public void setId(int id) {
        this.id = id;
    }

    /**
     * Get id of parent entry
     *
     * @return Id of the entry
     **/

    public int getTodoId() {
        return this.todoId;
    }

    /**
     * Set parent id of entry
     *
     * @param  todoId  Id of parent entry
     **/

    public void setTodoId(int todoId) {
        this.todoId = todoId;
    }
}
