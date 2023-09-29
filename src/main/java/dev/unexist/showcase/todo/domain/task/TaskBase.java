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

import com.fasterxml.jackson.annotation.JsonInclude;

import javax.validation.constraints.NotBlank;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TaskBase {
    @NotBlank
    private String title;

    @NotBlank
    private String description;

    private Boolean done;

    /**
     * Get title of the entry
     *
     * @return Title of the entry
     **/

    public String getTitle() {
        return title;
    }

    /**
     * Set title of the entry
     *
     * @param  title  Title of the entry
     **/

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Get description of entry
     *
     * @return Description of the entry
     **/

    public String getDescription() {
        return description;
    }

    /**
     * Set description of the entry
     *
     * @param description
     *          Description of the entry
     **/

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get done state of entry
     *
     * @return Done state of the entry
     **/

    public Boolean getDone() {
        return done;
    }

    /**
     * Set done state of entry
     *
     * @param  done  Done state of the entry
     **/

    public void setDone(Boolean done) {
        this.done = done;
    }
}
