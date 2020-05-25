// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.remote

import com.intellij.execution.rmi.RemoteObject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Disposer
import com.intellij.util.containers.MultiMap
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly

class ExternalSystemProgressNotificationManagerImpl : RemoteObject(), ExternalSystemProgressNotificationManager, RemoteExternalSystemProgressNotificationManager {
  private val myListeners: MultiMap<Any, ExternalSystemTaskNotificationListener> = MultiMap.createConcurrentSet()
  override fun addNotificationListener(listener: ExternalSystemTaskNotificationListener): Boolean {
    if (listener in myListeners[ALL_TASKS_KEY]) return false
    myListeners.putValue(ALL_TASKS_KEY, listener)
    return true
  }

  override fun addNotificationListener(listener: ExternalSystemTaskNotificationListener,
                                       parentDisposable: Disposable): Boolean {
    Disposer.register(parentDisposable, Disposable { removeNotificationListener(listener) })
    return addNotificationListener(listener)
  }

  override fun addNotificationListener(taskId: ExternalSystemTaskId,
                                       listener: ExternalSystemTaskNotificationListener): Boolean {
    if (listener in myListeners[taskId]) return false
    myListeners.putValue(taskId, listener)
    return true
  }

  override fun removeNotificationListener(listener: ExternalSystemTaskNotificationListener): Boolean {
    var removed = false
    myListeners.entrySet().forEach { removed = removed or it.value.remove(listener) }
    return removed
  }

  override fun onStart(id: ExternalSystemTaskId, workingDir: String) {
    forEachListenerSafe(id) { it.onStart(id, workingDir) }
  }

  override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {
    forEachListenerSafe(event.id) { it.onStatusChange(event) }
  }

  override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
    forEachListenerSafe(id) { it.onTaskOutput(id, text, stdOut) }
  }

  override fun onEnd(id: ExternalSystemTaskId) {
    forEachListenerSafe(id) { it.onEnd(id) }
    myListeners.remove(id)
  }

  override fun onSuccess(id: ExternalSystemTaskId) {
    forEachListenerSafe(id) { it.onSuccess(id) }
  }

  override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
    forEachListenerSafe(id) { it.onFailure(id, e) }
  }

  override fun beforeCancel(id: ExternalSystemTaskId) {
    forEachListenerSafe(id) { it.beforeCancel(id) }
  }

  override fun onCancel(id: ExternalSystemTaskId) {
    forEachListenerSafe(id) { it.onCancel(id) }
  }

  private fun forEachListenerSafe(taskId: ExternalSystemTaskId, action: (ExternalSystemTaskNotificationListener) -> Unit) {
    val listeners = myListeners[taskId].asSequence() + myListeners[ALL_TASKS_KEY].asSequence()
    for (listener in listeners) {
      try {
        action.invoke(listener)
      }
      catch (e: ProcessCanceledException) {
        throw e
      }
      catch (e: java.lang.Exception) {
        LOG.error(e)
      }
    }
    ExternalSystemTaskNotificationListener.EP_NAME.forEachExtensionSafe(action::invoke)
  }

  companion object {
    private val LOG = logger<ExternalSystemProgressNotificationManagerImpl>()
    private val ALL_TASKS_KEY = Any()

    @JvmStatic
    fun getInstanceImpl(): ExternalSystemProgressNotificationManagerImpl {
      val application = ApplicationManager.getApplication()
      return application.getService(ExternalSystemProgressNotificationManager::class.java) as ExternalSystemProgressNotificationManagerImpl
    }

    @JvmStatic
    @TestOnly
    @ApiStatus.Internal
    fun assertListenersReleased(taskId: Any? = null) {
      val listeners = getInstanceImpl().myListeners
      if (taskId == null && !listeners.isEmpty) {
        throw AssertionError("Leaked listeners: $listeners")
      }
      if (taskId != null && listeners.get(taskId).isNotEmpty()) {
        throw AssertionError("Leaked listeners for task '$taskId': ${listeners.get(taskId)}")
      }
    }
  }
}