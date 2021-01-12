// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.remote

import com.intellij.execution.rmi.RemoteObject
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskId
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationEvent
import com.intellij.openapi.externalSystem.model.task.ExternalSystemTaskNotificationListener
import com.intellij.openapi.externalSystem.service.notification.ExternalSystemProgressNotificationManager
import com.intellij.util.EventDispatcher
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.annotations.TestOnly

class ExternalSystemProgressNotificationManagerImpl : RemoteObject(), ExternalSystemProgressNotificationManager, RemoteExternalSystemProgressNotificationManager {
  private val dispatcher = EventDispatcher.create(ExternalSystemTaskNotificationListener::class.java)

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
    val toRemove = dispatcher.listeners.filter { (it as TaskListenerWrapper).delegate === listener }
    dispatcher.listeners.removeAll(toRemove)
    return toRemove.isNotEmpty()
  }

  override fun onStart(id: ExternalSystemTaskId, workingDir: String) {
    forEachListener { it.onStart(id, workingDir) }
  }

  override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {
    forEachListener { it.onStatusChange(event) }
  }

  override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
    forEachListener { it.onTaskOutput(id, text, stdOut) }
  }

  override fun onEnd(id: ExternalSystemTaskId) {
    try {
      forEachListener { it.onEnd(id) }
    }
    finally {
      val toRemove = dispatcher.listeners.filter { (it as TaskListenerWrapper).taskId === id }
      dispatcher.listeners.removeAll(toRemove)
    }
  }

  override fun onSuccess(id: ExternalSystemTaskId) {
    forEachListener { it.onSuccess(id) }
  }

  override fun onFailure(id: ExternalSystemTaskId, e: Exception) {
    forEachListener { it.onFailure(id, e) }
  }

  override fun beforeCancel(id: ExternalSystemTaskId) {
    forEachListener { it.beforeCancel(id) }
  }

  override fun onCancel(id: ExternalSystemTaskId) {
    forEachListener { it.onCancel(id) }
  }

  private fun addListener(tasksKey: Any, listener: ExternalSystemTaskNotificationListener, parentDisposable: Disposable? = null): Boolean {
    val wrapper = TaskListenerWrapper(tasksKey, listener)
    if (dispatcher.listeners.contains(wrapper)) return false
    if (parentDisposable == null) {
      dispatcher.addListener(wrapper)
    }
    else {
      dispatcher.addListener(wrapper, parentDisposable)
    }
    return true
  }

  private fun forEachListener(action: (ExternalSystemTaskNotificationListener) -> Unit) {
    action.invoke(dispatcher.multicaster)
    ExternalSystemTaskNotificationListener.EP_NAME.forEachExtensionSafe(action::invoke)
  }

  private class TaskListenerWrapper(
    val taskId: Any,
    val delegate: ExternalSystemTaskNotificationListener
  ) : ExternalSystemTaskNotificationListener {
    override fun onSuccess(id: ExternalSystemTaskId) {
      if (taskId !== ALL_TASKS_KEY && taskId !== id) return
      delegate.onSuccess(id)
    }

    override fun onFailure(id: ExternalSystemTaskId, e: java.lang.Exception) {
      if (taskId !== ALL_TASKS_KEY && taskId !== id) return
      delegate.onFailure(id, e)
    }

    override fun onTaskOutput(id: ExternalSystemTaskId, text: String, stdOut: Boolean) {
      if (taskId !== ALL_TASKS_KEY && taskId !== id) return
      delegate.onTaskOutput(id, text, stdOut)
    }

    override fun onStatusChange(event: ExternalSystemTaskNotificationEvent) {
      if (taskId !== ALL_TASKS_KEY && taskId !== event.id) return
      delegate.onStatusChange(event)
    }

    override fun onCancel(id: ExternalSystemTaskId) {
      if (taskId !== ALL_TASKS_KEY && taskId !== id) return
      delegate.onCancel(id)
    }

    override fun onEnd(id: ExternalSystemTaskId) {
      if (taskId !== ALL_TASKS_KEY && taskId !== id) return
      delegate.onEnd(id)
    }

    override fun beforeCancel(id: ExternalSystemTaskId) {
      if (taskId !== ALL_TASKS_KEY && taskId !== id) return
      delegate.beforeCancel(id)
    }

    override fun onStart(id: ExternalSystemTaskId, workingDir: String?) {
      if (taskId !== ALL_TASKS_KEY && taskId !== id) return
      delegate.onStart(id, workingDir)
    }

    override fun onStart(id: ExternalSystemTaskId) {
      if (taskId !== ALL_TASKS_KEY && taskId !== id) return
      @Suppress("DEPRECATION")
      delegate.onStart(id)
    }

    override fun equals(other: Any?): Boolean {
      if (this === other) return true
      if (javaClass != other?.javaClass) return false
      other as TaskListenerWrapper
      if (taskId !== other.taskId) return false
      if (delegate !== other.delegate) return false
      return true
    }

    override fun hashCode(): Int {
      var result = taskId.hashCode()
      result = 31 * result + delegate.hashCode()
      return result
    }
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
    fun getListeners(): Map<Any, List<ExternalSystemTaskNotificationListener>> {
      return getInstanceImpl().dispatcher.listeners.groupBy({ (it as TaskListenerWrapper).taskId },
                                                            { (it as TaskListenerWrapper).delegate })
    }

    @JvmStatic
    @TestOnly
    @ApiStatus.Internal
    fun assertListenersReleased(taskId: Any? = null) {
      val listeners = getListeners()
      if (taskId == null && listeners.isNotEmpty()) {
        throw AssertionError("Leaked listeners: $listeners")
      }
      if (taskId != null && listeners.containsKey(taskId)) {
        throw AssertionError("Leaked listeners for task '$taskId': ${listeners[taskId]}")
      }
    }
  }
}