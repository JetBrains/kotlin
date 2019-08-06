// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.intellij.execution.impl.ConsoleViewImpl
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.internal.statistic.eventLog.EventLogNotificationService
import com.intellij.internal.statistic.eventLog.LogEvent
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.util.Disposer
import com.intellij.util.text.DateFormatUtil

const val eventLogToolWindowsId = "FUS Event Log"

class StatisticsEventLogToolWindow(myProject: Project) : SimpleToolWindowPanel(false, true), Disposable{
  private val consoleView = ConsoleViewImpl(myProject, true)
  private val eventLogListener: (LogEvent) -> Unit = { consoleView.print(buildLogMessage(it), ConsoleViewContentType.NORMAL_OUTPUT) }

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

    fun buildLogMessage(logEvent: LogEvent): String {
      return buildString {
        append("${DateFormatUtil.formatDate(logEvent.time)} ${DateFormatUtil.formatTimeWithSeconds(logEvent.time)}")
        append(" - ")
        append("['${logEvent.group.id}', v${logEvent.group.version}]: '${logEvent.event.id}' ")

        append("{")
        append(logEvent.event.data
                 .filter { (key, _) -> !systemFields.contains(key) }
                 .map { (key, value) ->
                   val valueAsString = value.toString()
                   val length = valueAsString.length
                   if (key == "project" && valueAsString.isNotBlank() && length > maxProjectIdSize) {
                     "$key='${valueAsString.substring(0, projectIdPrefixSize)}...${valueAsString.substring(length - projectIdSuffixSize, length)}'"
                   }
                   else {
                     "$key='$valueAsString'"
                   }
                 }
                 .joinToString(", "))
        appendln("}")
      }
    }

  }
}
