/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.internal.testing

import com.microsoft.playwright.Playwright
import org.gradle.api.internal.tasks.testing.TestExecuter
import org.gradle.api.internal.tasks.testing.TestExecutionSpec
import org.gradle.api.internal.tasks.testing.TestResultProcessor
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("org.jetbrains.kotlin.gradle.tasks.testing.PlaywrightTestExecutor")

internal class PwExecutionSpec(
    val createClient: (TestResultProcessor, org.slf4j.Logger) -> TCServiceMessagesClient,
    val url: String
) : TestExecutionSpec

internal class PlaywrightTestExecutor : TestExecuter<PwExecutionSpec> {
    private val playwright = Playwright.create()

    override fun execute(spec: PwExecutionSpec, testResultProcessor: TestResultProcessor) {
        val client = spec.createClient(testResultProcessor, log)

        val handler = TCServiceMessageOutputStreamHandler(
            client,
            { },
            log,
            false,
        )

        client.root {
            val browser = playwright.chromium().launch()
            browser.use {
                val page = browser.newPage()
                var finished = false
                page.onConsoleMessage {
                    if (it.text().startsWith("THE END")) {
                        finished = true
                    } else {
                        handler.write(it.text().toByteArray())
                        handler.writeEndLine()
                    }
                }
                page.navigate(spec.url)
                page.waitForCondition { finished }
            }
        }
    }

    override fun stopNow() {
        playwright.close()
    }
}
