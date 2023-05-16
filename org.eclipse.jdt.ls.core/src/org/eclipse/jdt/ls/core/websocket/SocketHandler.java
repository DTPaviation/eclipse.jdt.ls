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

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.CountDownLatch;

import javax.websocket.CloseReason;
import javax.websocket.MessageHandler;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

import org.eclipse.jdt.ls.core.internal.JavaClientConnection.JavaLanguageClient;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.handlers.JDTLanguageServer;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageProducer;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.json.ConcurrentMessageProcessor;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;

import com.google.gson.GsonBuilder;

@ServerEndpoint("/myHandler/websocket")
public class SocketHandler {



	private CountDownLatch closureLatch = new CountDownLatch(1);


	@OnOpen
	public void onWebSocketConnect(Session session) {

		JDTLanguageServer protocol = new JDTLanguageServer(JavaLanguageServerPlugin.getProjectsManager(), JavaLanguageServerPlugin.getPreferencesManager());

		LinkedHashMap<String, JsonRpcMethod> supportedMethods = new LinkedHashMap<>();
		supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(JavaLanguageClient.class));
		/*if ((protocol instanceof JsonRpcMethodProvider)) {
		  supportedMethods.putAll(protocol.supportedMethods());
		}*/
		supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(protocol.getClass()));
		MessageJsonHandler jsonHandler = new MessageJsonHandler(supportedMethods) {
			@Override
			public GsonBuilder getDefaultGsonBuilder() {
				return super.getDefaultGsonBuilder();
			}
		};
		WebSocketMessageConsumer outgoingMessageStream = new WebSocketMessageConsumer(session, jsonHandler);
		Endpoint _endpoint = ServiceEndpoints.toEndpoint(protocol);
		RemoteEndpoint serverEndpoint = new RemoteEndpoint(outgoingMessageStream, _endpoint);
		jsonHandler.setMethodProvider(serverEndpoint);
		StreamMessageProducer incomingMessageStream = new StreamMessageProducer(null, jsonHandler);
		JavaLanguageClient remoteProxy = ServiceEndpoints.<JavaLanguageClient>toServiceObject(serverEndpoint, JavaLanguageClient.class);

			protocol.connectClient(remoteProxy);



		//ExecutorService executorService = Executors.newCachedThreadPool();
		LanguageMessageHandler languageHandler = new LanguageMessageHandler(incomingMessageStream, serverEndpoint, protocol);

		session.addMessageHandler(languageHandler);

		//ConcurrentMessageProcessor msgProcessor = createMessageProcessor(incomingMessageStream, outgoingMessageStream, remoteProxy);

		//msgProcessor.beginProcessing(executorService);
		//  MessageHandlerWithSession messageHandler = new MessageHandlerWithSession(languageHandler, session);

	}

	/**
	 * Create the message processor that listens to the input stream.
	 */
	protected ConcurrentMessageProcessor createMessageProcessor(MessageProducer reader, MessageConsumer messageConsumer, JavaLanguageClient remoteProxy) {
		return new ConcurrentMessageProcessor(reader, messageConsumer);
	}

	@OnMessage
	public void OnMessage(Session session, String message) {

		for (MessageHandler handler : session.getMessageHandlers()) {

			((LanguageMessageHandler) handler).onMessage(message, true);
		}

	}

	@OnClose
	public void onWebSocketClose(CloseReason reason, Session session) throws IOException {
		System.out.println("Socket Closed: " + reason);

		for (MessageHandler handler : session.getMessageHandlers()) {

			LanguageMessageHandler languageMsgHandler = ((LanguageMessageHandler) handler);
			languageMsgHandler.getProtocol().disconnectClient();
		}
		session.close();
		closureLatch.countDown();
	}



	@OnError
	public void onWebSocketError(Throwable cause) {
		cause.printStackTrace(System.err);
	}

	public void awaitClosure() throws InterruptedException {
		System.out.println("Awaiting closure from remote");
		closureLatch.await();
	}

}

