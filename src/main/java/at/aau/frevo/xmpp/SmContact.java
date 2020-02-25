/**
 * File: SmContact.java
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

import eu.cpswarm.optimization.messages.RunSimulationMessage;
import eu.cpswarm.optimization.messages.SimulationResultMessage;
import eu.cpswarm.optimization.statuses.SimulationManagerStatus;

/**
 * Simulation manager contact. Used to run simulations remotely.
 */
public class SmContact extends Contact {

  private static final Logger LOGGER = LogManager.getLogger(SmContact.class);

  private SimulationManager simulationManager;
  private WorkItem currentWorkItem;
  private String simulationConfigurationId;
  private String simulationId;
  private long currentWorkItemStartMillis;

  /**
   * Creates a new {@code SmContact} instance with the specified wrapper, jid and status.
   * 
   * @param wrapper the wrapper
   * @param jid     the jid
   * @param status  the initial status
   */
  public SmContact(FrevoWrapper wrapper, Jid jid, SimulationManagerStatus status) {
    super(wrapper, jid);
    LOGGER.debug("Created SmContact: {}", jid.toString());
    simulationManager = wrapper.getSimulationManager();
    handleStatus(status);
  }

  @Override
  protected void handleStatus(String statusString) {
    var status = JsonSerializer.toStatus(statusString);
    if (status instanceof SimulationManagerStatus) {
      handleStatus((SimulationManagerStatus) status);
    } else {
      LOGGER.warn("Unable to process status: {}", statusString);
    }
  }

  @Override
  protected void handleMessage(String messageString) {
    var message = JsonSerializer.toMessage(messageString);
    if (message instanceof SimulationResultMessage) {
      handleSimulationResultMessage((SimulationResultMessage) message);
    } else {
      LOGGER.warn("Unable to process message: {}", messageString);
    }
  }

  /**
   * Handles an incoming simulation result message to finish processing the current work item.
   * 
   * @param message the message
   */
  private synchronized void handleSimulationResultMessage(SimulationResultMessage message) {
    LOGGER.trace("handleSimulationResultMessage: {}", JsonSerializer.toJson(message));

    if (currentWorkItem == null) {
      return;
    }

    if (message.getSimulationId().equals(currentWorkItem.getSimulationId())) {
      if (message.getSuccess()) {
        currentWorkItem.getEvaluation().setFitness(message.getFitnessValue());
        currentWorkItem.markComplete();
        currentWorkItem = null;
      } else {
        putBackWorkItem();
      }
    }
  }

  /**
   * Handles an incoming status.
   * 
   * @param status the status
   */
  private synchronized void handleStatus(SimulationManagerStatus status) {
    LOGGER.trace("handleStatus: {}", JsonSerializer.toJson(status));

    // parse fields
    simulationConfigurationId = status.getSimulationConfigurationId();
    if ((simulationConfigurationId != null) && (simulationConfigurationId.length() == 0)) {
      simulationConfigurationId = null;
    }
    simulationId = status.getSimulationId();
    if ((simulationId != null) && (simulationId.length() == 0)) {
      simulationId = null;
    }

    // cancel current work item if required
    if (currentWorkItem != null) {
      // cancel if SM has gone offline
      if (!isAvailable()) {
        putBackWorkItem();
      }
      // cancel if SM is working on something else
      else if ((simulationId != null) && !simulationId.equals(currentWorkItem.getSimulationId())) {
        putBackWorkItem();
      }
      // cancel if SM has configured for a different sort of simulation
      else if ((simulationConfigurationId != null)
          && !simulationConfigurationId.equals(currentWorkItem.getSimulationConfigurationId())) {
        putBackWorkItem();
      }
    }

  }

  /**
   * Determines if the simulation manager is likely free and can accept work.
   * 
   * @return {@code true} if the simulation manager is likely free.
   */
  public synchronized boolean isFree() {
    // check for timeout
    if (currentWorkItem != null) {
      if (currentWorkItem.getSimulationTimeoutSeconds() != 0) {
        var simulationTimeSeconds =
            (System.currentTimeMillis() - currentWorkItemStartMillis) / 1000.0;
        if (simulationTimeSeconds > currentWorkItem.getSimulationTimeoutSeconds()) {
          putBackWorkItem();
        }
      }
    }

    // check all conditions
    if (!isAvailable()) {
      return false;
    }

    if (currentWorkItem != null) {
      return false;
    }

    if (simulationId != null) {
      return false;
    }

    return true;
  }

  /**
   * Gets the simulation configuration id.
   * 
   * @return the simulation configuration id
   */
  public synchronized String getSimulationConfigurationId() {
    return simulationConfigurationId;
  }

  /**
   * Tries to run a simulation by sending the simulation manager a {@code RunSimulationMessage}.
   * 
   * @param workItem the work item to run
   * @return {@code true} if success
   */
  public boolean runSimulation(WorkItem workItem) {

    // try to set the current work item
    synchronized (this) {
      if (!isFree()) {
        return false;
      }

      if (!workItem.getSimulationConfigurationId().equals(simulationConfigurationId)) {
        return false;
      }

      currentWorkItem = workItem;
      currentWorkItemStartMillis = System.currentTimeMillis();
    }

    if (sendMessage(JsonSerializer.toJson(new RunSimulationMessage(workItem.getOptimizationId(),
        workItem.getSimulationId(), workItem.getEvaluation().getSeed(),
        Transport.toParameterList(workItem.getEvaluation().getRepresentation()))))) {
      return true;
    } else {
      putBackWorkItem();
      return false;
    }
  }

  /**
   * Puts the current work item back in the simulation manager's queue.
   */
  private synchronized void putBackWorkItem() {
    if (currentWorkItem != null) {
      simulationManager.addWork(currentWorkItem);
      currentWorkItem = null;
    }
  }
}
