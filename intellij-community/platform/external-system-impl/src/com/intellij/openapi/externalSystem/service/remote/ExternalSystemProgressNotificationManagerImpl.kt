// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.remote

import com.intellij.execution.rmi.RemoteObject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.openapi.util.Disposer
import com.intellij.util.EventDispatcher
import com.intellij.util.containers.DisposableWrapperList
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.ConcurrentHashMap

class ExternalSystemProgressNotificationManagerImpl : RemoteObject(), ExternalSystemProgressNotificationManager, RemoteExternalSystemProgressNotificationManager {
  private val myListeners: ConcurrentHashMap<Any, EventDispatcher<ExternalSystemTaskNotificationListener>> = ConcurrentHashMap()

  override fun addNotificationListener(listener: ExternalSystemTaskNotificationListener): Boolean {
    return addListener(ALL_TASKS_KEY, listener)
  }

  override fun addNotificationListener(listener: ExternalSystemTaskNotificationListener, parentDisposable: Disposable): Boolean {
    return addListener(ALL_TASKS_KEY, listener, parentDisposable)
  }

  override fun addNotificationListener(taskId: ExternalSystemTaskId, listener: ExternalSystemTaskNotificationListener): Boolean {
    return addListener(taskId, listener)
  }

  override fun removeNotificationListener(listener: ExternalSystemTaskNotificationListener): Boolean {
    synchronized(myListeners) {
      var removed = false
      for ((taskId, dispatcher) in myListeners) {
        removed = removed or dispatcher.listeners.remove(listener)
        if (!dispatcher.hasListeners()) {
          myListeners.remove(taskId)
        }
      }
      return removed
    }
  }

  override fun onStart(id: ExternalSystemTaskId, workingDir: String) {
    forEachListener(id) { it.onStart(id, workingDir) }
  }

  override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {
    forEachListener(event.id) { it.onStatusChange(event) }
  }

  override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
    forEachListener(id) { it.onTaskOutput(id, text, stdOut) }
  }

  override fun onEnd(id: ExternalSystemTaskId) {
    try {
      forEachListener(id) { it.onEnd(id) }
    }
    finally {
      synchronized(myListeners) {
        myListeners[id]?.listeners?.clear()
        myListeners.remove(id)
      }
    }
  }

  override fun onSuccess(id: ExternalSystemTaskId) {
    forEachListener(id) { it.onSuccess(id) }
  }

  override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
    forEachListener(id) { it.onFailure(id, e) }
  }

  override fun beforeCancel(id: ExternalSystemTaskId) {
    forEachListener(id) { it.beforeCancel(id) }
  }

  override fun onCancel(id: ExternalSystemTaskId) {
    forEachListener(id) { it.onCancel(id) }
  }

  private fun addListener(tasksKey: Any, listener: ExternalSystemTaskNotificationListener, parentDisposable: Disposable? = null): Boolean {
    synchronized(myListeners) {
      val dispatcher = myListeners[tasksKey]
      if (dispatcher != null && listener in dispatcher.listeners) return false
      myListeners.computeIfAbsent(tasksKey) {
        EventDispatcher.create(ExternalSystemTaskNotificationListener::class.java)
      }.apply {
        if (parentDisposable == null) {
          addListener(listener)
        }
        else {
          val disposable = (listeners as DisposableWrapperList).add(listener, parentDisposable)
          Disposer.register(disposable, Disposable {
            synchronized(myListeners) {
              if (listeners.size == 1) {
                myListeners.remove(tasksKey)
              }
            }
          })
        }
      }
      return true
    }
  }

  private fun forEachListener(taskId: ExternalSystemTaskId, action: (ExternalSystemTaskNotificationListener) -> Unit) {
    myListeners[taskId]?.multicaster?.run { action(this) }
    myListeners[ALL_TASKS_KEY]?.multicaster?.run { action(this) }
    ExternalSystemTaskNotificationListener.EP_NAME.forEachExtensionSafe(action::invoke)
  }

  companion object {
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
      if (taskId == null && listeners.isNotEmpty()) {
        val listenersMap = listeners.mapValues { it.value.listeners }
        throw AssertionError("Leaked listeners: $listenersMap")
      }
      if (taskId != null && listeners[taskId]?.hasListeners() == true) {
        throw AssertionError("Leaked listeners for task '$taskId': ${listeners[taskId]?.listeners}")
      }
    }
  }
}