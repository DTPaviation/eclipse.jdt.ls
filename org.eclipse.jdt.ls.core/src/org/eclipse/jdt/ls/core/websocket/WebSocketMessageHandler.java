package org.eclipse.jdt.ls.core.websocket;

import javax.websocket.MessageHandler;

import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.MessageIssueException;
import org.eclipse.lsp4j.jsonrpc.MessageIssueHandler;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.messages.Message;

/**
 * WebSocket message handler that parses JSON messages and forwards them to a {@link MessageConsumer}.
 */
public class WebSocketMessageHandler implements MessageHandler.Whole<String> {

	private final MessageConsumer callback;
	private final MessageJsonHandler jsonHandler;
	private final MessageIssueHandler issueHandler;

	public WebSocketMessageHandler(MessageConsumer callback, MessageJsonHandler jsonHandler, MessageIssueHandler issueHandler) {
		this.callback = callback;
		this.jsonHandler = jsonHandler;
		this.issueHandler = issueHandler;
	}

	@Override
	public void onMessage(String content) {
		try {
			Message message = jsonHandler.parseMessage(content);
			callback.consume(message);
		} catch (MessageIssueException exception) {
			// An issue was found while parsing or validating the message
			issueHandler.handle(exception.getRpcMessage(), exception.getIssues());
		}
	}

}