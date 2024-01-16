/**
 * @package Showcase-OData-Quarkus
 *
 * @file Todo list repository
 * @copyright 2022-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.infrastructure.persistence;

import dev.unexist.showcase.todo.domain.CrudRepository;
import dev.unexist.showcase.todo.domain.todo.Todo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@ApplicationScoped
public class TodoListRepository implements CrudRepository<Todo> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TodoListRepository.class);

    private final List<Todo> list;

    /**
     * Constructor
     **/

    public TodoListRepository() {
        this.list = new ArrayList<>();
    }

    @Override
    public boolean add(final Todo todo) {
        todo.setId(this.list.size() + 1);

        return this.list.add(todo);
    }

    @Override
    public boolean update(final Todo todo) {
        boolean ret = false;

        try {
            this.list.set(todo.getId(), todo);

            ret = true;
        } catch (IndexOutOfBoundsException e) {
            LOGGER.warn("update: id={} not found", todo.getId());
        }

        return ret;
    }

    @Override
    public boolean deleteById(int id) {
        boolean ret = false;

        try {
            this.list.remove(id);

            ret = true;
        } catch (IndexOutOfBoundsException e) {
            LOGGER.warn("deleteById: id={} not found", id);
        }

        return ret;
    }

    @Override
    public List<Todo> getAll() {
        return Collections.unmodifiableList(this.list);
    }

    @Override
    public Optional<Todo> findById(int id) {
        return this.list.stream()
                .filter(t -> t.getId() == id)
                .findFirst();
    }

    @Override
    public Optional<Todo> findByPredicate(Predicate<Todo> filterBy) {
        return this.list.stream()
                .filter(filterBy)
                .findFirst();
    }

    @Override
    public List<Todo> findAllByPredicate(Predicate<Todo> filterBy) {
        return this.list.stream()
                .filter(filterBy)
                .collect(Collectors.toUnmodifiableList());
    }

    @Override
    public void clear() {
        this.list.clear();
    }
}
