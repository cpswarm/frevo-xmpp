/**
 * File: OptimizationTask.java
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

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.JsonSyntaxException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import at.aau.frevo.Recipe;
import at.aau.frevo.Representation;
import at.aau.frevo.Result;
import at.aau.frevo.exporter.cppexporter.CppExporter;
import eu.cpswarm.optimization.messages.OptimizationCancelledMessage;
import eu.cpswarm.optimization.messages.OptimizationProgressMessage;
import eu.cpswarm.optimization.messages.ReplyMessage.Status;
import eu.cpswarm.optimization.messages.SimulationResultMessage;

/**
 * Optimization task implementation.
 * <p>
 * Carries out evolution by running a {@code Recipe} on a separate thread.
 */
public class OptimizationTask implements Runnable {

  protected final static Logger LOGGER = LogManager.getLogger(OptimizationTask.class);

  protected String id;
  protected OptimizationConfiguration optimizationConfiguration;
  protected String simulationConfiguration;
  protected Map<String, SimulationManager> simulationManagers;
  protected CppExporter exporter;
  protected FrevoXmpp frevoXmpp;
  protected String jid;

  protected volatile boolean cancelled = false;
  protected Object progressLock = new Object();
  protected double progress;
  protected Result<? extends Representation> bestResult = new Result<>(null, 0);
  protected AtomicInteger sidCounter = new AtomicInteger(0);

  /**
   * Creates a new {@code OptimizationTask} instance by creating a connection with the specified
   * parameters.
   * 
   * @param id                            the id of this instance
   * @param optimizationConfigurationJson
   * @param simulationConfiguration       the simulation configuration
   * @param simulationManagerJids         the
   * @param frevoXmpp
   * @param jid
   */
  public OptimizationTask(String id, String optimizationConfigurationJson,
      String simulationConfiguration, List<String> simulationManagerJids, FrevoXmpp frevoXmpp,
      String jid) {
    LOGGER.trace("Starting OptimizationTask: " + id + ", " + optimizationConfigurationJson + ", "
        + simulationConfiguration + ", " + simulationManagerJids.toString());

    this.id = id;

    try {
      optimizationConfiguration = OptimizationConfiguration.fromJson(optimizationConfigurationJson);
      if (optimizationConfiguration == null) {
        optimizationConfiguration = new OptimizationConfiguration();
      }
    } catch (JsonSyntaxException e) {
      optimizationConfiguration = new OptimizationConfiguration();
    }
    LOGGER.trace("Round trip optimizationConfiguration: " + optimizationConfiguration.toJson());

    this.simulationConfiguration = simulationConfiguration;
    this.frevoXmpp = frevoXmpp;
    this.jid = jid;

    // create associated simulation manager wrappers
    simulationManagers = Collections.synchronizedMap(new HashMap<>());
    for (var simulationManagerJid : simulationManagerJids) {
      simulationManagers.put(simulationManagerJid,
          new SimulationManager(simulationManagerJid, this, frevoXmpp));
    }

    exporter = new CppExporter(frevoXmpp.getConfiguration().isCodeGenerationEnabled());
  }

  @Override
  public void run() {
    try {

      var recipe = Recipe.forceConstruction(optimizationConfiguration.getRepresentationBuilder(),
          optimizationConfiguration.getOperatorBuilder(),
          optimizationConfiguration.getMethodBuilder(),
          optimizationConfiguration.getExecutorBuilder(),
          optimizationConfiguration.getProblemBuilder(),
          optimizationConfiguration.getEvolutionSeed(),
          optimizationConfiguration.getEvaluationSeed());

      recipe.getProblemBuilder().setOptimizationTask(this);

      // DO IT!
      recipe.prepare(optimizationConfiguration.getCandidateCount());
      for (var i = 0; i < optimizationConfiguration.getGenerationCount(); i++) {

        LOGGER.trace(
            "Starting generation: " + i + " of " + optimizationConfiguration.getGenerationCount());

        if (cancelled) {
          frevoXmpp.sendMessage(jid, new OptimizationCancelledMessage(id, null, Status.OK));
          break;
        }

        // optimize through one generation
        var results = recipe.run(1);
        var result = results.get(0);

        // update progress
        synchronized (progressLock) {

          if (result.getFitness() > bestResult.getFitness()) {
            bestResult = result;
          }

          if (result.getFitness() >= recipe.getProblemBuilder().getMaximumFitness()) {
            progress = 100;
            break;
          }
          progress = 100.0 * i / optimizationConfiguration.getGenerationCount();
        }

        // optionally dump top result to c file
        var baseCandidateFile = frevoXmpp.getConfiguration().getBaseCandidateFilename();
        if ((baseCandidateFile != null) && (bestResult.getRepresentation() != null)) {
          var suffix = "_" + id + "_" + i + "_" + (int) (bestResult.getFitness()) + ".c";
          try (var writer = new PrintWriter(baseCandidateFile + suffix)) {
            writer.println(exporter.toCode(bestResult.getRepresentation()));
          } catch (FileNotFoundException e) {
            LOGGER.warn("Could not write result file", e);
          }
        }
      }

      // send final progress
      synchronized (progressLock) {
        progress = 100;
        sendProgress(Status.OK, null);
      }
      LOGGER.trace("Optimization complete");
    } catch (Exception e) {
      LOGGER.error("An exception occurred while optimizing", e);
      sendProgress(Status.ERROR, e.toString());
    } finally {
      frevoXmpp.getOptimizationTasks().remove(this.id);
    }
  }

  /**
   * Helper method to send back progress.
   * 
   * @param status      the status to send
   * @param description the description to send
   */
  protected void sendProgress(Status status, String description) {
    var bestFitness = 0.0;
    var bestCandidate = "";

    if (bestResult != null) {
      bestFitness = bestResult.getFitness();
      if (bestResult.getRepresentation() != null) {
        bestCandidate = exporter.toCode(bestResult.getRepresentation());
      }
    }
    frevoXmpp.sendMessage(jid, new OptimizationProgressMessage(id, description, status, progress,
        bestFitness, bestCandidate));
  }

  /**
   * Cancels the {@code OptimizationTask}.
   */
  public void cancel() {
    cancelled = true;
  }

  /**
   * Requests that the {@code OptimizationTask} sends an {@code OptimizationProgressMessage}.
   */
  public void requestProgressUpdate() {
    synchronized (progressLock) {
      sendProgress(Status.OK, null);
    }
  }

  /**
   * Tests a candidate by sending out a {@code RunSimulationMessage}.
   * 
   * @param candidate the candidate representaiton to test
   * @return the fitness value
   */
  public double evaluateRepresentation(Representation candidate) {
    while (true) {
      // try to aquire a SimulationManager and evaluate the Representation
      for (var simulationManager : simulationManagers.values()) {
        if (simulationManager.tryAcquire()) {
          return simulationManager.evaluateRepresentation(candidate);
        }
      }

      // wait before retrying
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        LOGGER.trace("Interrupted while evaluating representation ", e);
        return 0;
      }
    }
  }

  /**
   * Handles a incoming {@code SimulationResultMessage}.
   * 
   * @param message the incoming message
   * @param jid     the used to route the message
   */
  public void handleSimulationResultMessage(SimulationResultMessage message, String jid) {
    var simulationManager = simulationManagers.get(jid);
    if (simulationManager != null) {
      simulationManager.handleSimulationResultMessage(message);
    }
  }

  /**
   * Gets the id of this {@code OptimizationTask}.
   * 
   * @return the id
   */
  public String getId() {
    return id;
  }

  /***
   * Gets the optimization configuration for this {@code OptimizationTask}.
   * 
   * @return the optimization configuration
   */
  public OptimizationConfiguration getOptimizationConfiguration() {
    return optimizationConfiguration;
  }

  /**
   * Gets the simulation configuration for this {@code OptimizationTask}.
   * 
   * @return the simulation configuration
   */
  public String getSimulationConfiguration() {
    return simulationConfiguration;
  }

  /**
   * Gets the next simulation id for this {@code OptimizationTask}.
   * 
   * @return the next simulation id to use
   */
  public String getNextSimulationId() {
    return Integer.toString(sidCounter.getAndIncrement());
  }

  /**
   * Gets the exporter attached to this {@code OptimizationTask}.
   * 
   * @return the exporter
   */
  public CppExporter getExporter() {
    return exporter;
  }
}
