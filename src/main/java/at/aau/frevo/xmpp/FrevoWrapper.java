/**
 * File: FrevoWrapper.java
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.Jid;

import eu.cpswarm.optimization.statuses.OptimizationToolStatus;
import eu.cpswarm.optimization.statuses.SOOStatus;
import eu.cpswarm.optimization.statuses.SimulationManagerStatus;

/**
 * FREVO specific XMPP wrapper.
 */
public class FrevoWrapper extends Wrapper {

  private static final Logger LOGGER = LogManager.getLogger(FrevoWrapper.class);

  private SimulationManager simulationManager = new SimulationManager();
  private OptimizationManager optimizationManager = new OptimizationManager(this);
  private ExecutorService executorService = Executors.newWorkStealingPool();

  /**
   * Creates a new {@code FrevoWrapper} instance using the specified connection configuration.
   * <p>
   * Starts associated {@code SimulationManager} and {@code OptimizationManager} instances.
   * <p>
   * This method does not return.
   * 
   * @param connectionConfiguration the connection configuration
   */
  public FrevoWrapper(XMPPTCPConnectionConfiguration connectionConfiguration) {
    super(connectionConfiguration);
    executorService.execute(simulationManager);
    executorService.execute(optimizationManager);
    setStatus(JsonSerializer.toJson(new OptimizationToolStatus()));
    maintainConnection();
  }

  @Override
  protected Contact createContact(Jid jid, String statusString) {
    var status = JsonSerializer.toStatus(statusString);
    if (status instanceof SimulationManagerStatus) {
      var contact = new SmContact(this, jid, (SimulationManagerStatus) status);
      simulationManager.addContact(contact);
      return contact;
    } else if (status instanceof SOOStatus) {
      return new SooContact(this, jid);
    } else {
      LOGGER.warn("No contact created for {}: {}", jid.toString(), statusString);
      return null;
    }
  }

  /**
   * Gets the {@code SimulationManager} associated with this wrapper.
   * 
   * @return the simulationManager
   */
  public SimulationManager getSimulationManager() {
    return simulationManager;
  }

  /**
   * Gets the {@code OptimizationManager} associated with this wrapper.
   * 
   * @return the optimizationManager
   */
  public OptimizationManager getOptimizationManager() {
    return optimizationManager;
  }
}
