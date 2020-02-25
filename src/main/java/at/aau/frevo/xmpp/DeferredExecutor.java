/**
 * File: DeferredExecutor.java
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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;

import at.aau.frevo.Evaluation;
import at.aau.frevo.Problem;
import at.aau.frevo.ProblemBuilder;
import at.aau.frevo.Representation;
import at.aau.frevo.executor.baseexecutor.BaseExecutor;
import at.aau.frevo.representation.parameterset.ParameterSet;

/**
 * Executor that defers execution to a queue of work items.
 */
public class DeferredExecutor extends BaseExecutor {

  private String optimizationId;
  private String simulationConfigurationId;
  private long simulationTimeoutSeconds;
  private SimulationManager simulationManager;

  /**
   * Creates a new {@code DeferredExecutor} instance using the specified parameters.
   * 
   * @param builder        the {@code DeferredExecutorBuilder}
   * @param problemBuilder the {@code ProblemBuilder}
   * @param random         the random number generator
   */
  public DeferredExecutor(DeferredExecutorBuilder builder,
      ProblemBuilder<? extends Problem> problemBuilder, SplittableRandom random) {
    super(builder, problemBuilder, random);
    optimizationId = builder.getOptimizationId();
    simulationConfigurationId = builder.getSimulationConfigurationId();
    simulationTimeoutSeconds = builder.getSimulationTimeoutSeconds();
    simulationManager = builder.getSimulationManager();
  }

  @Override
  @SuppressWarnings("unchecked")
  protected <R extends Representation> void dispatchEvaluation(
      ArrayBlockingQueue<Evaluation<R>> evaluationQueue, CountDownLatch evaluationCountDownLatch) {
    // drain the evaluation queue to the central work queue
    while (!evaluationQueue.isEmpty()) {
      simulationManager.addWork(new WorkItem(optimizationId, simulationConfigurationId,
          (Evaluation<ParameterSet>) evaluationQueue.poll(), evaluationCountDownLatch,
          simulationTimeoutSeconds));
    }
  }
}
