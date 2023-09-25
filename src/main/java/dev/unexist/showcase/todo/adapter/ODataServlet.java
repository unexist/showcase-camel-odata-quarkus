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
import java.util.ArrayList;

@WebServlet(name = "ODataServlet", urlPatterns = "/odata/*")
public class ODataServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ODataServlet.class);

    @Inject
    EdmProvider edmProvider;

    @Inject
    EntityCollectionProcessor entityCollectionProcessor;

    @Override
    protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        LOGGER.info("odata1");

        try {
            OData odata = OData.newInstance();
            ServiceMetadata edm = odata.createServiceMetadata(edmProvider,
                    new ArrayList<>());

            ODataHttpHandler handler = odata.createHandler(edm);

            handler.register(entityCollectionProcessor);
            handler.process(httpRequest, httpResponse);
        } catch (RuntimeException e) {
            LOGGER.error("Server Error occurred in servlet", e);
        }
    }
}
