/**
 * File: SimulationManager.java
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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Manager of connections to remote simulation managers.
 */
public class SimulationManager implements Runnable {

  private static final Logger LOGGER = LogManager.getLogger(SimulationManager.class);

  private static final long SLEEP_MILLIS = 1000;

  private List<SmContact> contacts = new ArrayList<>();
  private Map<String, LinkedList<WorkItem>> workByScid = new HashMap<>();

  @Override
  public void run() {
    LOGGER.debug("Running");
    try {
      while (true) {
        synchronized (contacts) {
          // for all free simulation managers
          for (var contact : contacts) {
            if (contact.isFree()) {

              // try to find work
              WorkItem workItem = null;
              synchronized (workByScid) {
                var list = workByScid.get(contact.getSimulationConfigurationId());
                if (list != null) {
                  workItem = list.poll();
                }
              }
              if (workItem != null) {
                if (!contact.runSimulation(workItem)) {
                  addWork(workItem);
                }
              }
            }
          }
        }
        // TODO: remove busy wait?
        Thread.sleep(SLEEP_MILLIS);
      }
    } catch (Exception e) {
      LOGGER.error("Exception: ", e);
    }
  }

  /**
   * Adds a contact to the simulation manager.
   * 
   * @param contact the contact to add
   */
  public void addContact(SmContact contact) {
    synchronized (contacts) {
      contacts.add(contact);
    }
  }

  /**
   * Adds a work item to the appropriate queue to be executed when a simulation manager becomes
   * available.
   * 
   * @param workItem the work item to add
   */
  public void addWork(WorkItem workItem) {
    synchronized (workByScid) {
      var list = workByScid.get(workItem.getSimulationConfigurationId());
      if (list == null) {
        list = new LinkedList<>();
        workByScid.put(workItem.getSimulationConfigurationId(), list);
      }
      list.add(workItem);
    }
  }

  /**
   * Cancels all work items associated with an {@code OptimizationTask}.
   * 
   * @param task the {@code OptimizationTask}
   */
  public void cancelWorkByTask(OptimizationTask task) {
    var optimizationId = task.getId();
    synchronized (workByScid) {
      var list = workByScid.get(task.getSimulationConfigurationId());
      if (list != null) {
        // clear and remove all work assosciated with the optimizationId
        list.stream().filter(w -> w.getOptimizationId() == optimizationId)
            .forEach(w -> w.markComplete());
        list.removeIf(j -> j.getOptimizationId() == optimizationId);
      }
    }
  }
}
