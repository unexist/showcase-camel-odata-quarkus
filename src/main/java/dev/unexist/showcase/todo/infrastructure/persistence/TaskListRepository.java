/**
 * @package Showcase-OData-Quarkus
 *
 * @file Task list repository
 * @copyright 2022-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.infrastructure.persistence;

import dev.unexist.showcase.todo.domain.CrudRepository;
import dev.unexist.showcase.todo.domain.task.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@ApplicationScoped
public class TaskListRepository implements CrudRepository<Task> {
    private static final Logger LOGGER = LoggerFactory.getLogger(TaskListRepository.class);

    private final List<Task> list;

    /**
     * Constructor
     **/

    public TaskListRepository() {
        this.list = new ArrayList<>();
    }

    @Override
    public boolean add(final Task task) {
        task.setId(this.list.size() + 1);

        return this.list.add(task);
    }

    @Override
    public boolean update(final Task task) {
        boolean ret = false;

        try {
            this.list.set(task.getId(), task);

            ret = true;
        } catch (IndexOutOfBoundsException e) {
            LOGGER.warn("update: id={} not found", task.getId());
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
    public List<Task> getAll() {
        return Collections.unmodifiableList(this.list);
    }

    @Override
    public Optional<Task> findById(int id) {
        return this.list.stream()
                .filter(t -> t.getId() == id)
                .findFirst();
    }

    @Override
    public Optional<Task> findByPredicate(Predicate<Task> filterBy) {
        return this.list.stream()
                .filter(filterBy)
                .findFirst();
    }
}
