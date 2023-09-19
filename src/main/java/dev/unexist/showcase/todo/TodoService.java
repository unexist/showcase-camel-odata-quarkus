/**
 * @package Showcase-Camel-OData-Quarkus
 *
 * @file
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo;

import io.quarkus.runtime.annotations.RegisterForReflection;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Optional;

@RegisterForReflection
@ApplicationScoped
public class TodoService {

    @Inject
    private TodoRepository todoRepository;

    /**
     * Create new {@link Todo} entry and store it in repository
     *
     * @param todo
     *         A {@link Todo} to add
     *
     * @return Either {@code true} on success; otherwise {@code false}
     **/

    public boolean create(Todo todo) {
        this.todoRepository.add(todo);

        return true;
    }

    /**
     * Update {@link Todo} at with given id
     *
     * @param id
     *         Id to update
     * @param values
     *         Values for the entry
     *
     * @return Either {@code true} on success; otherwise {@code false}
     **/

    public boolean update(int id, Todo values) {
        Optional<Todo> todo = this.findById(id);
        boolean ret = false;

        if (todo.isPresent()) {
            values.setId(todo.get().getId());

            ret = this.todoRepository.update(values);
        }

        return ret;
    }

    /**
     * Delete {@link Todo} with given id
     *
     * @param id
     *         Id to delete
     *
     * @return Either {@code true} on success; otherwise {@code false}
     **/

    public boolean delete(int id) {
        return this.todoRepository.deleteById(id);
    }

    /**
     * Get all {@link Todo} entries
     *
     * @return List of all {@link Todo}; might be empty
     **/

    public List<Todo> getAll() {
        return this.todoRepository.getAll();
    }

    /**
     * Find {@link Todo} by given id
     *
     * @param id
     *         Id to look for
     *
     * @return A {@link Optional} of the entry
     **/

    public Optional<Todo> findById(int id) {
        return this.todoRepository.findById(id);
    }
}
