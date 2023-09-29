/**
 * @package Showcase-OData-Quarkus
 *
 * @file K repository interface
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.domain;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public interface CrudRepository<K> {

    /**
     * Add {@link K} entry to list
     *
     * @param  k  {@link K} entry to add
     *
     * @return Either {@code true} on success; otherwise {@code false}
     **/

    boolean add(K k);

    /**
     * Update {@link K} with given id
     *
     * @param  k  A {@link K} to update
     *
     * @return Either {@code true} on success; otherwise {@code false}
     **/

    boolean update(K k);

    /**
     * Delete {@link K} with given id
     *
     * @param  id  Id to delete
     *
     * @return Either {@code true} on success; otherwise {@code false}
     **/

    boolean deleteById(int id);

    /**
     * Get all {@link K} entries
     *
     * @return List of all stored {@link K}
     **/

    List<K> getAll();

    /**
     * Find {@link K} by given id
     *
     * @param  id  Id to find
     *
     * @return A {@link Optional} with the result of the lookup
     **/

    Optional<K> findById(int id);

    /**
     * Find {@link K} by given {@link Predicate}
     *
     * @param  filterBy  A {@link Predicate} to use
     *
     * @return A {@link Optional} with the result of the lookup
     **/

    Optional<K> findByPredicate(Predicate<K> filterBy);
}
