/**
 * File: SimulationManager.java
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.aau.frevo.Representation;
import eu.cpswarm.optimization.messages.RunSimulationMessage;
import eu.cpswarm.optimization.messages.SimulationResultMessage;

/**
 * Local representation of a remote simulation manager.
 */
public class SimulationManager {

  protected final static Logger LOGGER = LogManager.getLogger(SimulationManager.class);

  protected String jid;
  protected OptimizationTask optimizationTask;
  protected FrevoXmpp frevoXmpp;

  protected boolean busy;
  protected double fitnessValue;
  protected CountDownLatch latch;

  /**
   * Creates a new {@code SimulationManager} instance.
   * 
   * @param jid              the JID of the remote simulation manager
   * @param optimizationTask the {@code OptimizationTask} associated with this
   *                         {@code SimulationManager}
   * @param frevoXmpp        the {@code FrevoXmpp} associated with this {@code SimulationManager}
   */
  public SimulationManager(String jid, OptimizationTask optimizationTask, FrevoXmpp frevoXmpp) {
    this.jid = jid;
    this.optimizationTask = optimizationTask;
    this.frevoXmpp = frevoXmpp;
    busy = false;
  }

  /**
   * Tries to acquire this {@code SimulationManager} for evaluating a {@code Representation}.
   * 
   * @return {@code true} if successfully acquired, otherwise {@code false}
   */
  public synchronized boolean tryAcquire() {
    if (busy) {
      return false;
    }
    busy = true;
    fitnessValue = 0;
    return true;
  }

  /**
   * Evaluates a {@code Representation}.
   * <p>
   * Sends a {@code RunSimulationMessage} and awaits a {@code SimulationResultMessage}.
   * 
   * @param representation the {@code Representation} to evaluate
   * @return the fitness value
   */
  public double evaluateRepresentation(Representation representation) {
    latch = new CountDownLatch(1);
    LOGGER.trace("Running simulation on: " + jid);

    frevoXmpp.sendMessage(jid,
        new RunSimulationMessage(optimizationTask.getId(), null,
            optimizationTask.getNextSimulationId(), optimizationTask.getSimulationConfiguration(),
            optimizationTask.prepareParameterSetForTransport(representation)));
    try {
      // wait and block until a {@code SimulationResultMessage} arrives or timeout occurs
      latch.await(optimizationTask.getOptimizationConfiguration().getSimulationTimeoutSeconds(),
          TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      LOGGER.trace("Simulation timed out", e);
    }

    synchronized (this) {
      LOGGER.trace("Simulation complete on: " + jid + ", fitness=" + fitnessValue);
      busy = false;
      return fitnessValue;
    }
  }

  /**
   * Handles a {@code SimulationResultMessage} by signalling the waiting thread.
   * 
   * @param message the {@code SimulationResultMessage}
   */
  public synchronized void handleSimulationResultMessage(SimulationResultMessage message) {
    if (busy) {
      fitnessValue = message.getFitnessValue();
      latch.countDown();
    }
  }
}
