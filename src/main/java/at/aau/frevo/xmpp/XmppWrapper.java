/**
 * File: XmppWrapper.java
 * 
 * Copyright (C) 2019 FREVO XMPP project contributors
 *
 * Universitaet Klagenfurt licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package at.aau.frevo.xmpp;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.net.ssl.SSLContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.chat2.IncomingChatMessageListener;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.roster.PresenceEventListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.Roster.SubscriptionMode;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Simple wrapper class for an XMPP connection.
 */
public class XmppWrapper implements IncomingChatMessageListener, ConnectionListener,
    PresenceEventListener, ReconnectionListener {

  private static final Logger LOGGER = LogManager.getLogger(XmppWrapper.class);

  /**
   * Interface for receiving incoming messages.
   */
  public interface MessageListener {

    /**
     * Notifies that a new incoming message has arrived.
     * 
     * @param sourceJid the source jid
     * @param message   the message
     */
    void newIncomingMessage(String sourceJid, String message);
  }

  protected static final int SEND_RETRY_COUNT = 10;
  protected XMPPTCPConnection connection;
  protected ChatManager chatManager;
  protected List<MessageListener> listeners = new CopyOnWriteArrayList<MessageListener>();

  /**
   * Creates a new {@code XmppWrapper} instance by creating a connection with the specified
   * parameters.
   * 
   * @param host   the host name of the XMPP server
   * @param port   the port of the XMPP server
   * @param domain the domain of the XMPP server
   * @param debug  flag to optionally enable debugging
   */
  public XmppWrapper(String host, int port, String domain, boolean debug) {
    try {
      var sslContext = SSLContext.getInstance("TLS");
      sslContext.init(null, null, new SecureRandom());

      var connectionConfig =
          XMPPTCPConnectionConfiguration.builder().setHost(host).setPort(port).setXmppDomain(domain)
              .setCompressionEnabled(false).setCustomSSLContext(sslContext).build();

      connection = new XMPPTCPConnection(connectionConfig);
      connection.setUseStreamManagement(true);
      connection.setUseStreamManagementResumption(true);

      var reconnectionManager = ReconnectionManager.getInstanceFor(connection);
      reconnectionManager.enableAutomaticReconnection();
      reconnectionManager.addReconnectionListener(this);

      var roster = Roster.getInstanceFor(connection);
      roster.setSubscriptionMode(SubscriptionMode.accept_all);
      roster.addPresenceEventListener(this);

      chatManager = ChatManager.getInstanceFor(connection);
      chatManager.addIncomingListener(this);

      connection.addConnectionListener(this);
    } catch (Exception e) {
      LOGGER.error("Exception while constructing", e);
    }
  }

  /**
   * Adds a listener to be notified about incoming messages.
   * 
   * @param listener the {@code MessageListener}
   */
  public void addListener(MessageListener listener) {
    listeners.add(listener);
  }

  /**
   * Removes a listener.
   * 
   * @param listener the {@code MessageListener}
   */
  public void removeListener(MessageListener listener) {
    listeners.remove(listener);
  }

  /**
   * Connects, logs in and begins executing a loop.
   * <p>
   * Control does not return from this method.
   * 
   * @param clientId       client id of connection
   * @param clientPassword client password of the connection
   * @param resource       the resource to log in to
   */
  public void execute(String clientId, String clientPassword, String resource) {
    while (true) {
      try {
        Thread.sleep(1000);
        connection.connect();
        LOGGER.info("Connected");
        connection.login(clientId, clientPassword, Resourcepart.from(resource));
        LOGGER.info("Logged In");
        Thread.sleep(1000);
        while (connection.isConnected()) {
          Thread.sleep(1000);
        }

      } catch (SmackException | IOException | XMPPException | InterruptedException e) {
        LOGGER.error("Exception while executing", e);
      }
    }
  }

  /**
   * Sends a message using the XMPP connection.
   * 
   * @param targetJid the recipient
   * @param message   the message
   * @return {@code true} if the message was send, otherwise {@code false}
   */
  public synchronized boolean sendMessage(String targetJid, String message) {
    try {
      var chat = chatManager.chatWith(JidCreate.entityBareFrom(targetJid));

      // try to send until retries are exhausted
      for (var i = 0; i < SEND_RETRY_COUNT; i++) {
        try {
          chat.send(message);
          LOGGER.trace("Sent message: " + targetJid + ": " + message);
          return true;
        } catch (NotConnectedException | InterruptedException e) {
          LOGGER.warn("Couldnt send message", e);
        }

        try {
          Thread.sleep(50);
        } catch (InterruptedException e) {
          LOGGER.trace("An exception occured while sleeping", e);
        }
      }
      LOGGER.error("Failed to send message: " + targetJid + ": " + message);
      return false;

    } catch (XmppStringprepException e) {
      LOGGER.error("Failed to get chat for jid", e);
      return false;
    }
  }

  @Override
  public synchronized void newIncomingMessage(EntityBareJid from,
      org.jivesoftware.smack.packet.Message message, Chat chat) {
    var incomingMessage = message.getBody();
    var sourceJid = from.toString();
    LOGGER.trace("Received message: " + sourceJid + ": " + incomingMessage);
    for (var listener : listeners) {
      listener.newIncomingMessage(sourceJid, incomingMessage);
    }
  }

  @Override
  public void connectionClosedOnError(Exception e) {
    LOGGER.trace("Connection closed on error", e);
  }

  @Override
  public void connectionClosed() {
    LOGGER.trace("Connection closed");
  }

  @Override
  public void connected(XMPPConnection connection) {
    LOGGER.trace("Connected");

  }

  @Override
  public void authenticated(XMPPConnection connection, boolean resumed) {
    LOGGER.trace("Authenticated");
    try {
      connection.sendStanza(new Presence(Type.available, "Ready", 127, Mode.available));
    } catch (NotConnectedException | InterruptedException e) {
      LOGGER.error("An exception occured while sending presence", e);
    }
  }

  @Override
  public void presenceUnsubscribed(BareJid address, Presence unsubscribedPresence) {
    LOGGER.trace("Presence unsubscribed", new Object[] {address, unsubscribedPresence});
  }

  @Override
  public void presenceUnavailable(FullJid address, Presence presence) {
    LOGGER.trace("Presence unavailable", new Object[] {address, presence});
  }

  @Override
  public void presenceSubscribed(BareJid address, Presence subscribedPresence) {
    LOGGER.trace("Presence subscribed", new Object[] {address, subscribedPresence});
  }

  @Override
  public void presenceError(Jid address, Presence errorPresence) {
    LOGGER.trace("Presence error", new Object[] {address, errorPresence});
  }

  @Override
  public void presenceAvailable(FullJid address, Presence availablePresence) {
    LOGGER.trace("Presence available", new Object[] {address, availablePresence});
  }

  @Override
  public void reconnectingIn(int seconds) {
    LOGGER.trace("Reconnecting in", new Object[] {seconds});
  }

  @Override
  public void reconnectionFailed(Exception e) {
    LOGGER.trace("Reconnection failed", new Object[] {e});
  }
}
