/**
 * File: OptimizationConfiguration.java
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import at.aau.frevo.Executor;
import at.aau.frevo.ExecutorBuilder;
import at.aau.frevo.Method;
import at.aau.frevo.MethodBuilder;
import at.aau.frevo.Operator;
import at.aau.frevo.OperatorBuilder;
import at.aau.frevo.Representation;
import at.aau.frevo.RepresentationBuilder;
import at.aau.frevo.executor.poolexecutor.PoolExecutorBuilder;
import at.aau.frevo.method.nnga.NngaMethodBuilder;
import at.aau.frevo.representation.fullymeshednet.FullyMeshedNetBuilder;
import at.aau.frevo.representation.fullymeshednet.FullyMeshedNetOpBuilder;

/**
 * Configuration for an {@code OptimizationTask}.
 */
public class OptimizationConfiguration {
  protected int simulationTimeoutSeconds = 120;
  protected int evolutionSeed = 0;
  protected int evaluationSeed = 0;
  protected int generationCount = 100;
  protected int candidateCount = 100;

  protected GenericProblemBuilder problemBuilder;
  protected RepresentationBuilder<? extends Representation> representationBuilder;
  protected OperatorBuilder<? extends Operator<? extends Representation>, ? extends Representation> operatorBuilder;
  protected MethodBuilder<? extends Method<? extends Representation>> methodBuilder;
  protected ExecutorBuilder<? extends Executor> executorBuilder;

  /**
   * Creates a new {@code OptimizationConfiguration} instance.
   */
  public OptimizationConfiguration() {
    problemBuilder = new GenericProblemBuilder();
    representationBuilder = new FullyMeshedNetBuilder();
    operatorBuilder = new FullyMeshedNetOpBuilder();
    methodBuilder = new NngaMethodBuilder();
    executorBuilder = new PoolExecutorBuilder();
  }

  /**
   * Gets the simulation timeout in seconds.
   * 
   * @return the simulation timeout seconds
   */
  public int getSimulationTimeoutSeconds() {
    return simulationTimeoutSeconds;
  }

  /**
   * Gets the evolution seed.
   * 
   * @return the evolution seed
   */
  public int getEvolutionSeed() {
    return evolutionSeed;
  }

  /**
   * Gets the evaluation seed.
   * 
   * @return the evaluation seed
   */
  public int getEvaluationSeed() {
    return evaluationSeed;
  }

  /**
   * Gets the generation count.
   * 
   * @return the generation count
   */
  public int getGenerationCount() {
    return generationCount;
  }

  /**
   * Gets the candidate count.
   * 
   * @return the candidate count
   */
  public int getCandidateCount() {
    return candidateCount;
  }

  /**
   * Gets the {@code GenericProblemBuilder} instance.
   * 
   * @return the {@code GenericProblemBuilder} instance
   */
  public GenericProblemBuilder getProblemBuilder() {
    return problemBuilder;
  }


  /**
   * Gets the {@code OperatorBuilder} instance.
   * 
   * @return the {@code OperatorBuilder} instance
   */
  public OperatorBuilder<? extends Operator<? extends Representation>, ? extends Representation> getOperatorBuilder() {
    return operatorBuilder;
  }

  /**
   * Gets the {@code MethodBuilder} instance.
   * 
   * @return the {@code MethodBuilder} instance
   */
  public MethodBuilder<? extends Method<? extends Representation>> getMethodBuilder() {
    return methodBuilder;
  }

  /**
   * Gets the {@code ExecutorBuilder} instance.
   * 
   * @return the {@code ExecutorBuilder} instance
   */
  public ExecutorBuilder<? extends Executor> getExecutorBuilder() {
    return executorBuilder;
  }

  /**
   * Gets the {@code RepresentationBuilder} instance.
   * 
   * @return the {@code RepresentationBuilder} instance
   */
  public RepresentationBuilder<? extends Representation> getRepresentationBuilder() {
    return representationBuilder;
  }

  /**
   * Creates a {@link Gson} instance ready for use.
   * 
   * @return the {@link Gson} instance
   */
  protected static Gson createGson() {
    var representationBuilderTypeFactory = RuntimeTypeAdapterFactory.of(RepresentationBuilder.class)
        .registerSubtype(FullyMeshedNetBuilder.class, "FullyMeshedNetBuilder");
    var operatorBuilderTypeFactory = RuntimeTypeAdapterFactory.of(OperatorBuilder.class)
        .registerSubtype(FullyMeshedNetOpBuilder.class, "FullyMeshedNetOpBuilder");
    var methodBuilderTypeFactory = RuntimeTypeAdapterFactory.of(MethodBuilder.class)
        .registerSubtype(NngaMethodBuilder.class, "NngaMethodBuilder");
    var executorBuilderTypeFactory = RuntimeTypeAdapterFactory.of(ExecutorBuilder.class)
        .registerSubtype(PoolExecutorBuilder.class, "PoolExecutorBuilder");

    return new GsonBuilder().registerTypeAdapterFactory(representationBuilderTypeFactory)
        .registerTypeAdapterFactory(operatorBuilderTypeFactory)
        .registerTypeAdapterFactory(methodBuilderTypeFactory)
        .registerTypeAdapterFactory(executorBuilderTypeFactory).create();
  }

  /**
   * Converts this {@code OptimizationConfiguration} instance to a JSON string.
   * 
   * @return the {@link OptimizationConfiguration} as a JSON string
   */
  public String toJson() {
    var gson = createGson();
    return gson.toJson(this);
  }

  /**
   * Converts a JSON string into a {@link OptimizationConfiguration} instance.
   * 
   * @param optimizationConfiguration the optimization configuration as a JSON string
   * @return the {@link OptimizationConfiguration}
   */
  public static OptimizationConfiguration fromJson(String optimizationConfiguration) {
    var gson = createGson();
    return gson.fromJson(optimizationConfiguration, OptimizationConfiguration.class);
  }
}
