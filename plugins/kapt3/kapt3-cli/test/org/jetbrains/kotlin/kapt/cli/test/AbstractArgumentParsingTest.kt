/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.kapt.cli.test

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.kapt.cli.transformArgs
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.junit.runner.RunWith
import java.io.File

private val LINE_SEPARATOR: String = System.getProperty("line.separator")

@RunWith(JUnit3RunnerWithInners::class)
abstract class AbstractArgumentParsingTest : TestCase() {
    fun doTest(filePath: String) {
        val testFile = File(filePath)

        val sections = Section.parse(testFile)
        val before = sections.single { it.name == "before" }

        val messageCollector = TestMessageCollector()
        val transformedArgs = transformArgs(before.content.lines(), messageCollector, isTest = true)
        val actualAfter = if (messageCollector.hasErrors()) messageCollector.toString() else transformedArgs.joinToString(LINE_SEPARATOR)
        val actual = sections.replacingSection("after", actualAfter).render()

        KotlinTestUtils.assertEqualsToFile(testFile, actual)
    }
}

class TestMessageCollector : MessageCollector {
    data class Message(val severity: CompilerMessageSeverity, val message: String, val location: CompilerMessageSourceLocation?)

    val messages = arrayListOf<Message>()

    override fun clear() {
        messages.clear()
    }

    override fun report(severity: CompilerMessageSeverity, message: String, location: CompilerMessageSourceLocation?) {
        messages.add(Message(severity, message, location))
    }

    override fun hasErrors(): Boolean = messages.any { it.severity == CompilerMessageSeverity.EXCEPTION || it.severity == CompilerMessageSeverity.ERROR }

    override fun toString(): String {
        return messages.joinToString("\n") { "${it.severity}: ${it.message}${it.location?.let{" at $it"} ?: ""}" }
    }
}
