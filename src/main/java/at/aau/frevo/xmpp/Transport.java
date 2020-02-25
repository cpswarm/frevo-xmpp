/**
 * File: Transport.java
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
import java.util.List;

import at.aau.frevo.Result;
import at.aau.frevo.representation.parameterset.ParameterSet;
import eu.cpswarm.optimization.parameters.Candidate;

/**
 * Helper functions for converting to / from shared types for transport.
 */
public class Transport {

  /**
   * Prepares a parameter set for transport.
   * 
   * @param parameterSet the parameter set to pack
   * @return the packed parameter set
   */
  public static ArrayList<eu.cpswarm.optimization.parameters.Parameter> toParameterList(
      ParameterSet parameterSet) {
    var parameterTransport = new ArrayList<eu.cpswarm.optimization.parameters.Parameter>();
    var bestValues = parameterSet.getValues();
    var bestParameters = parameterSet.getParameters();
    for (int i = 0; i < bestValues.length; i++) {
      var p = bestParameters[i];
      var v = bestValues[i];
      parameterTransport.add(new eu.cpswarm.optimization.parameters.Parameter(p.getName(),
          p.getMetaInformation(), p.getScale() * v));
    }
    return parameterTransport;
  }

  /**
   * Prepares candidate parameter set instances for transport.
   * 
   * @param candidates the candidates to pack
   * @return the packed candidates
   */
  public static ArrayList<Candidate> toCandidateList(List<Result<ParameterSet>> candidates) {
    var candidateList = new ArrayList<Candidate>();
    for (var candidate : candidates) {
      candidateList
          .add(new Candidate(candidate.getRepresentation().getValues(), candidate.getFitness()));
    }
    return candidateList;
  }
}
