/**
 * File: OptimizationManager.java
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import eu.cpswarm.optimization.parameters.ParameterOptimizationConfiguration;
import eu.cpswarm.optimization.statuses.OptimizationStatusType;
import eu.cpswarm.optimization.statuses.OptimizationTaskStatus;
import eu.cpswarm.optimization.statuses.OptimizationToolStatus;

/**
 * Manager for {@code OptimizationTask} instances.
 */
public class OptimizationManager implements Runnable {

  private static final Logger LOGGER = LogManager.getLogger(OptimizationManager.class);

  private static final int SLEEP_MILLIS = 30000;

  private FrevoWrapper wrapper;
  private Map<String, OptimizationTask> tasks = new HashMap<>();
  private ExecutorService executorService = Executors.newWorkStealingPool();

  /**
   * Creates a new {@code OptimizationManager} instance attached to the specified wrapper.
   * 
   * @param wrapper the FREVO wrapper
   */
  public OptimizationManager(FrevoWrapper wrapper) {
    this.wrapper = wrapper;
  }

  @Override
  public void run() {
    LOGGER.debug("Running");
    try {
      while (true) {
        var oldOptimizationIds = new ArrayList<String>();

        synchronized (tasks) {

          // look for old tasks
          for (var task : tasks.values()) {
            var taskStatusType = task.getStatus().getStatusType();
            if ((taskStatusType != OptimizationStatusType.STARTED)
                && (taskStatusType != OptimizationStatusType.RUNNING)) {
              var finishedSeconds =
                  (System.currentTimeMillis() - task.getFinishedMillis()) / 1000.0;
              if (finishedSeconds > task.getConfiguration().getTaskKeepAliveSeconds()) {
                oldOptimizationIds.add(task.getId());
              }
            }
          }

          // remove old tasks
          for (var optimizationId : oldOptimizationIds) {
            LOGGER.debug("Removing task {}", optimizationId);
            tasks.remove(optimizationId);
          }

        }

        // update status
        if (!oldOptimizationIds.isEmpty()) {
          updateStatus();
        }

        // TODO: remove busy wait?
        Thread.sleep(SLEEP_MILLIS);
      }
    } catch (Exception e) {
      LOGGER.error("Exception: ", e);
    }
  }

  /**
   * Creates and starts a new {@code OptimizationTask}.
   * 
   * @param id                        the id
   * @param simulationConfigurationId the simulation configuration id
   * @param configuration             the configuration
   */
  public void createOptimizationTask(String id, String simulationConfigurationId,
      ParameterOptimizationConfiguration configuration) {
    OptimizationTask task = null;
    synchronized (tasks) {
      if (!tasks.containsKey(id)) {
        task = new OptimizationTask(this, wrapper.getSimulationManager(), id,
            simulationConfigurationId, configuration);
        tasks.put(id, task);
      }
    }
    if (task != null) {
      executorService.execute(task);
    }
    updateStatus();
  }

  /**
   * Cancels an {@code OptimizationTask}.
   * 
   * @param optimizationId the id of the {@code OptimizationTask}
   */
  public void cancelOptimizationTask(String optimizationId) {
    OptimizationTask task = null;
    synchronized (tasks) {
      task = tasks.get(optimizationId);
    }

    if (task != null) {
      task.cancel();
    }
  }

  /**
   * Gets an {@code OptimizationTask} by id.
   * 
   * @param id the id
   * @return the {@code OptimizationTask} instance or {@code null} if none exists for the id
   */
  public OptimizationTask getTask(String id) {
    synchronized (tasks) {
      return tasks.get(id);
    }
  }

  /**
   * Updates the status of the optimization tool.
   */
  public void updateStatus() {
    var taskStatuses = new ArrayList<OptimizationTaskStatus>();

    synchronized (tasks) {
      for (var task : tasks.values()) {
        taskStatuses.add(task.getStatus());
      }
    }

    wrapper.setStatus(JsonSerializer.toJson(new OptimizationToolStatus(taskStatuses)));
  }
}
