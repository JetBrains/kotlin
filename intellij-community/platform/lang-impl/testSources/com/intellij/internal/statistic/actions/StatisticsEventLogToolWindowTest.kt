// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.actions

import com.intellij.internal.statistic.actions.StatisticsEventLogToolWindow.Companion.buildLogMessage
import com.intellij.internal.statistic.eventLog.LogEvent
import com.intellij.internal.statistic.eventLog.LogEventAction
import org.junit.Assert.assertEquals
import org.junit.Test

class StatisticsEventLogToolWindowTest {
    @Test
    fun testBuildLogMessage() {
        val action = LogEventAction("third.party")
        action.addData("project", "5410c65eafb1f0abd78c6d9bdf33752f13c17b17ed57c3ae26801ae6ee7d17ea")
        action.addData("last", "1564643442610")
        action.addData("plugin_type", "PLATFORM")
        val expected = "01.08.2019 14:05:14 - ['toolwindow', v21]: 'third.party' {plugin_type='PLATFORM', project='5410c65e...ea'}\r\n"
        assertEquals(expected, buildLogMessage(buildLogEvent(action)))
    }

    @Test
    fun testBuildLogMessageWithShortProjectName() {
        val action = LogEventAction("third.party")
        val projectId = "12345"
        action.addData("project", projectId)
        val expected = "01.08.2019 14:05:14 - ['toolwindow', v21]: 'third.party' {project='$projectId'}\r\n"
        assertEquals(expected, buildLogMessage(buildLogEvent(action)))
    }

    private fun buildLogEvent(action: LogEventAction) =
            LogEvent("2e5b2e32e061", "193.1801", "176",
                    1564643114456, "toolwindow", "21", "32", action)
}