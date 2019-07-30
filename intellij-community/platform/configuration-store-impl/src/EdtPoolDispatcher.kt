// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.configurationStore

import com.intellij.ide.SaveAndSyncHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.runAndLogException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.CoroutineContext

/**
 * Only for configuration store usages.
 */
internal val storeEdtCoroutineDispatcher: CoroutineDispatcher get() = EdtPoolDispatcher

internal class EdtPoolDispatcherManager {
  private val queue = ArrayDeque<Runnable>()
  private var isScheduled = AtomicBoolean()

  fun dispatch(block: Runnable) {
    synchronized(queue) {
      queue.add(block)
    }

    scheduleFlush()
  }

  private fun scheduleFlush() {
    if (isScheduled.compareAndSet(false, true)) {
      ApplicationManager.getApplication().invokeLater(this::processQueue)
    }
  }

  private fun getNextTask(): Runnable? {
    synchronized(queue) {
      return queue.pollFirst()
    }
  }

  fun processTasks() {
    ApplicationManager.getApplication().assertIsDispatchThread()
    isScheduled.set(true)
    processQueue()
  }

  private fun processQueue() {
    try {
      while (true) {
        val task = getNextTask() ?: return
        LOG.runAndLogException {
          // exception not expected here because kotlin must handle it (runnable here is kotlin wrapper around user task)
          task.run()
        }
      }
    }
    finally {
      isScheduled.set(false)
      val isFlushNeeded = synchronized(queue) {
        // or error occurred and we need to process rest of tasks,
        // or new tasks were added but flush not scheduled because isScheduled is setting to false on the end of processing, not on begin.
        !queue.isEmpty()
      }

      if (isFlushNeeded) {
        // do not process again - as LaterInvocator, prefer to process in small batches
        scheduleFlush()
      }
    }
  }
}

private object EdtPoolDispatcher : CoroutineDispatcher() {
  private val edtPoolDispatcherManager: EdtPoolDispatcherManager
    get() = (SaveAndSyncHandler.getInstance() as BaseSaveAndSyncHandler).edtPoolDispatcherManager

  override fun dispatch(context: CoroutineContext, block: Runnable) {
    edtPoolDispatcherManager.dispatch(block)
  }

  @ExperimentalCoroutinesApi
  override fun isDispatchNeeded(context: CoroutineContext): Boolean {
    return !ApplicationManager.getApplication().isDispatchThread
  }

  override fun toString() = "store EDT dispatcher"
}