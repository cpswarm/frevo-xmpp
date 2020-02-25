/**
 * File: JsonSerializer.java
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

import com.google.gson.JsonParseException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.cpswarm.optimization.messages.Message;
import eu.cpswarm.optimization.messages.MessageSerializer;
import eu.cpswarm.optimization.statuses.BaseStatus;
import eu.cpswarm.optimization.statuses.StatusSerializer;

/**
 * Wrapper for JSON serialization.
 */
public class JsonSerializer {

  private static final Logger LOGGER = LogManager.getLogger(JsonSerializer.class);

  private static StatusSerializer statusSerializer = new StatusSerializer();
  private static MessageSerializer messageSerializer = new MessageSerializer();

  /**
   * Converts a JSON string to an instance derived from {@code BaseStatus}.
   * 
   * @param <T>        the type
   * @param jsonStatus the status as a JSON string
   * @return the instance or {@code null} if the conversion failed
   */
  public static <T extends BaseStatus> T toStatus(String jsonStatus) {
    try {
      return statusSerializer.fromJson(jsonStatus);
    } catch (JsonParseException e) {
      LOGGER.error("Exception:", e);
      return null;
    }
  }

  /**
   * Converts an instance derived from {@code BaseStatus} to a JSON string.
   * 
   * @param status the status to convert
   * @return the JSON string
   */
  public static String toJson(BaseStatus status) {
    return statusSerializer.toJson(status);
  }

  /**
   * Converts a JSON string to an instance derived from {@code Message}.
   * 
   * @param <T>         the type
   * @param jsonMessage the message as a JSON string
   * @return the instance or {@code null} if the conversion failed
   */
  public static <T extends Message> T toMessage(String jsonMessage) {
    try {
      return messageSerializer.fromJson(jsonMessage);
    } catch (JsonParseException e) {
      LOGGER.error("Exception:", e);
      return null;
    }
  }

  /**
   * Converts an instance derived from {@code Message} to a JSON string.
   * 
   * @param message the message to convert
   * @return the JSON string
   */
  public static String toJson(Message message) {
    return messageSerializer.toJson(message);
  }
}
