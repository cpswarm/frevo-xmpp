/**
 * File: SooContact.java
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
import org.jxmpp.jid.Jid;

import eu.cpswarm.optimization.messages.CancelOptimizationMessage;
import eu.cpswarm.optimization.messages.GetOptimizationStatusMessage;
import eu.cpswarm.optimization.messages.StartOptimizationMessage;

/**
 * Simulation and optimization orchestrator contact.
 */
public class SooContact extends Contact {

  private static final Logger LOGGER = LogManager.getLogger(SooContact.class);

  private OptimizationManager optimizationManager;

  /**
   * Creates a new {@code SooContact} instance with the specified wrapper and jid.
   * 
   * @param wrapper the wrapper
   * @param jid     the jid
   */
  public SooContact(FrevoWrapper wrapper, Jid jid) {
    super(wrapper, jid);
    optimizationManager = wrapper.getOptimizationManager();
    LOGGER.debug("Created SooContact: {}", jid.toString());
  }

  @Override
  protected void handleStatus(String statusString) {
    // do nothing
    LOGGER.trace("parseStatus: {}", statusString);
  }

  @Override
  protected void handleMessage(String messageString) {
    var message = JsonSerializer.toMessage(messageString);
    if (message instanceof StartOptimizationMessage) {
      handleStartOptimization((StartOptimizationMessage) message);
    } else if (message instanceof CancelOptimizationMessage) {
      handleCancelOptimization((CancelOptimizationMessage) message);
    } else if (message instanceof GetOptimizationStatusMessage) {
      handleGetOptimizationStatus((GetOptimizationStatusMessage) message);
    } else {
      LOGGER.warn("Unable to process message: {}", messageString);
    }
  }

  /**
   * Handles a start optimization message.
   * 
   * @param message the message
   */
  private void handleStartOptimization(StartOptimizationMessage message) {
    LOGGER.trace("startOptimization: {}", JsonSerializer.toJson(message));
    optimizationManager.createOptimizationTask(message.getOptimizationId(),
        message.getSimulationConfigurationId(), message.getConfiguration());
  }

  /**
   * Handles a cancel optimization message.
   * 
   * @param message the message
   */
  private void handleCancelOptimization(CancelOptimizationMessage message) {
    LOGGER.trace("cancelOptimization: {}", JsonSerializer.toJson(message));
    optimizationManager.cancelOptimizationTask(message.getOptimizationId());
  }

  /**
   * Handles a get optimization status message.
   * 
   * @param message
   */
  private void handleGetOptimizationStatus(GetOptimizationStatusMessage message) {
    LOGGER.trace("getOptimizationStatus: {}", JsonSerializer.toJson(message));
    var task = optimizationManager.getTask(message.getOptimizationId());
    if (task != null) {
      sendMessage(JsonSerializer.toJson(task.getSnapshot()));
    }
  }
}
