/*
 * Copyright 2017 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pinterest.rocksplicator.controller.tasks;

import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.concurrent.Callable;

/**
 * Task interface.
 *
 * @author Shu Zhang (shu@pinterest.com)
 */
public abstract class Task<T> implements Callable<T> {

  public enum State {
    RUNNING,
    DONE,
    FAILED
  }

  private static final Logger LOG = LoggerFactory.getLogger(Task.class);
  protected JsonNode taskBody;
  protected String cluster;
  protected State state;

  public Task (String cluster, JsonNode taskBody) {
    this.taskBody = taskBody;
    this.cluster = cluster;
    this.state = State.RUNNING;
  }

  public String getCluster() {
    return this.cluster;
  }

  public State getState() {
    return this.state;
  }

  /**
   * Subclasses implement this method for task logic.
   * @return task response
   */
  public abstract T process();

  /**
   * Subclasses implement this method for task failure handling.
   * @return task response when it is failed
   */
  public abstract T onFailure();

  /**
   * Write the response back to DB.
   * TaskExecutionResponse's
   */
  public abstract boolean writeBackResponse(T response);

  @Override
  public T call() {
    Date date = new Date();
    long start = date.getTime();
    T response = null;
    try {
      response = process();
      this.state = State.DONE;
      return response;
    } catch (Exception e) {
      LOG.warn("Task " + this.getClass().getName() + "Failed!", e);
      response = onFailure();
      this.state = State.FAILED;
      return response;
    } finally {
      Date endDate = new Date();
      long duration = endDate.getTime() - start;
      writeBackResponse(response);
    }
  }
}
