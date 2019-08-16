// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.intellij.execution.ui.ConsoleViewContentType
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
    assertTrue("Not all messages have NORMAL_OUTPUT type", actual.all { it.second == ConsoleViewContentType.NORMAL_OUTPUT })
    assertEquals("${DateFormatUtil.formatTimeWithSeconds(eventTime)} - ['toolwindow', v21]: '$eventId' {\"plugin_type\":\"PLATFORM\", \"project\":\"5410c65e...ea\"}\n",
                 actual.joinToString("") { it.first })
  }

  @Test
  fun testNotShortenProjectId() {
    val action = LogEventAction(eventId)
    val projectId = "12345"
    action.addData("project", projectId)

    val actual = buildLogMessage(buildLogEvent(action))
    assertTrue("Not all messages have NORMAL_OUTPUT type", actual.all { it.second == ConsoleViewContentType.NORMAL_OUTPUT })
    assertEquals("${DateFormatUtil.formatTimeWithSeconds(eventTime)} - ['toolwindow', v21]: '$eventId' {\"project\":\"$projectId\"}\n",
                 actual.joinToString("") { it.first })
  }

  @Test
  fun testFilterSystemFields() {
    val action = LogEventAction(eventId)
    action.addData("last", "1564643442610")
    action.addData("created", "1564643442610")

    val actual = buildLogMessage(buildLogEvent(action))
    assertTrue("Not all messages have NORMAL_OUTPUT type", actual.all { it.second == ConsoleViewContentType.NORMAL_OUTPUT })
    assertEquals("${DateFormatUtil.formatTimeWithSeconds(eventTime)} - ['toolwindow', v21]: '$eventId' {}\n",
                 actual.joinToString("") { it.first })
  }

  @Test
  fun testLogIncorrectEventIdAsError() {
    val incorrectEventId = INCORRECT_RULE.description
    val action = LogEventAction(incorrectEventId)

    val actual = buildLogMessage(buildLogEvent(action))
    assertEquals(4, actual.size)
    assertEquals(ConsoleViewContentType.ERROR_OUTPUT, actual[1].second)
  }

  @Test
  fun testLogIncorrectEventDataAsError() {
    val action = LogEventAction(eventId)
    action.addData("test", INCORRECT_RULE.description)
    action.addData("project", UNDEFINED_RULE.description)

    val actual = buildLogMessage(buildLogEvent(action))
    assertEquals(7, actual.size)
    assertEquals("\"test\":\"validation.incorrect_rule\"", actual[3].first)
    assertEquals(ConsoleViewContentType.ERROR_OUTPUT, actual[3].second)
    assertEquals("\"project\":\"validation.undefined_rule\"", actual[5].first)
    assertEquals(ConsoleViewContentType.ERROR_OUTPUT, actual[5].second)
  }

  @Test
  fun testAllValidationTypesUsed() {
    val correctValidationTypes = setOf(ACCEPTED, THIRD_PARTY)
    for (resultType in values()) {
      assert(StatisticsEventLogToolWindow.incorrectValidationTypes.contains(resultType) || correctValidationTypes.contains(resultType))
    }
  }

  private fun buildLogEvent(action: LogEventAction) = LogEvent("2e5b2e32e061", "193.1801", "176", eventTime,
                                                               "toolwindow", "21", "32", action)
}