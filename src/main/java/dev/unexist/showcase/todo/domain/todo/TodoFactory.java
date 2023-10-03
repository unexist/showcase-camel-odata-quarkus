/**
 * @package Showcase-OData-Quarkus
 *
 * @file Todo factory class
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.domain.todo;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class TodoFactory {

    /**
     * Create a new {@link Todo} entry from given data
     *
     * @param  id            ID of the entry
     * @param  title         Title of the entry
     * @param  description   Description of the entry
     * @param  startDateStr  Start date string
     * @param  dueDateStr    Due date string
     *
     * @return A newly created {@code Todo}
     **/

    public static Todo fromData(int id, String title, String description,
                                String startDateStr, String dueDateStr) {
        Todo todo = new Todo();

        todo.setId(id);
        todo.setTitle(title);
        todo.setDescription(description);

        DueDate dueDate = new DueDate();

        dueDate.setStart(createDate(startDateStr));
        dueDate.setDue(createDate(dueDateStr));

        todo.setDueDate(dueDate);

        return todo;
    }

    /**
     * Create a {@link LocalDate} from given date string
     *
     * @param  dateStr  Date string to use
     *
     * @return A newly created {@code LocalDate}
     **/

    private static LocalDate createDate(String dateStr) {
        Objects.requireNonNull(dateStr, "Date string cannot be null");

        return LocalDate.from(
                DateTimeFormatter.ofPattern(DueDate.DATE_PATTERN).parse(dateStr));
    }
}
