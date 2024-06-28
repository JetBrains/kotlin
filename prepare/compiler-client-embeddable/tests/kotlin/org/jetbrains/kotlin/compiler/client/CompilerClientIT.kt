/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.compiler.client

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.daemon.client.DaemonReportMessage
import org.jetbrains.kotlin.daemon.client.DaemonReportingTargets
import org.jetbrains.kotlin.daemon.client.KotlinCompilerClient
import org.jetbrains.kotlin.daemon.common.CompileService
import org.jetbrains.kotlin.daemon.common.CompilerId
import org.jetbrains.kotlin.daemon.common.DaemonOptions
import org.jetbrains.kotlin.daemon.common.ReportSeverity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.PrintStream
import java.nio.file.Files
import kotlin.test.assertEquals


class CompilerClientIT {

    @JvmField
    @Rule
    val workingDir = TemporaryFolder()

    private val compilerClasspath: List<File> by lazy {
        filesFromProp("compilerClasspath", "kotlin-compiler.jar", "kotlin-daemon.jar")
    }

    private val compilationClasspath: List<File> by lazy {
        filesFromProp("compilationClasspath", "kotlin-stdlib.jar", "kotlin-script-runtime.jar")
    }

    private val clientAliveFile by lazy {
        Files.createTempFile("client", ".alive").toFile().apply {
            deleteOnExit()
        }
    }

    private fun filesFromProp(propName: String, vararg defaultPaths: String): List<File> =
        (System.getProperty(propName)?.split(File.pathSeparator) ?: defaultPaths.asList()).map {
            File(it).takeIf(File::exists)
                ?: throw FileNotFoundException("cannot find ($it)")
        }

    private val compilerService: CompileService by lazy {
        val compilerId = CompilerId.makeCompilerId(compilerClasspath)
        val daemonOptions = DaemonOptions(runFilesPath = File(workingDir.root, "daemonRunPath").absolutePath, verbose = true, reportPerf = true)
        val daemonJVMOptions = org.jetbrains.kotlin.daemon.common.DaemonJVMOptions()
        val daemonReportMessages = arrayListOf<DaemonReportMessage>()

        KotlinCompilerClient.connectToCompileService(compilerId, clientAliveFile, daemonJVMOptions, daemonOptions,
                DaemonReportingTargets(messages = daemonReportMessages), true)
                ?: throw IllegalStateException("Unable to connect to compiler daemon:" + daemonReportMessages.joinToString("\n  ", prefix = "\n  ") { "${it.category.name} ${it.message}" })
    }

    private val myMessageCollector = TestMessageCollector()

    @Test
    fun testSimpleScript() {
        val (out, code) = runCompiler(
                "-cp", compilationClasspath.joinToString(File.pathSeparator) { it.canonicalPath },
                "-Xuse-fir-lt=false", "-Xallow-any-scripts-in-source-roots",
                File("testData/scripts/simpleHelloWorld.kts").canonicalPath)
        assertEquals(0, code, "compilation failed:\n" + out + "\n")
    }

    private fun runCompiler(vararg args: String): Pair<String, Int> {

        var code = -1
        myMessageCollector.clear()
        val out = captureOutAndErr {
            code = KotlinCompilerClient.compile(compilerService, CompileService.NO_SESSION, CompileService.TargetPlatform.JVM, args, myMessageCollector,
                    reportSeverity = ReportSeverity.DEBUG)
        }
        return myMessageCollector.messages.joinToString("\n") { it.message } + "\n" + out to code
    }
}

internal fun captureOutAndErr(body: () -> Unit): String {
    val outStream = ByteArrayOutputStream()
    val prevOut = System.out
    val prevErr = System.err
    System.setOut(PrintStream(outStream))
    System.setErr(PrintStream(outStream))
    try {
        body()
    }
    finally {
        System.out.flush()
        System.setOut(prevOut)
        System.err.flush()
        System.setErr(prevErr)
    }
    return outStream.toString()
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
}
