// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.intellij.diagnostic.logging.LogConsoleBase
import com.intellij.diagnostic.logging.LogFilter
import com.intellij.diagnostic.logging.LogFilterListener
import com.intellij.diagnostic.logging.LogFilterModel
import com.intellij.execution.process.ProcessOutputType
import com.intellij.internal.statistic.actions.StatisticsEventLogToolWindow.Companion.rejectedValidationTypes
import com.intellij.internal.statistic.eventLog.EventLogNotificationService
import com.intellij.internal.statistic.eventLog.LogEvent
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.FilterComponent
import com.intellij.ui.JBColor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.text.DateFormatUtil
import java.awt.BorderLayout
import javax.swing.BorderFactory
import javax.swing.JPanel

const val eventLogToolWindowsId = "Statistics Event Log"

class StatisticsEventLogToolWindow(project: Project, private val recorderId: String) : SimpleToolWindowPanel(false, true), Disposable {
  private val consoleLog = StatisticsEventLogConsole(project, StatisticsLogFilterModel())
  private val eventLogListener: (LogEvent) -> Unit = { logEvent -> consoleLog.addLogLine(buildLogMessage(logEvent)) }

  init {
    val topPanel = JPanel(BorderLayout())
    topPanel.add(consoleLog.filter, BorderLayout.EAST)
    topPanel.border = BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border())
    add(topPanel, BorderLayout.NORTH)

    setContent(consoleLog.component)

    val toolbarActions = DefaultActionGroup()
    toolbarActions.add(consoleLog.orCreateActions)
    toolbarActions.add(ConfigureWhitelistAction())
    toolbarActions.add(UpdateWhitelistAction(recorderId))
    toolbar = ActionManager.getInstance().createActionToolbar("FusEventLogToolWindow", toolbarActions, false).component

    Disposer.register(this, consoleLog)
    EventLogNotificationService.subscribe(eventLogListener, recorderId)
  }

  override fun dispose() {
    EventLogNotificationService.unsubscribe(eventLogListener, recorderId)
  }

  companion object {
    private val systemFields = setOf("last", "created")
    private const val projectIdPrefixSize = 8
    private const val projectIdSuffixSize = 2
    private const val maxProjectIdSize = projectIdPrefixSize + projectIdSuffixSize
    val rejectedValidationTypes = setOf(REJECTED, INCORRECT_RULE, UNDEFINED_RULE, UNREACHABLE_WHITELIST, PERFORMANCE_ISSUE)

    fun buildLogMessage(logEvent: LogEvent): String {
      return buildString {
        append(DateFormatUtil.formatTimeWithSeconds(logEvent.time))
        append(" - ['${logEvent.group.id}', v${logEvent.group.version}]: '${logEvent.event.id}' ")
        append("{")
        append(buildEventDataMessages(logEvent))
        append("}")
      }
    }

    private fun buildEventDataMessages(logEvent: LogEvent): String {
      return logEvent.event.data
        .filter { (key, _) -> !systemFields.contains(key) }
        .map { (key, value) ->
          var valueAsString = value.toString()
          if (key == "project") {
            valueAsString = shortenProjectId(valueAsString)
          }
          "\"$key\":\"$valueAsString\""
        }
        .joinToString(", ")
    }

    private fun shortenProjectId(projectId: String): String {
      val length = projectId.length
      val isRejected = rejectedValidationTypes.any { it.description == projectId }
      if (!isRejected && projectId.isNotBlank() && length > maxProjectIdSize) {
        return "${projectId.substring(0, projectIdPrefixSize)}...${projectId.substring(length - projectIdSuffixSize, length)}"
      }
      else {
        return projectId
      }
    }

  }
}

private class StatisticsEventLogConsole(val project: Project,
                                        val model: LogFilterModel) : LogConsoleBase(project, null, eventLogToolWindowsId, false, model) {
  val filter = object : FilterComponent("STATISTICS_EVENT_LOG_FILTER_HISTORY", 5) {
    override fun filter() {
      val task = object : Task.Backgroundable(project, APPLYING_FILTER_TITLE) {
        override fun run(indicator: ProgressIndicator) {
          model.updateCustomFilter(filter)
        }
      }
      ProgressManager.getInstance().run(task)
    }
  }

  override fun isActive(): Boolean {
    return ToolWindowManager.getInstance(project).getToolWindow(eventLogToolWindowsId).isVisible
  }

  fun addLogLine(line: String) {
    super.addMessage(line)
  }
}

class StatisticsLogFilterModel : LogFilterModel() {
  private val listeners = ContainerUtil.createLockFreeCopyOnWriteList<LogFilterListener>()
  private var customFilter: String? = null


  override fun getCustomFilter(): String? = customFilter

  override fun addFilterListener(listener: LogFilterListener?) {
    listeners.add(listener)
  }

  override fun removeFilterListener(listener: LogFilterListener?) {
    listeners.remove(listener)
  }

  override fun getLogFilters(): List<LogFilter> = emptyList()

  override fun isFilterSelected(filter: LogFilter?): Boolean = false

  override fun selectFilter(filter: LogFilter?) {}

  override fun updateCustomFilter(filter: String) {
    super.updateCustomFilter(filter)
    customFilter = filter
    for (listener in listeners) {
      listener.onTextFilterChange()
    }
  }

  override fun processLine(line: String): MyProcessingResult {
    val contentType = defineContentType(line)
    val applicable = isApplicable(line)
    return MyProcessingResult(contentType, applicable, null)
  }

  private fun defineContentType(line: String): ProcessOutputType {
    return when {
      rejectedValidationTypes.any { line.contains(it.description) } -> ProcessOutputType.STDERR
      else -> ProcessOutputType.STDOUT
    }
  }

}