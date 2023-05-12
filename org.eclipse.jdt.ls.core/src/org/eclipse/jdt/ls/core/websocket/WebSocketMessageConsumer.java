package org.eclipse.jdt.ls.core.websocket;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;

import org.eclipse.jetty.websocket.jsr356.JsrSession;
import org.eclipse.lsp4j.jsonrpc.json.MessageJsonHandler;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageConsumer;
import org.eclipse.lsp4j.jsonrpc.messages.Message;
import org.eclipse.xtext.xbase.lib.Exceptions;


/**
 * LSP4J message consumer that forwards messages to a web socket.
 */
@SuppressWarnings("all")
public class WebSocketMessageConsumer extends StreamMessageConsumer {
  protected RemoteEndpoint.Async remote;

	private Session webSocketSession;

  public WebSocketMessageConsumer(final RemoteEndpoint.Async remote, final MessageJsonHandler jsonHandler) {
    super(new ByteArrayOutputStream(), jsonHandler);
    this.remote = remote;
  }

  public WebSocketMessageConsumer(final RemoteEndpoint.Async remote, final String encoding, final MessageJsonHandler jsonHandler) {
    super(new ByteArrayOutputStream(), encoding, jsonHandler);
    this.remote = remote;
  }

	public WebSocketMessageConsumer(final Session webSocketSession, final MessageJsonHandler jsonHandler) {
    super(new ByteArrayOutputStream(), jsonHandler);
    this.webSocketSession = webSocketSession;
  }

  @Override
  public void consume(final Message message) {
		super.consume(message);
    try {
      String content = message.toString();
		//  TextMessage _textMessage = new TextMessage(content);
		((JsrSession) this.webSocketSession).getBasicRemote().sendText(content);
    } catch (final Throwable _t) {
      if (_t instanceof IOException) {
        final IOException e = (IOException)_t;
        e.printStackTrace();
      } else {
        throw Exceptions.sneakyThrow(_t);
      }
    }
  }
}
