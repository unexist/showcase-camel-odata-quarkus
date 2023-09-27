/**
 * @package Showcase-OData-Quarkus
 *
 * @file Todo OData resource
 * @copyright 2023-present Christoph Kappel <christoph@unexist.dev>
 * @version $Id$
 *
 * This program can be distributed under the terms of the Apache License v2.0.
 * See the file LICENSE for details.
 **/

package dev.unexist.showcase.todo.adapter;

import dev.unexist.showcase.todo.adapter.odata.TodoEdmProvider;
import dev.unexist.showcase.todo.adapter.odata.TodoEntityCollectionProcessor;
import dev.unexist.showcase.todo.adapter.odata.TodoEntityProcessor;
import dev.unexist.showcase.todo.adapter.odata.TodoEntityStorage;
import dev.unexist.showcase.todo.adapter.odata.TodoPrimitiveProcessor;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;

@WebServlet(name = "ODataServlet", urlPatterns = "/odata/*")
public class ODataServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ODataServlet.class);

    @Inject
    TodoEntityStorage storage;

    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) {
        HttpSession session = request.getSession(true);
        TodoEntityStorage storage = (TodoEntityStorage)session.getAttribute(TodoEntityStorage.class.getName());

        if (null == storage) {
            storage = this.storage;

            session.setAttribute(TodoEntityStorage.class.getName(), storage);
        }

        try {
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(new TodoEdmProvider(), new ArrayList<>());
            ODataHttpHandler handler = odata.createHandler(edm);

            handler.register(new TodoEntityCollectionProcessor(storage));
            handler.register(new TodoEntityProcessor(storage));
            handler.register(new TodoPrimitiveProcessor(storage));

            handler.process(request, response);
        } catch (RuntimeException e) {
            LOGGER.error("Server Error occurred in servlet", e);
        }
    }
}
