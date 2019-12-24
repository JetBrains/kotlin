// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.lineMarker

import com.intellij.execution.Executor
import com.intellij.execution.actions.BaseRunConfigurationAction
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.configurations.LocatableConfiguration
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Key

private val LOG = logger<ExecutorAction>()
private val CONFIGURATION_CACHE = Key.create<List<ConfigurationFromContext>>("ConfigurationFromContext")

/**
 * @author Dmitry Avdeev
 */
@Suppress("ComponentNotRegistered")
class ExecutorAction private constructor(val origin: AnAction,
                                         private val executor: Executor,
                                         private val order: Int) : ActionGroup() {
  init {
    copyFrom(origin)
  }

  companion object {
    @JvmStatic
    @JvmOverloads
    fun getActions(order: Int = 0) = getActionList(order).toTypedArray()

    @JvmStatic
    @JvmOverloads
    fun getActionList(order: Int = 0): List<AnAction> {
      val actionManager = ActionManager.getInstance()
      val createAction = actionManager.getAction("CreateRunConfiguration")
      val extensions = Executor.EXECUTOR_EXTENSION_NAME.extensionList
      val result = ArrayList<AnAction>(extensions.size + (if (createAction == null) 0 else 1))
      extensions
        .mapNotNullTo(result) { executor ->
          actionManager.getAction(executor.contextActionId)?.let {
            ExecutorAction(it, executor, order)
          }
        }
      if (createAction != null) {
        result.add(createAction)
      }
      return result
    }

    private fun getConfigurations(dataContext: DataContext): List<ConfigurationFromContext> {
      var result = DataManager.getInstance().loadFromDataContext(dataContext, CONFIGURATION_CACHE)
      if (result == null) {
        result = computeConfigurations(dataContext)
        DataManager.getInstance().saveInDataContext(dataContext, CONFIGURATION_CACHE, result)
      }
      return result
    }

    private fun computeConfigurations(dataContext: DataContext): List<ConfigurationFromContext> {
      val originalContext = ConfigurationContext.getFromContext(dataContext)
      return originalContext.configurationsFromContext ?: return emptyList()
    }
  }

  override fun update(e: AnActionEvent) {
    val name = getActionName(e.dataContext)
    e.presentation.isEnabledAndVisible = name != null
    origin.update(e)
    if (name != null) {
      e.presentation.text = name
    }
  }

  override fun actionPerformed(e: AnActionEvent) {
    origin.actionPerformed(e)
  }

  override fun canBePerformed(context: DataContext) = origin !is ActionGroup || origin.canBePerformed(context)

  override fun getChildren(e: AnActionEvent?): Array<AnAction> = (origin as? ActionGroup)?.getChildren(e) ?: AnAction.EMPTY_ARRAY

  override fun isDumbAware() = origin.isDumbAware

  override fun isPopup() = origin !is ActionGroup || origin.isPopup

  override fun hideIfNoVisibleChildren() = origin is ActionGroup && origin.hideIfNoVisibleChildren()

  override fun disableIfNoVisibleChildren() = origin !is ActionGroup || origin.disableIfNoVisibleChildren()

  fun getActionName(dataContext: DataContext): String? {
    val list = getConfigurations(dataContext)
    if (list.isEmpty()) {
      return null
    }

    val configuration = list.get(if (order < list.size) order else 0).configuration as LocatableConfiguration
    return executor.getStartActionText(BaseRunConfigurationAction.suggestRunActionName(configuration))
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other !is ExecutorAction) {
      return false
    }

    if (origin != other.origin) return false
    if (executor != other.executor) return false
    if (order != other.order) return false

    return true
  }

  override fun hashCode(): Int {
    var result = origin.hashCode()
    result = 31 * result + executor.hashCode()
    result = 31 * result + order
    return result
  }
}