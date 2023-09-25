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

import javax.inject.Inject;

public class ODataServlet extends HttpServlet {
    private static final Logger LOGGER = LoggerFactory.getLogger(ODataResource.class);

    @Inject
    EdmProvider edmProvider;

    @Inject
    EntityCollectionProcessor entityCollectionProcessor;

    @Override
    protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
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
