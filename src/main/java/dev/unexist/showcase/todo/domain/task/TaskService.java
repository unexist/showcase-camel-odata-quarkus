/**
 * @package Showcase-OData-Quarkus
 *
 * @file Task domain service
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.domain.task;

import dev.unexist.showcase.todo.domain.CrudRepository;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class TaskService {

    @Inject
    CrudRepository<Task> taskRepository;

    /**
     * Create new {@link Task} entry and store it in repository
     *
     * @param  base  A {@link TaskBase} entry
     *
     * @return Either id of the entry on success; otherwise {@code -1}
     **/

    public Optional<Task> create(TaskBase base) {
        Task task = new Task(base);

        boolean retval = this.taskRepository.add(task);

        return Optional.ofNullable(retval ? task : null);
    }

    /**
     * Update {@link Task} at with given id
     *
     * @param  id    Id to update
     * @param  base  Values for the entry
     *
     * @return Either {@code true} on success; otherwise {@code false}
     **/

    public boolean update(int id, TaskBase base) {
        Optional<Task> task = this.findById(id);
        boolean ret = false;

        if (task.isPresent()) {
            task.get().update(base);

            ret = this.taskRepository.update(task.get());
        }

        return ret;
    }

    /**
     * Delete {@link Task} with given id
     *
     * @param  id  Id to delete
     *
     * @return Either {@code true} on success; otherwise {@code false}
     **/

    public boolean delete(int id) {
        return this.taskRepository.deleteById(id);
    }

    /**
     * Get all {@link Task} entries
     *
     * @return List of all {@link Task}; might be empty
     **/

    public List<Task> getAll() {
        return this.taskRepository.getAll();
    }

    /**
     * Get all {@link Task} entries by todo id
     *
     * @param  todoId  Id to find
     *
     * @return List of all {@link Task}; might be empty
     **/

    public List<Task> getAllByTodoId(int todoId) {
        return this.taskRepository.findAllByPredicate(t -> t.getTodoId() == todoId);
    }

    /**
     * Find {@link Task} by given id
     *
     * @param  id  Id to look for
     *
     * @return A {@link Optional} of the entry
     **/

    public Optional<Task> findById(int id) {
        return this.taskRepository.findById(id);
    }
}
