/**
 * File: GenericProblemBuilder.java
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

import java.util.SplittableRandom;
import at.aau.frevo.ProblemBuilder;

/**
 * {@code ProblemBuilder} for {@code GenericProblem} instances
 */
public class GenericProblemBuilder extends ProblemBuilder<GenericProblem> {

  protected int representationInputCount = 6;
  protected int representationOutputCount = 2;
  protected double maximumFitness = 100;

  protected transient OptimizationTask optimizationTask;

  /**
   * Creates a new {@code GenericProblemBuilder} instance.
   */
  public GenericProblemBuilder() {
  }

  /**
   * Constructs a new {@code XorProblemBuilder} instance by copying the properties of the specified
   * instance.
   * 
   * @param source
   */
  public GenericProblemBuilder(GenericProblemBuilder source) {
    optimizationTask = source.optimizationTask;
    representationInputCount = source.representationInputCount;
    representationOutputCount = source.representationOutputCount;
    maximumFitness = source.maximumFitness;
  }

  @Override
  public GenericProblem create(SplittableRandom random) {
    return new GenericProblem(this, random);
  }

  @Override
  public String getName() {
    return GenericProblem.class.getName();
  }

  @Override
  public GenericProblemBuilder cloneBuilder() {
    return new GenericProblemBuilder(this);
  }

  @Override
  public int getRepresentationInputCount() {
    return representationInputCount;
  }

  /**
   * Sets the representation input count required by {@code GenericProblem} instances created by
   * this {@code GenericProblemBuilder}.
   * 
   * @param representationInputCount the representation input count
   * @return this {@code GenericProblemBuilder} instance
   */
  public GenericProblemBuilder setRepresentationInputCount(int representationInputCount) {
    this.representationInputCount = representationInputCount;
    return this;
  }

  @Override
  public int getRepresentationOutputCount() {
    return representationOutputCount;
  }

  /**
   * Sets the representation output count required by {@code GenericProblem} instances created by
   * this {@code GenericProblemBuilder}.
   * 
   * @param representationOutputCount the representation output count
   * @return this {@code GenericProblemBuilder} instance
   */
  public GenericProblemBuilder setRepresentationOutputCount(int representationOutputCount) {
    this.representationOutputCount = representationOutputCount;
    return this;
  }

  @Override
  public double getMaximumFitness() {
    return maximumFitness;
  }

  /**
   * Sets the maximum fitness of {@code GenericProblem} instances created by this
   * {@code GenericProblemBuilder}.
   * 
   * @param maximumFitness the maximum fitness
   * @return this {@code GenericProblemBuilder} instance
   */
  public GenericProblemBuilder setMaximumFitness(double maximumFitness) {
    this.maximumFitness = maximumFitness;
    return this;
  }

  /**
   * Gets the {@code OptimizationTask} associated with this {@code GenericProblemBuilder}.
   * 
   * @return the {@code OptimizationTask}
   */
  public OptimizationTask getOptimizationTask() {
    return optimizationTask;
  }

  /**
   * Sets the {@code OptimizationTask} associated with this {@code GenericProblemBuilder}.
   * 
   * @param optimizationTask the {@code OptimizationTask}
   * @return this {@code GenericProblemBuilder} instance
   */
  public GenericProblemBuilder setOptimizationTask(OptimizationTask optimizationTask) {
    this.optimizationTask = optimizationTask;
    return this;
  }
}
