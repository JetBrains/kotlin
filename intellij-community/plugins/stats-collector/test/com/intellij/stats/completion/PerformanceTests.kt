// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.completion

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.stats.network.service.RequestService
import com.intellij.stats.network.service.ResponseData
import com.intellij.stats.sender.StatisticSenderImpl
import com.intellij.stats.storage.FilePathProvider
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.replaceService
import org.mockito.Mockito.*
import org.picocontainer.MutablePicoContainer
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class PerformanceTests : LightFixtureCompletionTestCase() {
    private lateinit var pathProvider: FilePathProvider

    private val runnable = "interface Runnable { void run();  void notify(); void wait(); void notifyAll(); }"
    private val text = """
class Test {
    public void run() {
        Runnable r = new Runnable() {
            public void run() {}
        };
        r<caret>
    }
}
"""

    override fun setUp() {
        super.setUp()
        val container = ApplicationManager.getApplication().picoContainer as MutablePicoContainer
        pathProvider = container.getComponentInstance(FilePathProvider::class.java.name) as FilePathProvider
        CompletionTrackerInitializer.isEnabledInTests = true
    }

    override fun tearDown() {
        CompletionTrackerInitializer.isEnabledInTests = false
        try {
            super.tearDown()
        } finally {
            CompletionLoggerProvider.getInstance().dispose()
            val statsDir = pathProvider.getStatsDataDirectory()
            statsDir.deleteRecursively()
        }
    }

    fun `test do not block EDT on data send`() {
        myFixture.configureByText("Test.java", text)
        myFixture.addClass(runnable)

        val requestService = slowRequestService()

        val file = pathProvider.getUniqueFile()
        file.writeText("Some existing data to send")

        val app = ApplicationManager.getApplication()
        app.replaceService(FilePathProvider::class.java, pathProvider, testRootDisposable)
        app.replaceService(RequestService::class.java, requestService, testRootDisposable)

        val sender = StatisticSenderImpl()

        val isSendFinished = AtomicBoolean(false)

        val lock = Object()
        app.executeOnPooledThread {
            synchronized(lock, { lock.notify() })
            sender.sendStatsData("")
            isSendFinished.set(true)
        }
        synchronized(lock, { lock.wait() })

        myFixture.type('.')
        myFixture.completeBasic()
        myFixture.type("xx")

        UsefulTestCase.assertFalse(isSendFinished.get())
    }

    private fun slowRequestService(): RequestService {
        return mock(RequestService::class.java).apply {
            `when`(postZipped(anyString(), any() ?: File("."))).then {
                Thread.sleep(10000)
                ResponseData(200)
            }
        }
    }

}
