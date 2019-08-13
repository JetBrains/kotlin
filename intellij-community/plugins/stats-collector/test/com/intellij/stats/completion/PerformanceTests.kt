/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.stats.completion

import com.intellij.codeInsight.completion.LightFixtureCompletionTestCase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.stats.network.service.RequestService
import com.intellij.stats.network.service.ResponseData
import com.intellij.stats.sender.StatisticSenderImpl
import com.intellij.stats.storage.FilePathProvider
import com.intellij.testFramework.UsefulTestCase
import org.mockito.ArgumentMatchers
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
        
        val sender = StatisticSenderImpl(requestService, pathProvider)

        val isSendFinished = AtomicBoolean(false)

        val lock = Object()
        ApplicationManager.getApplication().executeOnPooledThread {
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
