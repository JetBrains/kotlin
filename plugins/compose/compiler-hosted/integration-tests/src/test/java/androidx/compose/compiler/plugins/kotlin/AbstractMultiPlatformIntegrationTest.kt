/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin

import com.intellij.openapi.util.io.FileUtil
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.common.CLICompiler
import org.jetbrains.kotlin.cli.common.CLITool
import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.util.TraceClassVisitor
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter

// AbstractCliTest
private fun executeCompilerGrabOutput(
    compiler: CLITool<*>,
    args: List<String>
): Pair<String, ExitCode> {
    val output = StringBuilder()

    var index = 0
    do {
        var next = args.subList(index, args.size).indexOf("---")
        if (next == -1) {
            next = args.size
        }
        val (first, second) = executeCompiler(compiler, args.subList(index, next))
        output.append(first)
        if (second != ExitCode.OK) {
            return Pair(output.toString(), second)
        }
        index = next + 1
    } while (index < args.size)

    return Pair(output.toString(), ExitCode.OK)
}
// CompilerTestUtil
private fun executeCompiler(compiler: CLITool<*>, args: List<String>): Pair<String, ExitCode> {
    val bytes = ByteArrayOutputStream()
    val origErr = System.err
    try {
        System.setErr(PrintStream(bytes))
        val exitCode = CLITool.doMainNoExit(compiler, args.toTypedArray())
        return Pair(String(bytes.toByteArray()), exitCode)
    } finally {
        System.setErr(origErr)
    }
}
// jetTestUtils
fun String.trimTrailingWhitespaces(): String =
    this.split('\n').joinToString(separator = "\n") { it.trimEnd() }
// jetTestUtils
fun String.trimTrailingWhitespacesAndAddNewlineAtEOF(): String =
    this.trimTrailingWhitespaces().let {
        result ->
        if (result.endsWith("\n")) result else result + "\n"
    }

abstract class AbstractMultiPlatformIntegrationTest : AbstractCompilerTest() {
    fun multiplatform(
        @Language("kotlin")
        common: String,
        @Language("kotlin")
        jvm: String,
        output: String
    ) {
        setUp()
        val tmpdir = tmpDir(getTestName(true))

        assert(
            composePluginJar.exists(),
            { "Compiler plugin jar does not exist: $composePluginJar" }
        )

        val optionalArgs = arrayOf(
            "-cp",
            defaultClassPath
                .filter { it.exists() }
                .joinToString(File.pathSeparator) { it.absolutePath },
            "-kotlin-home",
            AbstractCompilerTest.kotlinHome.absolutePath,
            "-Xplugin=${composePluginJar.absolutePath}",
            "-Xuse-ir"
        )

        val jvmOnlyArgs = arrayOf("-no-stdlib")

        val srcDir = File(tmpdir, "srcs").absolutePath
        val commonSrc = File(srcDir, "common.kt")
        val jvmSrc = File(srcDir, "jvm.kt")

        FileUtil.writeToFile(commonSrc, common)
        FileUtil.writeToFile(jvmSrc, jvm)

        val jvmDest = File(tmpdir, "jvm").absolutePath

        val result = K2JVMCompiler().compile(
            jvmSrc,
            commonSrc,
            "-d", jvmDest,
            *optionalArgs,
            *jvmOnlyArgs
        )

        val files = File(jvmDest).listFiles()

        if (files == null || files.isEmpty()) {
            assertEquals(output.trimIndent(), result)
            return
        }

        val sb = StringBuilder()

        files
            .filter { it.extension == "class" }
            .sortedBy { it.absolutePath }
            .distinctBy { it.name }
            .forEach {
                val os = ByteArrayOutputStream()
                val printWriter = PrintWriter(os)
                val writer = TraceClassVisitor(printWriter)
                val reader = ClassReader(it.inputStream().readBytes())
                reader.accept(
                    writer,
                    ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG or ClassReader.SKIP_FRAMES
                )
                sb.append(os.toString())
                sb.appendLine()
            }

        assertEquals(output.trimIndent(), printPublicApi(sb.toString(), "test"))
    }

    private fun CLICompiler<*>.compile(
        sources: File,
        commonSources: File?,
        vararg mainArguments: String
    ): String = buildString {
        val (output, exitCode) = executeCompilerGrabOutput(
            this@compile,
            listOfNotNull(
                sources.absolutePath,
                commonSources?.absolutePath,
                commonSources?.absolutePath?.let("-Xcommon-sources="::plus)
            ) +
                "-Xmulti-platform" + mainArguments
        )
        appendLine("Exit code: $exitCode")
        appendLine("Output:")
        appendLine(output)
    }.trimTrailingWhitespacesAndAddNewlineAtEOF().trimEnd('\r', '\n')
}
