/**
 * File: WorkItem.java
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import at.aau.frevo.Evaluation;
import at.aau.frevo.representation.parameterset.ParameterSet;

/**
 * Parameter set scheduled for evaluation through simulation.
 */
public class WorkItem {
  private static AtomicInteger simulationIdCounter = new AtomicInteger(0);

  private String optimizationId;
  private String simulationConfigurationId;
  private String simulationId;
  private Evaluation<ParameterSet> evaluation;
  private CountDownLatch countDownLatch;
  private long simulationTimeoutSeconds;

  /**
   * Creates a new {@code WorkItem} instance with the specified parameters.
   * 
   * @param optimizationId            the optimization id
   * @param simulationConfigurationId the simulation configuration id
   * @param evaluation                the evaluation to be performed
   * @param countDownLatch            the count down latch for signalling completion
   * @param simulationTimeoutSeconds  the simulation timeout seconds
   */
  public WorkItem(String optimizationId, String simulationConfigurationId,
      Evaluation<ParameterSet> evaluation, CountDownLatch countDownLatch,
      long simulationTimeoutSeconds) {
    this.optimizationId = optimizationId;
    this.simulationConfigurationId = simulationConfigurationId;
    this.evaluation = evaluation;
    this.countDownLatch = countDownLatch;
    this.simulationTimeoutSeconds = simulationTimeoutSeconds;

    // create a unique simulation id
    simulationId = String.format("s_%s_%d", optimizationId, simulationIdCounter.getAndIncrement());
  }

  /**
   * Gets the optimization id.
   * 
   * @return the optimization id
   */
  public String getOptimizationId() {
    return optimizationId;
  }

  /**
   * Gets the simulation configuration id of the work item.
   * 
   * @return the simulation configuration id
   */
  public String getSimulationConfigurationId() {
    return simulationConfigurationId;
  }

  /**
   * Gets simulation id.
   * 
   * @return the simulation id
   */
  public String getSimulationId() {
    return simulationId;
  }

  /**
   * Gets the evaluation.
   * 
   * @return the evaluation
   */
  public Evaluation<ParameterSet> getEvaluation() {
    return evaluation;
  }

  /**
   * Gets the simulation timeout seconds.
   * 
   * @return the simulation timeout seconds
   */
  public long getSimulationTimeoutSeconds() {
    return simulationTimeoutSeconds;
  }

  /**
   * Marks the evaluation as complete.
   */
  public void markComplete() {
    countDownLatch.countDown();
  }
}
