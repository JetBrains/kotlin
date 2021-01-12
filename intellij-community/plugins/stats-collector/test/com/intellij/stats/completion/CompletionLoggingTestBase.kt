// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.stats.completion.events.CompletionStartedEvent
import com.intellij.stats.completion.events.LogEvent
import com.intellij.testFramework.replaceService
import org.assertj.core.api.Assertions
import org.mockito.Mockito

const val runnableInterface = "interface Runnable { void run(); void runFast(); }"
const val testText = """
class Test {
    public void run() {
        Runnable r = new Runnable() {
            public void run() {}
        };
        r<caret>
    }
}
"""

fun List<LogEvent>.assertOrder(vararg actions: Action) {
  val completionEvents = filter { it.actionType != Action.CUSTOM }
  Assertions.assertThat(completionEvents.size).isEqualTo(actions.size)
  completionEvents.zip(actions).forEach { (event, action) ->
    Assertions.assertThat(event.actionType).isEqualTo(action)
  }
}

abstract class CompletionLoggingTestBase : LightFixtureCompletionTestCase() {
  val trackedEvents = mutableListOf<LogEvent>()

  private lateinit var mockLoggerProvider: CompletionLoggerProvider

  val completionStartedEvent: CompletionStartedEvent
    get() = trackedEvents.first() as CompletionStartedEvent

  open fun completionFileLogger(): CompletionFileLogger {
    val eventLogger = object : CompletionEventLogger {
      override fun log(event: LogEvent) {
        trackedEvents.add(event)
      }
    }
    return CompletionFileLogger("installation-uid", "completion-uid", "0", eventLogger)
  }

  override fun setUp() {
    super.setUp()

    trackedEvents.clear()

    mockLoggerProvider = Mockito.mock(CompletionLoggerProvider::class.java)
    Mockito.`when`(mockLoggerProvider.newCompletionLogger()).thenReturn(completionFileLogger())
    ApplicationManager.getApplication().replaceService(CompletionLoggerProvider::class.java, mockLoggerProvider, testRootDisposable)

    myFixture.addClass(runnableInterface)
    myFixture.configureByText(JavaFileType.INSTANCE, testText)

    CompletionTrackerInitializer.isEnabledInTests = true
  }

  override fun tearDown() {
    CompletionTrackerInitializer.isEnabledInTests = false

    super.tearDown()
  }
}

