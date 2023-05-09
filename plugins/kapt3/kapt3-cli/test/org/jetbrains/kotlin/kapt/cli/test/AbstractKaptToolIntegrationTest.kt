/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.cli.test

import com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.cli.common.arguments.readArgumentsFromArgFile
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInfo
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.jvm.optionals.getOrNull

abstract class AbstractKaptToolIntegrationTest {
    private lateinit var tmpdir: File
    private lateinit var testInfo: TestInfo

    @BeforeEach
    @OptIn(ExperimentalStdlibApi::class)
    fun setUp(testInfo: TestInfo) {
        this.testInfo = testInfo
        tmpdir = KtTestUtil.tmpDirForTest(
            testInfo.testClass.getOrNull()?.simpleName ?: "TEST",
            testInfo.displayName
        )
    }

    fun runTest(filePath: String) {
        val testDir = File(filePath)
        val testFile = File(testDir, "build.txt")
        assert(testFile.isFile) { "build.txt doesn't exist" }

        testDir.listFiles()?.forEach { it.copyRecursively(File(tmpdir, it.name)) }
        doTestInTempDirectory(testFile, File(tmpdir, testFile.name))
    }

    private class GotResult(val actual: String): RuntimeException()

    private fun doTestInTempDirectory(originalTestFile: File, testFile: File) {
        val sections = Section.parse(testFile)

        for (section in sections) {
            try {
                when (section.name) {
                    "mkdir" -> section.args.forEach { File(tmpdir, it).mkdirs() }
                    "copy" -> copyFile(originalTestFile.parentFile, section.args)
                    "kotlinc" -> runKotlinDistBinary("kotlinc", section.args)
                    "kapt" -> runKotlinDistBinary("kapt", section.args)
                    "javac" -> runJavac(section.args)
                    "java" -> runJava(section.args)
                    "output" -> {
                        val output = File(tmpdir, "processOutput.txt").readText()
                        val expected = section.content.trim()
                        JUnit5Assertions.assertTrue(output.contains(expected)) {
                            "Output\"$output\" doesn't contain the expected string \"$expected\""
                        }
                    }
                    "after" -> {}
                    else -> error("Unknown section name ${section.name}")
                }
            } catch (e: GotResult) {
                val actual = sections.replacingSection("after", e.actual).render()
                JUnit5Assertions.assertEqualsToFile(originalTestFile, actual)
                return
            } catch (e: Throwable) {
                throw RuntimeException("Section ${section.name} failed:\n${section.content}", e)
            }
        }
    }

    private fun copyFile(testDir: File, args: List<String>) {
        assert(args.size == 2)
        val source = File(testDir, args[0])
        val target = File(tmpdir, args[1]).also { it.parentFile.mkdirs() }
        source.copyRecursively(target)
    }

    private fun runKotlinDistBinary(name: String, args: List<String>) {
        val executableName = if (SystemInfo.isWindows) name + ".bat" else name
        val executablePath = File("dist/kotlinc/bin/" + executableName).absolutePath
        runProcess(executablePath, args)
    }

    private fun runJavac(args: List<String>) {
        val executableName = if (SystemInfo.isWindows) "javac.exe" else "javac"
        val executablePath = File(getJdk8Home(), "bin/" + executableName).absolutePath
        runProcess(executablePath, args)
    }

    private fun runJava(args: List<String>) {
        val outputFile = File(tmpdir, "javaOutput.txt")

        val executableName = if (SystemInfo.isWindows) "java.exe" else "java"
        val executablePath = File(getJdk8Home(), "bin/" + executableName).absolutePath
        runProcess(executablePath, args, outputFile)

        throw GotResult(outputFile.takeIf { it.isFile }?.readText() ?: "")
    }

    private fun runProcess(executablePath: String, args: List<String>, outputFile: File = File(tmpdir, "processOutput.txt")) {
        fun err(message: String): Nothing = error("$message: ${testInfo.displayName} (${args.joinToString(" ")})")

        outputFile.delete()

        val transformedArgs = transformArguments(args).toTypedArray()
        val process = ProcessBuilder(executablePath, *transformedArgs).directory(tmpdir)
            .inheritIO()
            .redirectError(ProcessBuilder.Redirect.to(outputFile))
            .redirectOutput(ProcessBuilder.Redirect.to(outputFile))
            .start()

        if (!process.waitFor(2, TimeUnit.MINUTES)) err("Process is still alive")
        if (process.exitValue() != 0) {
            throw GotResult(buildString {
                append("Return code: ").appendLine(process.exitValue()).appendLine()
                appendLine(outputFile.readText())
            })
        }
    }

    private fun transformArguments(args: List<String>): List<String> {
        return args.map {
            val arg = it.replace("%KOTLIN_STDLIB%", File("dist/kotlinc/lib/kotlin-stdlib.jar").absolutePath)
            if (SystemInfo.isWindows && (arg.contains("=") || arg.contains(":"))) {
                "\"" + arg + "\""
            } else {
                arg
            }
        }
    }

    private fun getJdk8Home(): File {
        val homePath = System.getenv()["JDK_1_8"] ?: System.getenv()["JDK_18"] ?: error("Can't find JDK 1.8 home, please define JDK_1_8 variable")
        return File(homePath)
    }
}

private val Section.args get() = readArgumentsFromArgFile(preprocessPathSeparators(content))

private fun preprocessPathSeparators(text: String): String = buildString {
    for (line in text.lineSequence()) {
        val transformed = if (line.startsWith("-cp ")) line.replace(':', File.pathSeparatorChar) else line
        appendLine(transformed)
    }
}
