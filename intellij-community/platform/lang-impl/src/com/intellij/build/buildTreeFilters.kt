// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("BuildTreeFilters")
package com.intellij.build

import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.util.registry.Registry
import org.jetbrains.annotations.ApiStatus
import java.util.function.Predicate

private val SUCCESSFUL_STEPS_FILTER = Predicate { node: ExecutionNode -> !node.isFailed && !node.hasWarnings() }
private val WARNINGS_FILTER = Predicate { node: ExecutionNode -> node.hasWarnings() || node.hasInfos() }

@ApiStatus.Experimental
fun createFilteringActionsGroup(filterable: Filterable<ExecutionNode>): DefaultActionGroup {
  val actionGroup = DefaultActionGroup("Filters", true)
  actionGroup.templatePresentation.icon = AllIcons.Actions.Show
  actionGroup.add(WarningsToggleAction(filterable))
  actionGroup.add(SuccessfulStepsToggleAction(filterable))
  return actionGroup
}

@ApiStatus.Experimental
fun install(filterable: Filterable<ExecutionNode>) {
  val filteringEnabled = filterable.isFilteringEnabled
  if (!filteringEnabled) return
  SuccessfulStepsToggleAction.install(filterable)
  WarningsToggleAction.install(filterable)
}

@ApiStatus.Experimental
open class FilterToggleAction constructor(text: String,
                                          private val stateKey: String?,
                                          private val filterable: Filterable<ExecutionNode>,
                                          private val filter: Predicate<ExecutionNode>,
                                          private val defaultState: Boolean) : ToggleAction(text), DumbAware {
  override fun isSelected(e: AnActionEvent): Boolean {
    val presentation = e.presentation
    if (!Registry.`is`("build.view.side-by-side", true)) {
      presentation.isVisible = false
      return false
    }
    val filteringEnabled = filterable.isFilteringEnabled
    presentation.isEnabledAndVisible = filteringEnabled
    if (filteringEnabled && stateKey != null &&
        PropertiesComponent.getInstance().getBoolean(stateKey, defaultState) &&
        !filterable.contains(filter)) {
      setSelected(e, true)
    }

    return filterable.contains(filter)
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    if (state) {
      filterable.addFilter(filter)
    }
    else {
      filterable.removeFilter(filter)
    }
    if (stateKey != null) {
      PropertiesComponent.getInstance().setValue(stateKey, state, defaultState)
    }
  }

  companion object {
    fun install(filterable: Filterable<ExecutionNode>,
                filter: Predicate<ExecutionNode>,
                stateKey: String,
                defaultState: Boolean) {
      if (PropertiesComponent.getInstance().getBoolean(stateKey, defaultState) &&
          !filterable.contains(filter)) {
        filterable.addFilter(filter)
      }
    }
  }
}

@ApiStatus.Experimental
class SuccessfulStepsToggleAction(filterable: Filterable<ExecutionNode>) :
  FilterToggleAction("Show Successful Steps", STATE_KEY, filterable, SUCCESSFUL_STEPS_FILTER, false), DumbAware {
  companion object {
    private const val STATE_KEY = "build.toolwindow.show.successful.steps.selection.state"
    fun install(filterable: Filterable<ExecutionNode>) {
      install(filterable, SUCCESSFUL_STEPS_FILTER, STATE_KEY, false)
    }
  }
}

@ApiStatus.Experimental
class WarningsToggleAction(filterable: Filterable<ExecutionNode>) :
  FilterToggleAction("Show Warnings", STATE_KEY, filterable, WARNINGS_FILTER, true), DumbAware {
  companion object {
    private const val STATE_KEY = "build.toolwindow.show.warnings.selection.state"
    fun install(filterable: Filterable<ExecutionNode>) {
      install(filterable, WARNINGS_FILTER, STATE_KEY, true)
    }
  }
}
