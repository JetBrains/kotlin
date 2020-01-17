// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.runAnything.activity

import com.intellij.icons.AllIcons.Actions.Run_anything
import com.intellij.ide.IdeBundle
import com.intellij.ide.actions.runAnything.RunAnythingUtil.fetchProject
import com.intellij.ide.actions.runAnything.activity.RunAnythingNotifiableProvider.ExecutionStatus.ERROR
import com.intellij.ide.actions.runAnything.activity.RunAnythingNotifiableProvider.ExecutionStatus.SUCCESS
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.notification.Notifications
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext

/**
 * Implement notifiable provider if you desire to run an arbitrary activity in the IDE, that may hasn't provide visual effects,
 * and show notification about it with optional actions.
 *
 * @param V see [RunAnythingProvider]
 */
abstract class RunAnythingNotifiableProvider<V> : RunAnythingProviderBase<V>() {

  private val RUN_ANYTHING_GROUP_ID = IdeBundle.message("run.anything.custom.activity.notification.group.id")

  private val notificationConfigurators = LinkedHashMap<ExecutionStatus, NotificationBuilder.() -> Unit>()

  /**
   * Runs an activity silently.
   *
   * @param dataContext 'Run Anything' data context
   * @return true if succeed, false is failed
   */
  protected abstract fun run(dataContext: DataContext, value: V): Boolean

  override fun execute(dataContext: DataContext, value: V) {
    try {
      when (run(dataContext, value)) {
        true -> notifyNotificationIfNeeded(SUCCESS, dataContext, value)
        else -> notifyNotificationIfNeeded(ERROR, dataContext, value)
      }
    }
    catch (ex: Throwable) {
      notifyNotificationIfNeeded(ERROR, dataContext, value)
      throw ex
    }
  }

  private fun notifyNotificationIfNeeded(status: ExecutionStatus, dataContext: DataContext, value: V) {
    val configure = notificationConfigurators[status] ?: return
    val builder = NotificationBuilder(dataContext, value)
    val notification = builder.apply(configure).build()
    val project = fetchProject(dataContext)
    Notifications.Bus.notify(notification, project)
  }

  protected fun notification(after: ExecutionStatus = SUCCESS, configure: NotificationBuilder.() -> Unit) {
    notificationConfigurators[after] = configure
  }

  protected inner class NotificationBuilder(val dataContext: DataContext, val value: V) {
    private val actions = ArrayList<ActionData>()

    var title: String? = null
    var subtitle: String? = null
    var content: String? = null

    fun action(name: String, perform: () -> Unit) {
      actions.add(ActionData(name, perform))
    }

    fun build(): Notification {
      val notification = Notification(RUN_ANYTHING_GROUP_ID, Run_anything, title, subtitle, content, INFORMATION, null)
      for (actionData in actions) {
        val action = object : AnAction(actionData.name) {
          override fun actionPerformed(e: AnActionEvent) {
            actionData.perform()
            notification.expire()
          }
        }
        notification.addAction(action)
      }
      return notification
    }
  }

  protected enum class ExecutionStatus { SUCCESS, ERROR }

  private data class ActionData(val name: String, val perform: () -> Unit)

  init {
    notification(ERROR) {
      title = IdeBundle.message("run.anything.notification.warning.title")
      content = IdeBundle.message("run.anything.notification.warning.content", getCommand(value))
    }
  }
}