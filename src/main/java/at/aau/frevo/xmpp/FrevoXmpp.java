/**
 * File: FrevoXmpp.java
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.aau.frevo.xmpp.XmppWrapper.MessageListener;
import eu.cpswarm.optimization.messages.CancelOptimizationMessage;
import eu.cpswarm.optimization.messages.GetProgressMessage;
import eu.cpswarm.optimization.messages.Message;
import eu.cpswarm.optimization.messages.MessageSerializer;
import eu.cpswarm.optimization.messages.OptimizationCancelledMessage;
import eu.cpswarm.optimization.messages.OptimizationProgressMessage;
import eu.cpswarm.optimization.messages.OptimizationStartedMessage;
import eu.cpswarm.optimization.messages.ReplyMessage.Status;
import eu.cpswarm.optimization.messages.SimulationResultMessage;
import eu.cpswarm.optimization.messages.StartOptimizationMessage;

/**
 * Main class for FREVO XMPP module for the CPSwarm project.
 * <p>
 * Sets up the XMPP connection and forwards messages to {@link OptimizationTask} and
 * {@link SimulationManager}.
 */
public class FrevoXmpp implements MessageListener {

  private static final Logger LOGGER = LogManager.getLogger(FrevoXmpp.class);

  protected MessageSerializer messageSerializer = new MessageSerializer();
  protected ExecutorService executorService = Executors.newWorkStealingPool();
  protected XmppWrapper xmppWrapper;

  protected Map<String, OptimizationTask> optimizationTasks =
      Collections.synchronizedMap(new HashMap<>());
  protected FrevoXmppConfiguration configuration;

  /**
   * Creates a new {@code FrevoXmpp} instance using the supplied configuration.
   * 
   * @param configuration the configuration
   */
  public FrevoXmpp(FrevoXmppConfiguration configuration) {
    this.configuration = configuration;

    LOGGER.info("Configuration: " + configuration);

    xmppWrapper = new XmppWrapper(configuration.getHost(), configuration.getPort(),
        configuration.getDomain(), configuration.isDebugging());
    xmppWrapper.addListener(this);

    // last line, control doesn't return here, instead new incoming message events will be received
    xmppWrapper.execute(configuration.getClientId(), configuration.getClientPassword(),
        configuration.getResource());
  }

  @Override
  public void newIncomingMessage(String sourceJid, String message) {
    var m = messageSerializer.fromJson(message);
    if (m instanceof StartOptimizationMessage) {
      handleStartOptimizationMessage(sourceJid, (StartOptimizationMessage) m);
    } else if (m instanceof CancelOptimizationMessage) {
      handleCancelOptimizationMessage(sourceJid, (CancelOptimizationMessage) m);
    } else if (m instanceof GetProgressMessage) {
      handleGetProgressMessage(sourceJid, (GetProgressMessage) m);
    } else if (m instanceof SimulationResultMessage) {
      handleSimulationResultMessage(sourceJid, (SimulationResultMessage) m);
    }
  }

  /**
   * Sends a message via the XMPP connection.
   * 
   * @param targetJid the recipient JID
   * @param message   the message to send
   */
  public void sendMessage(String targetJid, Message message) {
    xmppWrapper.sendMessage(targetJid, messageSerializer.toJson(message));
  }

  /**
   * Handles a {@code StartOptimizationMessage} by starting a new {@link OptimizationTask}.
   * 
   * @param sourceJid the sender
   * @param message   the incoming message
   */
  protected void handleStartOptimizationMessage(String sourceJid,
      StartOptimizationMessage message) {
    // check for duplicate id
    if (optimizationTasks.containsKey(message.getId())) {
      sendMessage(sourceJid,
          new OptimizationStartedMessage(message.getId(), "Duplicate ID", Status.ERROR));
      return;
    }

    // start new optimization task
    var optimizationTask =
        new OptimizationTask(message.getId(), message.getOptimizationConfiguration(),
            message.getSimulationConfiguration(), message.getSimulationManagers(), this, sourceJid);
    optimizationTasks.put(optimizationTask.getId(), optimizationTask);
    executorService.execute(optimizationTask);

    // notify source the {@code OptimizationTask} has started
    sendMessage(sourceJid, new OptimizationStartedMessage(message.getId(), null, Status.OK));
  }

  /**
   * Handles a {@code CancelOptimizationMessage} by cancelling an {@link OptimizationTask}.
   * 
   * @param sourceJid the sender
   * @param message   the incoming message
   */
  protected void handleCancelOptimizationMessage(String sourceJid,
      CancelOptimizationMessage message) {
    var optimizationTask = optimizationTasks.get(message.getId());
    if (optimizationTask == null) {
      sendMessage(sourceJid,
          new OptimizationCancelledMessage(message.getId(), "Unknown ID", Status.ERROR));
      return;
    }
    optimizationTask.cancel();
  }

  /**
   * Handles a {@code GetProgressMessage} by requesting a progress update from an
   * {@link OptimizationTask}.
   * 
   * @param sourceJid the sender
   * @param message   the incoming message
   * 
   */
  protected void handleGetProgressMessage(String sourceJid, GetProgressMessage message) {
    var optimizationTask = optimizationTasks.get(message.getId());
    if (optimizationTask == null) {
      sendMessage(sourceJid,
          new OptimizationProgressMessage(message.getId(), "Unknown ID", Status.ERROR, 0, 0, null));
      return;
    }
    optimizationTask.requestProgressUpdate();
  }

  /**
   * Handles a {@code SimulationResultMessage} forwarding it to the appropriate
   * {@link OptimizationTask}.
   * 
   * @param sourceJid the sender
   * @param message   the incoming message
   */
  protected void handleSimulationResultMessage(String sourceJid, SimulationResultMessage message) {
    var optimizationTask = optimizationTasks.get(message.getId());
    if (optimizationTask != null) {
      optimizationTask.handleSimulationResultMessage(message, sourceJid);
    }
  }

  /**
   * Gets a map of all {@code OptimizationTask} instances.
   * 
   * @return the optimization tasks
   */
  public Map<String, OptimizationTask> getOptimizationTasks() {
    return optimizationTasks;
  }

  /**
   * Gets the {@code FrevoXmppConfiguration}.
   * 
   * @return the configuration
   */
  public FrevoXmppConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Entry point for application.
   * 
   * @param args command line arguments
   */
  public static void main(String[] args) {
    try {
      var configuration = FrevoXmppCommandLine.buildConfiguration(args);
      new FrevoXmpp(configuration);
    } catch (ParseException e) {
      System.out.println("Error parsing arguments: " + e.getMessage());
      FrevoXmppCommandLine.printHelp();
    }
  }
}
