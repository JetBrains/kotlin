// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.intellij.execution.process.ProcessOutputType
import com.intellij.internal.statistic.actions.StatisticsEventLogToolWindow.Companion.buildLogMessage
import com.intellij.internal.statistic.eventLog.LogEvent
import com.intellij.internal.statistic.eventLog.LogEventAction
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType.*
import com.intellij.util.text.DateFormatUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsEventLogToolWindowTest {
  private val eventId = "third.party"
  private val eventTime = 1564643114456

  @Test
  fun testShortenProjectId() {
    val action = LogEventAction(eventId)
    action.addData("project", "5410c65eafb1f0abd78c6d9bdf33752f13c17b17ed57c3ae26801ae6ee7d17ea")
    action.addData("plugin_type", "PLATFORM")

    val actual = buildLogMessage(buildLogEvent(action))
    assertEquals("${DateFormatUtil.formatTimeWithSeconds(
      eventTime)} - ['toolwindow', v21]: '$eventId' {\"plugin_type\":\"PLATFORM\", \"project\":\"5410c65e...ea\"}",
                 actual)
  }

  @Test
  fun testNotShortenProjectId() {
    val action = LogEventAction(eventId)
    val projectId = "12345"
    action.addData("project", projectId)

    val actual = buildLogMessage(buildLogEvent(action))
    assertEquals("${DateFormatUtil.formatTimeWithSeconds(eventTime)} - ['toolwindow', v21]: '$eventId' {\"project\":\"$projectId\"}",
                 actual)
  }

  @Test
  fun testFilterSystemFields() {
    val action = LogEventAction(eventId)
    action.addData("last", "1564643442610")
    action.addData("created", "1564643442610")

    val actual = buildLogMessage(buildLogEvent(action))
    assertEquals("${DateFormatUtil.formatTimeWithSeconds(eventTime)} - ['toolwindow', v21]: '$eventId' {}",
                 actual)
  }

  @Test
  fun testLogIncorrectEventIdAsError() {
    val incorrectEventId = INCORRECT_RULE.description
    val action = LogEventAction(incorrectEventId)

    val filterModel = StatisticsLogFilterModel()
    val processingResult = filterModel.processLine(buildLogMessage(buildLogEvent(action)))
    assertEquals(processingResult.key, ProcessOutputType.STDERR)
  }

  @Test
  fun testLogIncorrectEventDataAsError() {
    val action = LogEventAction(eventId)
    action.addData("test", INCORRECT_RULE.description)
    action.addData("project", UNDEFINED_RULE.description)

    val filterModel = StatisticsLogFilterModel()
    val processingResult = filterModel.processLine(buildLogMessage(buildLogEvent(action)))
    assertEquals(processingResult.key, ProcessOutputType.STDERR)
  }

  @Test
  fun testAllValidationTypesUsed() {
    val correctValidationTypes = setOf(ACCEPTED, THIRD_PARTY)
    for (resultType in values()) {
      assertTrue("Don't forget to change toolWindow logic in case of a new value in ValidationResult",
                 StatisticsEventLogToolWindow.rejectedValidationTypes.contains(resultType) || correctValidationTypes.contains(resultType))
    }
  }

  private fun buildLogEvent(action: LogEventAction) = LogEvent("2e5b2e32e061", "193.1801", "176", eventTime,
                                                               "toolwindow", "21", "32", action)
}