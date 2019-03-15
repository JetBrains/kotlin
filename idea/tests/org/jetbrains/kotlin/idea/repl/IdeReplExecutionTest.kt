/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.repl

import com.intellij.execution.console.LanguageConsoleImpl
import com.intellij.testFramework.PlatformTestCase
import com.intellij.util.ui.UIUtil
import org.jetbrains.kotlin.console.KotlinConsoleKeeper
import org.jetbrains.kotlin.console.KotlinConsoleRunner
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.junit.Test
import kotlin.reflect.KMutableProperty0
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IdeReplExecutionTest : PlatformTestCase() {
    private lateinit var consoleRunner: KotlinConsoleRunner
    private var commandsSent = 0

    override fun setUp() {
        super.setUp()
        consoleRunner = KotlinConsoleKeeper.getInstance(project).run(module)!!
    }

    override fun tearDown() {
        consoleRunner.dispose()
        (this::consoleRunner as KMutableProperty0<KotlinConsoleRunner?>).set(null)
        super.tearDown()
    }

    private fun sendCommand(command: String) {
        runWriteAction { consoleRunner.consoleView.editorDocument.setText(command) }
        consoleRunner.executor.executeCommand()
        commandsSent++
    }

    private fun checkOutput(expectedOutput: String) {
        val output = getReplOutput(textOnTimeOut = { "Only ${consoleRunner.commandHistory.processedEntriesCount} commands were processed" }) {
            consoleRunner.commandHistory.processedEntriesCount >= commandsSent
        }
        assertTrue(output.trim().endsWith(expectedOutput), "'$expectedOutput' should be printed but document text is:\n$output")
    }

    private fun getReplOutput(maxIterations: Int = 50, sleepTime: Long = 500, textOnTimeOut: () -> String, predicate: () -> Boolean): String {
        for (i in 1..maxIterations) {
            UIUtil.dispatchAllInvocationEvents()
            if (predicate()) {
                return refreshAndGetHistoryEditorText()
            }
            Thread.sleep(sleepTime)
        }

        return textOnTimeOut()
    }

    private fun refreshAndGetHistoryEditorText(): String {
        val consoleView = consoleRunner.consoleView as LanguageConsoleImpl
        consoleView.flushDeferredText()

        return consoleView.historyViewer.document.text
    }

    private fun testSimpleCommand(command: String, expectedOutput: String) {
        sendCommand(command)
        checkOutput(expectedOutput)
    }

    @Test fun testRunPossibility() {
        val allOk = { x: String -> x.contains(":help for help") }
        val hasErrors = { x: String -> x.contains("Process finished with exit code 1") || x.contains("Exception in") || x.contains("Error") }

        val output = getReplOutput(textOnTimeOut = { "Repl startup timed out" }) {
            val editorText = refreshAndGetHistoryEditorText()
            hasErrors(editorText) || allOk(editorText)
        }

        assertFalse(hasErrors(output), "Cannot run kotlin repl")
        assertTrue(allOk(output), "Successful run should contain text: ':help for help'")
        assertFalse(consoleRunner.processHandler.isProcessTerminated, "Process accidentally terminated")
    }

    @Test fun testOnePlusOne() = testSimpleCommand("1 + 1", "2")
    @Test fun testPrintlnText() = "Hello, console world!".let { testSimpleCommand("println(\"$it\")", it) }
    @Test fun testDivisionByZeroException() = testSimpleCommand("1 / 0", "java.lang.ArithmeticException: / by zero")

    @Test fun testMultilineSupport() {
        val printText = "Print in multiline!"

        sendCommand("fun f() {\n" +
                    "    println(\"$printText\")\n" +
                    "}\n")
        sendCommand("f()")

        checkOutput(printText)
    }

    @Test fun testReadLineSingle() {
        val readLineText = "ReadMe!"

        sendCommand("val a = readLine()")
        sendCommand(readLineText)
        sendCommand("a")
        checkOutput(readLineText)
    }

    @Test fun testReadLineMultiple() {
        val readLineTextA = "ReadMe A!"
        val readLineTextB = "ReadMe B!"

        sendCommand("val a = readLine()\n" +
                    "val b = readLine()")
        sendCommand(readLineTextA)
        sendCommand(readLineTextB)

        sendCommand("a")
        checkOutput(readLineTextA)
        sendCommand("b")
        checkOutput(readLineTextB)
    }

    @Test fun testCorrectAfterError() {
        val message = "MyMessage"
        sendCommand("fun f() { println(x)\n println(y) ")
        sendCommand("println(\"$message\")")
        checkOutput(message)
    }

    @Test fun testMultipleErrorsHandling() {
        val veryLongTextWithErrors = "println($);".repeat(30)
        sendCommand(veryLongTextWithErrors)
        sendCommand(veryLongTextWithErrors)
        sendCommand(veryLongTextWithErrors)
        sendCommand("println(\"OK\")")
        checkOutput("OK")
    }
}