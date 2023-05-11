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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import javax.websocket.Session;

import org.eclipse.jdt.ls.core.internal.JavaClientConnection.JavaLanguageClient;
import org.eclipse.lsp4j.jsonrpc.Endpoint;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageProducer;
import org.eclipse.lsp4j.jsonrpc.MessageTracer;
import org.eclipse.lsp4j.jsonrpc.RemoteEndpoint;
import org.eclipse.lsp4j.jsonrpc.StandardLauncher;
import org.eclipse.lsp4j.jsonrpc.json.ConcurrentMessageProcessor;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethod;
import org.eclipse.lsp4j.jsonrpc.json.JsonRpcMethodProvider;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;
import org.eclipse.lsp4j.jsonrpc.services.ServiceEndpoints;
import org.eclipse.lsp4j.jsonrpc.validation.ReflectiveMessageValidator;

/**
 * @author rabih
 *
 */
public class WebSocketLauncherBuilder<T> {

	protected Collection<Object> localServices;

	protected Collection<Class<? extends T>> remoteInterfaces;

	protected ExecutorService executorService;

	protected Session session;

	protected ClassLoader classLoader;

	protected boolean validateMessages;

	protected Function<MessageConsumer, MessageConsumer> messageWrapper;

	protected MessageTracer messageTracer;

	public WebSocketLauncherBuilder<T> setSession(Session session) {
		this.session = session;
		return this;
	}

	public WebSocketLauncherBuilder<T> setLocalService(Object localService) {
		this.localServices = Collections.singletonList(localService);
		return this;
	}

	public WebSocketLauncherBuilder<T> setLocalServices(Collection<Object> localServices) {
		this.localServices = localServices;
		return this;
	}

	public WebSocketLauncherBuilder<T> setRemoteInterface(Class<? extends T> remoteInterface) {
		this.remoteInterfaces = Collections.singletonList(remoteInterface);
		return this;
	}

	public WebSocketLauncherBuilder<T> setRemoteInterfaces(Collection<Class<? extends T>> remoteInterfaces) {
		this.remoteInterfaces = remoteInterfaces;
		return this;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Launcher<JavaLanguageClient> create() {
		if (localServices == null) {
			throw new IllegalStateException("Local service must be configured.");
		}
		if (remoteInterfaces == null) {
			throw new IllegalStateException("Remote interface must be configured.");
		}

		MessageJsonHandler jsonHandler = createJsonHandler();
		RemoteEndpoint remoteEndpoint = createRemoteEndpoint(jsonHandler);
		addMessageHandlers(jsonHandler, remoteEndpoint);
		T remoteProxy = createProxy(remoteEndpoint);
		ExecutorService execService = executorService != null ? executorService : Executors.newCachedThreadPool();
		// Create the message processor
		StreamMessageProducer reader = new StreamMessageProducer(null, jsonHandler, remoteEndpoint);
		MessageConsumer messageConsumer = wrapMessageConsumer(remoteEndpoint);
		ConcurrentMessageProcessor msgProcessor = createMessageProcessor(reader, messageConsumer, remoteProxy);
		return new StandardLauncher(execService, remoteProxy, remoteEndpoint, msgProcessor);
	}

	/**
	 * Create the message processor that listens to the input stream.
	 */
	protected ConcurrentMessageProcessor createMessageProcessor(MessageProducer reader, MessageConsumer messageConsumer, T remoteProxy) {
		return new ConcurrentMessageProcessor(reader, messageConsumer);
	}

	protected RemoteEndpoint createRemoteEndpoint(MessageJsonHandler jsonHandler) {
		MessageConsumer outgoingMessageStream = new WebSocketMessageConsumer(session, jsonHandler);
		outgoingMessageStream = wrapMessageConsumer(outgoingMessageStream);
		Endpoint localEndpoint = ServiceEndpoints.toEndpoint(localServices);
		RemoteEndpoint remoteEndpoint;

		remoteEndpoint = new RemoteEndpoint(outgoingMessageStream, localEndpoint);
		jsonHandler.setMethodProvider(remoteEndpoint);
		return remoteEndpoint;
	}

	protected void addMessageHandlers(MessageJsonHandler jsonHandler, RemoteEndpoint remoteEndpoint) {
		MessageConsumer messageConsumer = wrapMessageConsumer(remoteEndpoint);
		session.addMessageHandler(new WebSocketMessageHandler(messageConsumer, jsonHandler, remoteEndpoint));
	}

	/**
	 * Gather all JSON-RPC methods from the local and remote services.
	 */
	protected Map<String, JsonRpcMethod> getSupportedMethods() {
		Map<String, JsonRpcMethod> supportedMethods = new LinkedHashMap<>();
		// Gather the supported methods of remote interfaces
		for (Class<?> interface_ : remoteInterfaces) {
			supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(interface_));
		}

		// Gather the supported methods of local services
		for (Object localService : localServices) {
			if (localService instanceof JsonRpcMethodProvider) {
				JsonRpcMethodProvider rpcMethodProvider = (JsonRpcMethodProvider) localService;
				supportedMethods.putAll(rpcMethodProvider.supportedMethods());
			} else {
				supportedMethods.putAll(ServiceEndpoints.getSupportedMethods(localService.getClass()));
			}
		}

		return supportedMethods;
	}

	/**
	 * Create the JSON handler for messages between the local and remote services.
	 */
	protected MessageJsonHandler createJsonHandler() {
		Map<String, JsonRpcMethod> supportedMethods = getSupportedMethods();

		return new MessageJsonHandler(supportedMethods);
	}

	@SuppressWarnings("unchecked")
	protected T createProxy(RemoteEndpoint remoteEndpoint) {
		if (localServices.size() == 1 && remoteInterfaces.size() == 1) {
			return ServiceEndpoints.toServiceObject(remoteEndpoint, remoteInterfaces.iterator().next());
		} else {
			return (T) ServiceEndpoints.toServiceObject(remoteEndpoint, (Collection<Class<?>>) (Object) remoteInterfaces, classLoader);
		}
	}

	public WebSocketLauncherBuilder<T> validateMessages(boolean validate) {
		this.validateMessages = validate;
		return this;
	}

	protected MessageConsumer wrapMessageConsumer(MessageConsumer consumer) {
		MessageConsumer result = consumer;
		if (messageTracer != null) {
			result = messageTracer.apply(consumer);
		}
		if (validateMessages) {
			result = new ReflectiveMessageValidator(result);
		}
		if (messageWrapper != null) {
			result = messageWrapper.apply(result);
		}
		return result;
	}

	public WebSocketLauncherBuilder<T> wrapMessages(Function<MessageConsumer, MessageConsumer> wrapper) {
		this.messageWrapper = wrapper;
		return this;
	}
}
