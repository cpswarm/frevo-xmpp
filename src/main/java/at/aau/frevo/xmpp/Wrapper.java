/**
 * File: Wrapper.java
 * 
 * Copyright (C) 2020 FREVO XMPP project contributors
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.Roster.SubscriptionMode;
import org.jivesoftware.smack.roster.RosterListener;
import org.jivesoftware.smack.roster.SubscribeListener;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smackx.ping.PingManager;
import org.jxmpp.jid.Jid;

/**
 * Wrapper for easily maintaining an XMPP connection.
 * <p>
 * Extend this class to implement wrapper-specific functionality, typically creating specific types
 * of contacts.
 */
public abstract class Wrapper {
  private static final Logger LOGGER = LogManager.getLogger(Wrapper.class);

  private static final int SEND_RETRY_COUNT = 10;

  private Map<String, Contact> contacts = Collections.synchronizedMap(new HashMap<>());
  private XMPPTCPConnection connection;

  /**
   * Creates a new {@code Wrapper} instance using the specified connection configuration.
   * 
   * @param connectionConfiguration the connection configuration
   */
  public Wrapper(XMPPTCPConnectionConfiguration connectionConfiguration) {
    try {
      // connect
      connection = new XMPPTCPConnection(connectionConfiguration);
      connection.connect();

      var roster = Roster.getInstanceFor(connection);

      // listen for subscriptions and automatically approve all
      roster.addSubscribeListener(new SubscribeListener() {
        @Override
        public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
          LOGGER.trace("processing subscribe", from);
          return SubscribeAnswer.ApproveAndAlsoRequestIfRequired;
        }
      });
      roster.setSubscriptionMode(SubscriptionMode.manual);

      // listen for presence changes
      roster.addRosterListener(new RosterListener() {

        @Override
        public void presenceChanged(Presence presence) {
          var key = presence.getFrom().asBareJid().toString();
          Contact contact = contacts.get(key);

          // try to create a new contact if none exists
          if (contact == null) {
            contact =
                createContact(presence.getFrom().asEntityBareJidIfPossible(), presence.getStatus());
            if (contact != null) {
              LOGGER.trace("added new contact: " + key);
              contacts.put(key, contact);
            }
          }

          // update contact
          if (contact != null) {
            contact.setAvailable(presence.getType() == Type.available);
            contact.handleStatus(presence.getStatus());
          }
        }

        @Override
        public void entriesUpdated(Collection<Jid> addresses) {
        }

        @Override
        public void entriesDeleted(Collection<Jid> addresses) {
        }

        @Override
        public void entriesAdded(Collection<Jid> addresses) {
        }
      });

      connection.login();
    } catch (XMPPException | SmackException | IOException | InterruptedException e) {
      LOGGER.error("Exception during wrapper creation", e);
    }
  }

  /**
   * Gets the connection.
   * 
   * @return the connection
   */
  public XMPPTCPConnection getConnection() {
    return connection;
  }

  /**
   * Creates a contact to be cached by the wrapper.
   * <p>
   * Subclasses should implement this to return a specific sort of contact.
   * 
   * @param jid    the jid
   * @param status the initial status
   * @return the new contact
   */
  protected abstract Contact createContact(Jid jid, String status);

  /**
   * Maintains the connection by pinging the server.
   * <p>
   * This method does not return.
   */
  public void maintainConnection() {
    var pingManager = PingManager.getInstanceFor(connection);
    while (true) {
      try {

        // try to ping forever until and exception occurs
        while (true) {
          pingManager.pingMyServer();
          Thread.sleep(5000);
        }
      } catch (InterruptedException | NotConnectedException e) {
        LOGGER.warn("Ping failed. Reconnecting...");
        try {
          // cycle the connection
          connection.disconnect();
          connection.connect().login();
          LOGGER.info("Reconnected");
        } catch (XMPPException | SmackException | IOException | InterruptedException e1) {
          LOGGER.warn("Reconnection failed. Retrying...");
        }
      }
    }
  }

  /**
   * Sets the status of the wrapper by sending a presence stanza.
   * 
   * @param status the status
   */
  public void setStatus(String status) {
    sendStanza(new Presence(Type.available, status, 127, Mode.available));
  }

  /**
   * Sends a stanza using the wrapper's connection.
   * 
   * @param stanza the stanza
   * @return {@code true} if sending was succeeded
   */
  public synchronized boolean sendStanza(Stanza stanza) {
    // try to send until retries are exhausted
    for (var i = 0; i < SEND_RETRY_COUNT; i++) {
      try {
        connection.sendStanza(stanza);
        return true;
      } catch (NotConnectedException | InterruptedException e) {
        LOGGER.warn("Couldnt send stanza", e);
      }

      // take a break before trying again
      try {
        Thread.sleep(500 * i);
      } catch (InterruptedException e) {
        LOGGER.trace("An exception occured while sleeping", e);
      }
    }
    LOGGER.error("Failed to send stanza: " + stanza);
    return false;
  }
}
