package org.eclipse.jdt.ls.core.websocket;

import javax.websocket.Session;

@SuppressWarnings("all")
public class MessageHandlerWithSession {
  private String id;

	private Session session;

  private LanguageMessageHandler messageHandler;

	public MessageHandlerWithSession(final LanguageMessageHandler languageHandler, final Session session2) {
    this.messageHandler = languageHandler;
    this.session = session2;
  }

  public String getId() {
    return this.id;
  }

  public String setId(final String id) {
    return this.id = id;
  }

	public Session getSession() {
    return this.session;
  }

	public Session setSession(final Session session) {
    return this.session = session;
  }

  public LanguageMessageHandler getMessageHandler() {
    return this.messageHandler;
  }

  public LanguageMessageHandler setMessageHandler(final LanguageMessageHandler messageHandler) {
    return this.messageHandler = messageHandler;
  }
}
