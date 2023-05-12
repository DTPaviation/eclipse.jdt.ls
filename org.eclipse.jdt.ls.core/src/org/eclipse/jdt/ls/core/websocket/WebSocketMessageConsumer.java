package org.eclipse.jdt.ls.core.websocket;

import java.io.IOException;
import java.util.logging.Logger;

import javax.websocket.Session;

import org.eclipse.lsp4j.jsonrpc.JsonRpcException;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Message;

/**
 * Message consumer that sends messages via a WebSocket session.
 */
public class WebSocketMessageConsumer implements MessageConsumer {

	private static final Logger LOG = Logger.getLogger(WebSocketMessageConsumer.class.getName());

	private final Session session;
	private final MessageJsonHandler jsonHandler;

	public WebSocketMessageConsumer(Session session, MessageJsonHandler jsonHandler) {
		this.session = session;
		this.jsonHandler = jsonHandler;
	}

	public Session getSession() {
		return session;
	}

	@Override
	public void consume(Message message) {
		String content = jsonHandler.serialize(message);
		try {
			sendMessage(content);
		} catch (IOException exception) {
			throw new JsonRpcException(exception);
		}
	}

	protected void sendMessage(String message) throws IOException {
		if (session.isOpen()) {
			int length = message.length();
			if (length <= session.getMaxTextMessageBufferSize()) {
				session.getAsyncRemote().sendText(message);
			} else {
				int currentOffset = 0;
				while (currentOffset < length) {
					int currentEnd = Math.min(currentOffset + session.getMaxTextMessageBufferSize(), length);
					session.getBasicRemote().sendText(message.substring(currentOffset, currentEnd), currentEnd == length);
					currentOffset = currentEnd;
				}
			}
		} else {
			LOG.info("Ignoring message due to closed session: " + message);
		}
	}

}
