/**
 * @package Showcase-OData-Quarkus
 *
 * @file Task factory class
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.domain.task;

public class TaskFactory {

    /**
     * Create a new {@link Task} entry from given data
     *
     * @param  id            ID of the entry
     * @param  title         Title of the entry
     * @param  description   Description of the entry
     * @param  isDone        Whether the entry is marked as done
     *
     * @return A newly created {@code Task}
     **/

    public static Task fromData(int id, String title, String description, boolean isDone) {
        Task task = new Task();

        task.setId(id);
        task.setTitle(title);
        task.setDescription(description);
        task.setDone(isDone);

        return task;
    }
}
