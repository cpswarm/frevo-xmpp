/**
 * File: GenericProblem.java
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
import at.aau.frevo.Problem;
import at.aau.frevo.Representation;

/**
 * Generic problem that delegates evaulation to a remote simulation manager.
 */
public class GenericProblem extends Problem {

  protected OptimizationTask optimizationTask;

  /**
   * Creates a new {@code GenericProblem} instance with the specified configuration.
   * 
   * @param builder the {@code GenericProblemBuilder} used for configuration
   * @param random  the random number generator to use
   */
  public GenericProblem(GenericProblemBuilder builder, SplittableRandom random) {
    super(random);
    optimizationTask = builder.getOptimizationTask();
  }

  @Override
  public double evaluateRepresentation(Representation representation) {

    // TODO: transfer seed for random number generator
    if (optimizationTask != null) {
      return optimizationTask.evaluateRepresentation(representation);
    }
    return 0;
  }
}
