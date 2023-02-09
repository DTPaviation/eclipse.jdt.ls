package org.eclipse.jdt.ls.core.websocket;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.websocket.MessageHandler;

import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.lsp4j.jsonrpc.json.StreamMessageProducer;

@SuppressWarnings("all")
public class LanguageMessageHandler implements MessageHandler.Partial<String> {
  protected static class PartialMessageInputStream extends FilterInputStream {
    private List<byte[]> messages;

    private int currentMessageIndex = 0;

    public PartialMessageInputStream(final List<byte[]> messages) {
      super(new ByteArrayInputStream(messages.get(0)));
      this.messages = messages;
    }

    protected boolean nextMessage() {
      this.currentMessageIndex++;
      int _size = this.messages.size();
      boolean _lessThan = (this.currentMessageIndex < _size);
      if (_lessThan) {
        byte[] _get = this.messages.get(this.currentMessageIndex);
        ByteArrayInputStream _byteArrayInputStream = new ByteArrayInputStream(_get);
        this.in = _byteArrayInputStream;
        return true;
      } else {
        return false;
      }
    }

    @Override
    public int available() throws IOException {
      int current = super.available();
      if (((current <= 0) && this.nextMessage())) {
        return super.available();
      } else {
        return current;
      }
    }

    @Override
    public int read() throws IOException {
      int current = super.read();
      if (((current < 0) && this.nextMessage())) {
        return super.read();
      } else {
        return current;
      }
    }

    @Override
    public int read(final byte[] b) throws IOException {
      int current = super.read(b);
      if (((current <= 0) && this.nextMessage())) {
        return super.read(b);
      } else {
        return current;
      }
    }

    @Override
    public int read(final byte[] b, final int off, final int len) throws IOException {
      int current = super.read(b, off, len);
      if (((current <= 0) && this.nextMessage())) {
        return super.read(b, off, len);
      } else {
        return current;
      }
    }

    @Override
    public boolean markSupported() {
      return false;
    }
  }

  private StreamMessageProducer messageProducer;

  private MessageConsumer serverEndpoint;

  private List<byte[]> messages = new ArrayList<>();

  public LanguageMessageHandler(final StreamMessageProducer messageProducer, final MessageConsumer serverEndpoint) {
    this.messageProducer = messageProducer;
    this.serverEndpoint = serverEndpoint;
  }

  @Override
  public void onMessage(final String partialMessage, final boolean last) {
    int _length = partialMessage.length();
    boolean _greaterThan = (_length > 0);
	if (_greaterThan) {
	   int _length_1 = partialMessage.length();
	   String _plus = ("Content-Length: " + Integer.valueOf(_length_1));
	   String contentLengh = (_plus + "\r\n\r\n");
		this.messages.add(contentLengh.getBytes(Charset.forName("UTF-8")));
	   this.messages.add(partialMessage.getBytes(Charset.forName("UTF-8")));
	}
    if ((last && (!this.messages.isEmpty()))) {
      LanguageMessageHandler.PartialMessageInputStream _partialMessageInputStream = new LanguageMessageHandler.PartialMessageInputStream(this.messages);
      this.messageProducer.setInput(_partialMessageInputStream);
      this.messageProducer.listen(this.serverEndpoint);
      this.messages.clear();
    }
  }
}
