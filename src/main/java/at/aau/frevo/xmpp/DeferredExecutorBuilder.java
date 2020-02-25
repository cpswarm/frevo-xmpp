/**
 * File: DeferredExecutorBuilder.java
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

import java.util.SplittableRandom;

import at.aau.frevo.ExecutorBuilder;
import at.aau.frevo.Problem;
import at.aau.frevo.ProblemBuilder;
import at.aau.frevo.executor.baseexecutor.BaseExecutorBuilder;

/**
 * Builder for {@code DeferredExecutor} instances.
 */
public class DeferredExecutorBuilder extends BaseExecutorBuilder<DeferredExecutor> {

  private String optimizationId;
  private String simulationConfigurationId;
  private long simulationTimeoutSeconds;
  private SimulationManager simulationManager;

  /**
   * Creates a new {@code DeferredExecutorBuilder} instance.
   */
  public DeferredExecutorBuilder() {
  }

  /**
   * Creates a new {@code DeferredExecutorBuilder} instance based on the source builder.
   * 
   * @param source the source builder
   */
  public DeferredExecutorBuilder(DeferredExecutorBuilder source) {
    super(source);
    optimizationId = source.optimizationId;
    simulationConfigurationId = source.simulationConfigurationId;
    simulationTimeoutSeconds = source.simulationTimeoutSeconds;
    simulationManager = source.simulationManager;
  }

  @Override
  public DeferredExecutor create(ProblemBuilder<? extends Problem> problemBuilder,
      SplittableRandom random) {
    return new DeferredExecutor(this, problemBuilder, random);
  }

  @Override
  public ExecutorBuilder<DeferredExecutor> cloneBuilder() {
    return new DeferredExecutorBuilder(this);
  }

  @Override
  public String getName() {
    return DeferredExecutor.class.getName();
  }

  @Override
  public DeferredExecutorBuilder setProblemVariantCount(int problemVariantCount) {
    return (DeferredExecutorBuilder) super.setProblemVariantCount(problemVariantCount);
  }

  @Override
  public DeferredExecutorBuilder setStrict(boolean strict) {
    return (DeferredExecutorBuilder) super.setStrict(strict);
  }

  @Override
  public DeferredExecutorBuilder setTimeoutMilliSeconds(long timeoutMilliSeconds) {
    return (DeferredExecutorBuilder) super.setTimeoutMilliSeconds(timeoutMilliSeconds);
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
   * Sets the optimization id.
   * 
   * @param optimizationId the optimization id
   * @return this {@code DeferredExecutorBuilder} instance
   */
  public DeferredExecutorBuilder setOptimizationId(String optimizationId) {
    this.optimizationId = optimizationId;
    return this;
  }

  /**
   * Gets the simulation configuration id.
   * 
   * @return the simulation configuration id
   */
  public String getSimulationConfigurationId() {
    return simulationConfigurationId;
  }

  /**
   * Sets the simulation configuration id.
   * 
   * @param simulationConfigurationId the simulation configuration id
   * @return this {@code DeferredExecutorBuilder} instance
   */
  public DeferredExecutorBuilder setSimulationConfigurationId(String simulationConfigurationId) {
    this.simulationConfigurationId = simulationConfigurationId;
    return this;
  }

  /**
   * Gets the simulation timeout.
   * 
   * @return the simulation timeout
   */
  public long getSimulationTimeoutSeconds() {
    return simulationTimeoutSeconds;
  }

  /**
   * Sets the simulation timeout.
   * 
   * @param simulationTimeoutSeconds the simulation timeout
   * @return this {@code DeferredExecutorBuilder} instance
   */
  public DeferredExecutorBuilder setSimulationTimeoutSeconds(long simulationTimeoutSeconds) {
    this.simulationTimeoutSeconds = simulationTimeoutSeconds;
    return this;
  }

  /**
   * Gets the {@code SimulationManager} used to receive work items.
   * 
   * @return the simulation manager
   */
  public SimulationManager getSimulationManager() {
    return simulationManager;
  }

  /**
   * Sets the {@code SimulationManager} sed to receive work items.
   * 
   * @param simulationManager the simulation manager
   * @return this {@code DeferredExecutorBuilder} instance
   */
  public DeferredExecutorBuilder setSimulationManager(SimulationManager simulationManager) {
    this.simulationManager = simulationManager;
    return this;
  }
}
