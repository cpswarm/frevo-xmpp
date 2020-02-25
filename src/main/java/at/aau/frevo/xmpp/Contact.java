/**
 * File: Contact.java
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jxmpp.jid.Jid;

/**
 * Encapsulates an XMPP contact.
 * <p>
 * Extend this class to implement contact-specific functionality.
 */
public abstract class Contact implements StanzaListener {

  private static final Logger LOGGER = LogManager.getLogger(Contact.class);

  private Jid jid;
  private Wrapper wrapper;
  private StanzaFilter incomingMessageFilter;
  private boolean available;

  /**
   * Creates a new {@code Contact} instance for the jid and attaches it the specified
   * {@code Wrapper}.
   * 
   * @param wrapper the assosciated {@code Wrapper}
   * @param jid     the jid of the contact
   */
  public Contact(Wrapper wrapper, Jid jid) {
    this.wrapper = wrapper;
    this.jid = jid;
    incomingMessageFilter =
        new AndFilter(MessageTypeFilter.NORMAL_OR_CHAT, FromMatchesFilter.create(jid));
    wrapper.getConnection().addSyncStanzaListener(this, incomingMessageFilter);
  }

  @Override
  public void processStanza(Stanza stanza)
      throws NotConnectedException, InterruptedException, NotLoggedInException {
    if (stanza instanceof Message) {
      final Message message = (Message) stanza;
      var body = message.getBody();
      if (body != null) {
        handleMessage(body);
      }
    }
  }

  /**
   * Sends a message to the contact.
   * 
   * @param message the message to send
   * @return {@code true} if the message was sent successfully
   */
  public boolean sendMessage(String message) {
    Message m = new Message(jid, message);
    LOGGER.trace("Sending message to {}: {}", jid, message);
    return wrapper.sendStanza(m);
  }

  /**
   * Called to handle an incoming status from the contact, usually due to a presence change.
   * <p>
   * Derived classes should implement this.
   * 
   * @param statusString the status
   */
  protected abstract void handleStatus(String statusString);

  /**
   * Called to handle an incoming message from the contact.
   * <p>
   * Derived classes should implement this.
   * 
   * @param message the message
   */
  protected abstract void handleMessage(String message);

  /**
   * Gets the {@code Wrapper} associated with this contact.
   * 
   * @return the wrapper
   */
  public Wrapper getWrapper() {
    return wrapper;
  }

  /**
   * Gets the jid of the contact.
   * 
   * @return the jid
   */
  public Jid getJid() {
    return jid;
  }

  /**
   * Sets the availabilty flag of the contact.
   * 
   * @param available the availabilty flag
   */
  public synchronized void setAvailable(boolean available) {
    this.available = available;
  }

  /**
   * Gets the availabilty flag of the contact.
   * 
   * @return the available
   */
  public synchronized boolean isAvailable() {
    return available;
  }
}
