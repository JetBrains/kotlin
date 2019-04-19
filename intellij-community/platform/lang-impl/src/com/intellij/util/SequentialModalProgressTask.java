/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.util;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

/**
 * Allows to execute {@link SequentialTask} under modal progress.
 * 
 * @author Denis Zhdanov
 */
public class SequentialModalProgressTask extends Task.Modal {
  private static final Logger LOG = Logger.getInstance(SequentialModalProgressTask.class);
  
  private static final long DEFAULT_MIN_ITERATION_MIN_TIME = 500;

  /**
   * We want to perform the task by big chunks at EDT. However, there is a possible case that particular task iteration
   * is executed in short amount of time. Hence, we may want to execute more than one chunk during single EDT iteration.
   * This field holds min amount of time (in milliseconds) to spend to performing the task.
   */
  private long myMinIterationTime = DEFAULT_MIN_ITERATION_MIN_TIME;

  private final String myTitle;

  private ProgressIndicator myIndicator;
  private SequentialTask myTask;

  public SequentialModalProgressTask(@Nullable Project project, @NotNull String title) {
    this(project, title, true);
  }

  public SequentialModalProgressTask(@Nullable Project project, @NotNull String title, boolean canBeCancelled) {
    super(project, title, canBeCancelled);
    myTitle = title;
  }

  @Override
  public void run(@NotNull ProgressIndicator indicator) {
    try {
      doRun(indicator);
    }
    catch (Exception e) {
      LOG.info("Unexpected exception occurred during processing sequential task '" + myTitle + "'", e);
    }
    finally {
      indicator.stop();
    }
  }

  public void doRun(@NotNull ProgressIndicator indicator) throws InvocationTargetException, InterruptedException {
    final SequentialTask task = myTask;
    if (task == null) {
      return;
    }
    
    myIndicator = indicator;
    indicator.setIndeterminate(false);
    prepare(task);
    
    // We need to sync background thread and EDT here in order to avoid situation when event queue is full of processing requests.
    while (!task.isDone()) {
      if (indicator.isCanceled()) {
        task.stop();
        break;
      }
      ApplicationManager.getApplication().invokeAndWait(() -> {
        long start = System.currentTimeMillis();
        try {
          while (!task.isDone() && System.currentTimeMillis() - start < myMinIterationTime) {
            task.iteration(indicator);
          }
        }
        catch (RuntimeException e) {
          task.stop();
          throw e;
        }
      });
    }
  }

  public void setMinIterationTime(long minIterationTime) {
    myMinIterationTime = minIterationTime;
  }

  public void setTask(@Nullable SequentialTask task) {
    myTask = task;
  }
  
  public ProgressIndicator getIndicator() {
    return myIndicator;
  }

  /**
   * Executes preliminary jobs prior to the target sequential task processing ({@link SequentialTask#prepare()} by default).
   * 
   * @param task  task to be executed
   */
  protected void prepare(@NotNull SequentialTask task) {
    task.prepare();
  }

  public abstract static class Adapter extends SequentialModalProgressTask implements SequentialTask {
    public Adapter(@Nullable Project project, @NotNull String title) {
      super(project, title);
      setTask(this);
    }

    public Adapter(@Nullable Project project, @NotNull String title, boolean canBeCancelled) {
      super(project, title, canBeCancelled);
    }

    @Override
    public void prepare() {
    }

    @Override
    public void stop() {
    }
  }
}
