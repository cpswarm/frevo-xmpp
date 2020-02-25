/**
 * File: OptimizationTask.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SplittableRandom;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import at.aau.frevo.Result;
import at.aau.frevo.method.nnga.NngaMethodBuilder;
import at.aau.frevo.representation.parameterset.Parameter;
import at.aau.frevo.representation.parameterset.ParameterSet;
import at.aau.frevo.representation.parameterset.ParameterSetBuilder;
import at.aau.frevo.representation.parameterset.ParameterSetOpBuilder;
import eu.cpswarm.optimization.messages.OptimizationStatusMessage;
import eu.cpswarm.optimization.parameters.ParameterOptimizationConfiguration;
import eu.cpswarm.optimization.statuses.OptimizationStatusType;
import eu.cpswarm.optimization.statuses.OptimizationTaskStatus;

/**
 * Optimization task implementation.
 * <p>
 * Carries out evolution on a separate thread.
 */
public class OptimizationTask implements Runnable {

  private static final Logger LOGGER = LogManager.getLogger(OptimizationTask.class);

  private OptimizationManager optimizationManager;
  private SimulationManager simulationManager;

  private String id;
  private ParameterOptimizationConfiguration configuration;
  private String simulationConfigurationId;

  private OptimizationStatusMessage snapshot;
  private OptimizationTaskStatus status;
  private boolean cancelled = false;
  private long finishedMillis = 0;

  /**
   * Creates a new {@code OptimizationTask} instance using the specified parameters
   * 
   * @param optimizationManager       the {@code OptimizationManager} owning this task
   * @param simulationManager         the associated {@code SimulationManager}
   * @param id                        the id of the task
   * @param simulationConfigurationId the simulation configuration id
   * @param configuration             the configuration
   */
  public OptimizationTask(OptimizationManager optimizationManager,
      SimulationManager simulationManager, String id, String simulationConfigurationId,
      ParameterOptimizationConfiguration configuration) {
    this.optimizationManager = optimizationManager;
    this.simulationManager = simulationManager;

    this.id = id;
    this.configuration = configuration;
    this.simulationConfigurationId = simulationConfigurationId;

    updateSnapshot(
        new OptimizationStatusMessage(id, OptimizationStatusType.STARTED, -1, 0, 0, null, null));
  }

  @Override
  public void run() {
    LOGGER.debug("Started running optimization {}", id);
    try {

      // transform parameter defintions
      var parameterList = new ArrayList<Parameter>();
      for (var p : configuration.getParameterDefinitions()) {
        parameterList.add(
            new Parameter(p.getName(), p.getMeta(), p.getMinimum(), p.getMaximum(), p.getScale()));
      }
      var parameters = parameterList.toArray(new Parameter[0]);

      var representationBuilder = new ParameterSetBuilder().setParameters(parameters)
          .setInputCount(0).setOutputCount(parameterList.size());

      var generationEvolutionSeed = configuration.getEvolutionSeed();
      var generationRandom = new SplittableRandom(generationEvolutionSeed);
      var nextGenerationEvoluationSeed = generationEvolutionSeed;

      var opBuilder = new ParameterSetOpBuilder()
          .setDirectMutationProbability(configuration.getDirectMutationProbability())
          .setDirectMutationSeverity(configuration.getDirectMutationSeverity())
          .setProportionalMutationProbability(configuration.getProportionalMutationProbability())
          .setProportionalMutationSeverity(configuration.getProportionalMutationSeverity());

      var methodBuilder = new NngaMethodBuilder().setSkewFactor(configuration.getSkewFactor())
          .setEliteWeight(configuration.getEliteWeight())
          .setRandomWeight(configuration.getRandomWeight())
          .setMutatedWeight(configuration.getMutatedWeight())
          .setCrossedWeight(configuration.getCrossedWeight())
          .setNewWeight(configuration.getNewWeight());

      var problemBuilder = new NopProblemBuilder().setRepresentationInputCount(0)
          .setRepresentationOutputCount(parameterList.size())
          .setMaximumFitness(configuration.getMaximumFitness());

      var executorBuilder =
          new DeferredExecutorBuilder().setProblemVariantCount(configuration.getVariantCount())
              .setStrict(configuration.isStrictVariantCount()).setOptimizationId(id)
              .setSimulationConfigurationId(simulationConfigurationId)
              .setSimulationTimeoutSeconds(configuration.getSimulationTimeoutSeconds())
              .setTimeoutMilliSeconds((long) (configuration.getSimulationTimeoutSeconds() * 1000
                  * configuration.getGenerationTimeoutFactor() * configuration.getCandidateCount()
                  * configuration.getVariantCount()))
              .setSimulationManager(simulationManager);

      var executor = executorBuilder.create(problemBuilder,
          new SplittableRandom(configuration.getEvaluationSeed()));

      // copy in any candidates with a fitness value
      List<Result<ParameterSet>> rankedCandidates = new ArrayList<Result<ParameterSet>>();
      var otherCandidates = new ArrayList<ParameterSet>();
      if (configuration.getCandidates() != null) {
        for (var candidate : configuration.getCandidates()) {
          var parameterSet = representationBuilder.create();
          System.arraycopy(candidate.getValues(), 0, parameterSet.getValues(), 0,
              candidate.getValues().length);

          if (candidate.getFitness() >= 0) {
            rankedCandidates.add(new Result<ParameterSet>(parameterSet, candidate.getFitness()));
            if (rankedCandidates.size() == configuration.getCandidateCount()) {
              break;
            }
          } else {
            otherCandidates.add(parameterSet);
          }
        }
      }

      // fill out candidates
      if (rankedCandidates.size() < configuration.getCandidateCount()) {

        // use candidates without fitness
        while ((rankedCandidates.size() + otherCandidates.size()) > configuration
            .getCandidateCount()) {
          otherCandidates.remove(otherCandidates.size() - 1);
        }

        // use random candidates
        var operator = opBuilder.create(representationBuilder, generationRandom);
        while ((rankedCandidates.size() + otherCandidates.size()) < configuration
            .getCandidateCount()) {
          otherCandidates.add(operator.operator0());
        }

        // evaluate them
        LOGGER.trace("Setup evaluating {} candidates", otherCandidates.size());
        rankedCandidates.addAll(executor.evaluateRepresentations(otherCandidates));
        LOGGER.trace("Setup complete, evaluated {} candidates", rankedCandidates.size());
      }

      var generation = configuration.getGeneration();

      while (true) {
        Collections.sort(rankedCandidates);

        var newConfiguration = new ParameterOptimizationConfiguration(configuration);
        newConfiguration.setGeneration(generation);
        newConfiguration.setEvolutionSeed(nextGenerationEvoluationSeed);
        newConfiguration.setCandidates(Transport.toCandidateList(rankedCandidates));

        var bestCandidate = rankedCandidates.get(0);

        // check for stop condition
        var statusType = OptimizationStatusType.RUNNING;
        if ((generation == configuration.getMaximumGeneration())
            || (bestCandidate.getFitness() >= configuration.getMaximumFitness())) {
          statusType = OptimizationStatusType.COMPLETE;
        } else if (configuration.getCandidateCount() != rankedCandidates.size()) {
          statusType = OptimizationStatusType.ERROR;
        } else if (isCancelled()) {
          statusType = OptimizationStatusType.CANCELLED;
        }

        updateSnapshot(new OptimizationStatusMessage(id, statusType, bestCandidate.getFitness(),
            generation, configuration.getMaximumGeneration(),
            Transport.toParameterList(bestCandidate.getRepresentation()), newConfiguration));

        // stop condition
        if (statusType != OptimizationStatusType.RUNNING) {
          break;
        }

        generationRandom = new SplittableRandom(nextGenerationEvoluationSeed);
        nextGenerationEvoluationSeed = generationRandom.nextLong();
        var operator = opBuilder.create(representationBuilder, generationRandom);
        var method = methodBuilder.create(rankedCandidates, operator, executor, generationRandom);

        // evolve a one generation and flush work queue
        rankedCandidates = method.run(1);
        simulationManager.cancelWorkByTask(this);

        LOGGER.debug("Optimization {}: completed generation {}", id, generation);
        generation++;
      }

    } catch (Exception e) {
      LOGGER.error("Optimization {}: {}", id, e);
      updateSnapshot(new OptimizationStatusMessage(id, OptimizationStatusType.ERROR,
          snapshot.getBestFitness(), snapshot.getGeneration(), configuration.getMaximumGeneration(),
          snapshot.getBestParameters(), snapshot.getConfiguration()));
    }

    synchronized (this) {
      finishedMillis = System.currentTimeMillis();
    }
    LOGGER.debug("Optimization {}: finished", id);
  }

  /**
   * Updates the snapshot of this optimization task. This propagated to the status of the task and
   * ultimately to the status of the optimization tool.
   * 
   * @param snapshot the new snapshot
   */
  protected synchronized void updateSnapshot(OptimizationStatusMessage snapshot) {
    this.snapshot = snapshot;
    status = new OptimizationTaskStatus(snapshot.getOptimizationId(), snapshot.getStatusType(),
        snapshot.getBestFitness(), snapshot.getGeneration(), snapshot.getMaximumGenerations());
    optimizationManager.updateStatus();
  }

  /**
   * Gets the status of the optimization task.
   * 
   * @return the status
   */
  public synchronized OptimizationTaskStatus getStatus() {
    return status;
  }

  /**
   * Gets the snapshot of the optimization task.
   * 
   * @return the snapshot
   */
  public synchronized OptimizationStatusMessage getSnapshot() {
    return snapshot;
  }

  /**
   * Cancels the optimization task.
   */
  public synchronized void cancel() {
    cancelled = true;
    simulationManager.cancelWorkByTask(this);
  }

  /**
   * Gets the cancellation flag.
   * 
   * @return the cancellation flag
   */
  public synchronized boolean isCancelled() {
    return cancelled;
  }

  /**
   * Gets the timestamp of when the optimization task finished.
   * 
   * @return the finished timestamp
   */
  public synchronized long getFinishedMillis() {
    return finishedMillis;
  }

  /**
   * Gets the id of the optimization task.
   * 
   * @return the id
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the simulation configuration id of the optimization task.
   * 
   * @return the simulation configuration id
   */
  public String getSimulationConfigurationId() {
    return simulationConfigurationId;
  }

  /**
   * Gets the configuration of the optimization task.
   * 
   * @return the configuration
   */
  public ParameterOptimizationConfiguration getConfiguration() {
    return configuration;
  }
}
