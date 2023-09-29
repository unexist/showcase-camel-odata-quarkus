/**
 * @package Showcase-OData-Quarkus
 *
 * @file Todo resource
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter;

import dev.unexist.showcase.todo.domain.task.Task;
import dev.unexist.showcase.todo.domain.task.TaskBase;
import dev.unexist.showcase.todo.domain.task.TaskService;
import dev.unexist.showcase.todo.domain.todo.Todo;
import dev.unexist.showcase.todo.domain.todo.TodoBase;
import dev.unexist.showcase.todo.domain.todo.TodoService;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.List;
import java.util.Optional;

@Path("/todo")
public class TodoResource {

    @Inject
    TodoService todoService;

    @Inject
    TaskService taskService;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create new todo")
    @Tag(name = "Todo")
    @APIResponses({
            @APIResponse(responseCode = "201", description = "Todo created"),
            @APIResponse(responseCode = "406", description = "Bad data"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Response create(TodoBase todoBase, @Context UriInfo uriInfo) {
        Response.ResponseBuilder builder;

        Optional<Todo> todo = this.todoService.create(todoBase);

        if (todo.isPresent()) {
            URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(Integer.toString(todo.get().getId()))
                    .build();

            builder = Response.created(uri);
        } else {
            builder = Response.status(Response.Status.NOT_ACCEPTABLE);
        }

        return builder.build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get all todos")
    @Tag(name = "Todo")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "List of todo", content =
                @Content(schema = @Schema(type = SchemaType.ARRAY, implementation = Todo.class))),
            @APIResponse(responseCode = "204", description = "Nothing found"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Response getAll() {
        List<Todo> todoList = this.todoService.getAll();

        Response.ResponseBuilder builder;

        if (todoList.isEmpty()) {
            builder = Response.noContent();
        } else {
            builder = Response.ok(Entity.json(todoList));
        }

        return builder.build();
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get todo by id")
    @Tag(name = "Todo")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Todo found", content =
                @Content(schema = @Schema(implementation = Todo.class))),
            @APIResponse(responseCode = "404", description = "Todo not found"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Response findById(@PathParam("id") int id) {
        Optional<Todo> result = this.todoService.findById(id);

        Response.ResponseBuilder builder;

        if (result.isPresent()) {
            builder = Response.ok(Entity.json(result.get()));
        } else {
            builder = Response.status(Response.Status.NOT_FOUND);
        }

        return builder.build();
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Update todo by id")
    @Tag(name = "Todo")
    @APIResponses({
            @APIResponse(responseCode = "204", description = "Todo updated"),
            @APIResponse(responseCode = "404", description = "Todo not found"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Response update(@PathParam("id") int id, TodoBase base) {
        Response.ResponseBuilder builder;

        if (this.todoService.update(id, base)) {
            builder = Response.noContent();
        } else {
            builder = Response.status(Response.Status.NOT_FOUND);
        }

        return builder.build();
    }

    @DELETE
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Delete todo by id")
    @Tag(name = "Todo")
    public Response delete(@PathParam("id") int id, TodoBase base) {
        Response.ResponseBuilder builder;

        if (this.todoService.delete(id)) {
            builder = Response.noContent();
        } else {
            builder = Response.status(Response.Status.NOT_FOUND);
        }

        return builder.build();
    }

    @POST
    @Path("{id}/task")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create new task to given todo")
    @Tag(name = "Todo")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Todo found", content =
                @Content(schema = @Schema(implementation = Task.class))),
            @APIResponse(responseCode = "404", description = "Task not found"),
            @APIResponse(responseCode = "500", description = "Server error")
    })
    public Response createTask(TaskBase taskBase, @PathParam("id") int id, @Context UriInfo uriInfo) {
        Response.ResponseBuilder builder;

        Optional<Todo> todo = this.todoService.findById(id);

        if (todo.isPresent()) {
            URI uri = uriInfo.getAbsolutePathBuilder()
                    .path(Integer.toString(todo.get().getId()))
                    .build();

            Optional<Task> task = this.taskService.create(taskBase);

            task.ifPresent(value -> value.setTodoId(todo.get().getId()));

            builder = Response.created(uri);
        } else {
            builder = Response.status(Response.Status.NOT_ACCEPTABLE);
        }

        return builder.build();
    }
}
