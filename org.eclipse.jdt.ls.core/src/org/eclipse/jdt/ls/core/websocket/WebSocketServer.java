/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.websocket;

import org.eclipse.jdt.ls.core.internal.managers.ContentProviderManager;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.websocket.jsr356.server.deploy.WebSocketServerContainerInitializer;

public class WebSocketServer {

	private String host = null;

	private int port;

	private String contextPath = null;

	private String servletPath = null;

	private ContentProviderManager contentProviderManager = null;
	private ProjectsManager projectsManager = null;
	private PreferenceManager preferenceManager = null;

	public WebSocketServer(String host, int port, String contextPath, String servletPath, ContentProviderManager contentProviderManager, ProjectsManager projectsManager, PreferenceManager preferenceManager) {
		this.host = host;
		this.port = port;
		this.contextPath = contextPath;
		this.servletPath = servletPath;
		this.contentProviderManager = contentProviderManager;
		this.projectsManager = projectsManager;
		this.preferenceManager = preferenceManager;

	}

	public void createAndStartServer() {

		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(port);
		connector.setHost(host);
		server.addConnector(connector);

		// Setup the basic application "context" for this application at "/"
		// This is also known as the handler tree (in jetty speak)
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath(contextPath);
		server.setHandler(context);



			try {
				// Initialize javax.websocket layer
				WebSocketServerContainerInitializer.configure(context, (servletContext, wsContainer) -> {
					// This lambda will be called at the appropriate place in the
					// ServletContext initialization phase where you can initialize
					// and configure  your websocket container.

					// Configure defaults for container
					wsContainer.setDefaultMaxTextMessageBufferSize(65535);

					// Add WebSocket endpoint to javax.websocket layer
					wsContainer.addEndpoint(SocketHandler.class);
				});



			server.start();
			server.join();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}

	}

	public static void main(String[] args) {
		Server server = new Server();
		ServerConnector connector = new ServerConnector(server);
		connector.setPort(8080);

		server.addConnector(connector);

		// Setup the basic application "context" for this application at "/"
		// This is also known as the handler tree (in jetty speak)
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		server.setHandler(context);

		try {
			// Initialize javax.websocket layer
			WebSocketServerContainerInitializer.configure(context, (servletContext, wsContainer) -> {
				// This lambda will be called at the appropriate place in the
				// ServletContext initialization phase where you can initialize
				// and configure  your websocket container.

				// Configure defaults for container
				wsContainer.setDefaultMaxTextMessageBufferSize(1024 * 1024);
				wsContainer.setDefaultMaxBinaryMessageBufferSize(1024 * 1024);

				// Add WebSocket endpoint to javax.websocket layer
				wsContainer.addEndpoint(SocketHandler.class);
			});

			server.start();
			server.join();
		} catch (Throwable t) {
			t.printStackTrace(System.err);
		}
	}
}

