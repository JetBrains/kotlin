// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.execution.ui.ConsoleViewContentType.ERROR_OUTPUT
import com.intellij.execution.ui.ConsoleViewContentType.NORMAL_OUTPUT
import com.intellij.internal.statistic.eventLog.EventLogNotificationService
import com.intellij.internal.statistic.eventLog.LogEvent
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.util.text.DateFormatUtil

const val eventLogToolWindowsId = "FUS Event Log"

class StatisticsEventLogToolWindow(myProject: Project) : SimpleToolWindowPanel(false, true), Disposable {
  private val consoleView = ConsoleViewImpl(myProject, true)
  private val eventLogListener: (LogEvent) -> Unit = { logEvent ->
    buildLogMessage(logEvent).forEach { (text, contentType) -> consoleView.print(text, contentType) }
  }

  init {
    setContent(consoleView.component)
    Disposer.register(this, consoleView)

    val actionGroup = DefaultActionGroup(*consoleView.createConsoleActions())
    toolbar = ActionManager.getInstance().createActionToolbar("FusEventLogToolWindow", actionGroup, false).component
    EventLogNotificationService.subscribe(eventLogListener)
  }

  override fun dispose() {
    EventLogNotificationService.unsubscribe(eventLogListener)
  }

  companion object {
    private val systemFields = setOf("last", "created")
    private const val projectIdPrefixSize = 8
    private const val projectIdSuffixSize = 2
    private const val maxProjectIdSize = projectIdPrefixSize + projectIdSuffixSize
    val incorrectValidationTypes = setOf(REJECTED, INCORRECT_RULE, UNDEFINED_RULE, UNREACHABLE_WHITELIST, PERFORMANCE_ISSUE)
    private val incorrectDescriptions = incorrectValidationTypes.map { it.description }

    fun buildLogMessage(logEvent: LogEvent): List<Pair<String, ConsoleViewContentType>> {
      val messages = arrayListOf<Pair<String, ConsoleViewContentType>>()
      messages.add(
        "${DateFormatUtil.formatTimeWithSeconds(logEvent.time)} - ['${logEvent.group.id}', v${logEvent.group.version}]: " to NORMAL_OUTPUT)
      messages.add("'${logEvent.event.id}' " to defineContentType(logEvent.event.id))
      messages.add("{" to NORMAL_OUTPUT)
      messages.addAll(buildEventDataMessages(logEvent))
      messages.add("}\n" to NORMAL_OUTPUT)
      return messages
    }

    private fun buildEventDataMessages(logEvent: LogEvent): List<Pair<String, ConsoleViewContentType>> {
      val eventDataMessages = arrayListOf<Pair<String, ConsoleViewContentType>>()
      var count = 0
      for ((key, value) in logEvent.event.data) {
        if (systemFields.contains(key)) continue
        var valueAsString = value.toString()
        if (++count > 1) eventDataMessages.add(", " to NORMAL_OUTPUT)
        val contentType = defineContentType(valueAsString)
        if (contentType != ERROR_OUTPUT && key == "project") {
          valueAsString = shortenProjectId(valueAsString)
        }
        eventDataMessages.add("\"$key\":\"$valueAsString\"" to contentType)
      }
      return eventDataMessages
    }

    private fun shortenProjectId(projectId: String): String {
      val length = projectId.length
      if (projectId.isNotBlank() && length > maxProjectIdSize) {
        return "${projectId.substring(0, projectIdPrefixSize)}...${projectId.substring(length - projectIdSuffixSize, length)}"
      }
      else {
        return projectId
      }
    }

    private fun defineContentType(value: String): ConsoleViewContentType =
      if (incorrectDescriptions.contains(value)) ERROR_OUTPUT else NORMAL_OUTPUT
  }
}
