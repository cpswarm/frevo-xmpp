/**
 * File: NopProblemBuilder.java
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

import at.aau.frevo.ProblemBuilder;

/**
 * {@code ProblemBuilder} for {@code NopProblem} instances.
 */
public class NopProblemBuilder extends ProblemBuilder<NopProblem> {

  protected int representationInputCount = 1;
  protected int representationOutputCount = 1;
  protected double maximumFitness = 100;

  /**
   * Creates a new {@code NopProblemBuilder} instance.
   */
  public NopProblemBuilder() {
  }

  /**
   * Constructs a new {@code NopProblemBuilder} instance by copying the properties of the specified
   * instance.
   * 
   * @param source the source builder
   */
  public NopProblemBuilder(NopProblemBuilder source) {
    representationInputCount = source.representationInputCount;
    representationOutputCount = source.representationOutputCount;
    maximumFitness = source.maximumFitness;
  }

  @Override
  public NopProblem create(long seed) {
    return new NopProblem(this, seed);
  }

  @Override
  public String getName() {
    return NopProblem.class.getName();
  }

  @Override
  public NopProblemBuilder cloneBuilder() {
    return new NopProblemBuilder(this);
  }

  @Override
  public int getRepresentationInputCount() {
    return representationInputCount;
  }

  /**
   * Sets the representation input count required by {@code NopProblem} instances created by this
   * {@code NopProblemBuilder}.
   * 
   * @param representationInputCount the representation input count
   * @return this {@code NopProblemBuilder} instance
   */
  public NopProblemBuilder setRepresentationInputCount(int representationInputCount) {
    this.representationInputCount = representationInputCount;
    return this;
  }

  @Override
  public int getRepresentationOutputCount() {
    return representationOutputCount;
  }

  /**
   * Sets the representation output count required by {@code NopProblem} instances created by this
   * {@code NopProblemBuilder}.
   * 
   * @param representationOutputCount the representation output count
   * @return this {@code NopProblemBuilder} instance
   */
  public NopProblemBuilder setRepresentationOutputCount(int representationOutputCount) {
    this.representationOutputCount = representationOutputCount;
    return this;
  }

  @Override
  public double getMaximumFitness() {
    return maximumFitness;
  }

  /**
   * Sets the maximum fitness of {@code NopProblem} instances created by this
   * {@code NopProblemBuilder}.
   * 
   * @param maximumFitness the maximum fitness
   * @return this {@code NopProblemBuilder} instance
   */
  public NopProblemBuilder setMaximumFitness(double maximumFitness) {
    this.maximumFitness = maximumFitness;
    return this;
  }
}
