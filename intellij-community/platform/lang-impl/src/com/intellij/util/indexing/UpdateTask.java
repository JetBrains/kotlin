/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.util.indexing;

import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.util.concurrency.Semaphore;
import com.intellij.util.containers.ContainerUtil;

import java.util.Collection;
import java.util.Set;

abstract class UpdateTask<Type> {
  private final Semaphore myUpdateSemaphore = new Semaphore();
  private final Set<Type> myItemsBeingIndexed = ContainerUtil.newConcurrentSet();
  private static final boolean DEBUG = false;

  final boolean processAll(Collection<? extends Type> itemsToProcess, Project project) {
    if (DEBUG) trace("enter processAll");
    try {
      boolean hasMoreToProcess;
      boolean allItemsProcessed = true;

      do {
        hasMoreToProcess = false;
        if (DEBUG) trace("observing " + itemsToProcess.size());
        // todo we can decrease itemsToProcess
        for (Type item : itemsToProcess) {
          myUpdateSemaphore.down();

          try {
            if (DEBUG) trace("about to process");
            boolean processed = process(item, project);
            if (DEBUG) trace(processed ? "processed " : "skipped");

            if (!processed) {
              hasMoreToProcess = true;
              allItemsProcessed = false;
            }
          }
          finally {
            myUpdateSemaphore.up();
          }
          ProgressManager.checkCanceled();
        }

        do {
          ProgressManager.checkCanceled();
        }
        while (!myUpdateSemaphore.waitFor(500));
        if (DEBUG) if (hasMoreToProcess) trace("reiterating");
      }
      while (hasMoreToProcess);

      return allItemsProcessed;
    } finally {
      if (DEBUG) trace("exits processAll");
    }
  }

  private boolean process(Type item, Project project) {
    if (myItemsBeingIndexed.add(item)) {
      try {
        doProcess(item, project);
        return true;
      } finally {
        myItemsBeingIndexed.remove(item);
      }
    }
    return false;
  }

  abstract void doProcess(Type item, Project project);

  protected static void trace(String s) {
    System.out.println(Thread.currentThread() + " " + s);
  }
}