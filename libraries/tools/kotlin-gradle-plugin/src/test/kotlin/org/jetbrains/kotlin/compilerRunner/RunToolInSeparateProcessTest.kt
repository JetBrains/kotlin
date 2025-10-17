/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.util.GradleTestCapturingKotlinLogger
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.IOException
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RunToolInSeparateProcessTest {
    @Rule
    @JvmField
    var tmp: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build();

    @Test
    fun testWhitespacesInCompilerCliArgumentsWithJvmArgsFile() {
        doDummyCompilerTest(expectJvmArgsFile = true, explicitJdk = File(System.getProperty("jdk11Home")) to 11)
    }

    @Test
    fun testWhitespacesInCompilerCliArgumentsWithoutJvmArgsFile() {
        expectWindowsFailure {
            doDummyCompilerTest(expectJvmArgsFile = false, explicitJdk = File(System.getProperty("jdk8Home")) to 8)
        }
    }

    @Test
    fun testWhitespacesInCompilerCliArgumentsDefault() {
        expectWindowsFailure {
            doDummyCompilerTest(expectJvmArgsFile = false)
        }
    }

    @Suppress("DEPRECATION")
    private fun expectWindowsFailure(test: () -> Unit) {
        if (isWindows) {
            try {
                test()
                assert(false) { "Expected test to fail on Windows" }
            } catch (e: IOException) {
                assertNotNull(e.message)
                assertContains(e.message!!, "CreateProcess error=206, The filename or extension is too long")
            }
        } else {
            test()
        }
    }

    private fun doDummyCompilerTest(expectJvmArgsFile: Boolean, explicitJdk: Pair<File, Int>? = null) {
        val logger = GradleTestCapturingKotlinLogger()
        val buildDir = tmp.newFolder()
        val args = listOf("argument 1", "argument 2")
        val currentClasspath = System.getProperty("java.class.path")
            .split(File.pathSeparator)
            .map { File(it) }
        val exitCode = runToolInSeparateProcess(
            argsArray = args.toTypedArray(),
            compilerClassName = DummyCompilerClass::class.java.name,
            classpath = currentClasspath,
            logger = logger,
            buildDir = buildDir,
            explicitJdk = explicitJdk,
        )
        val expectedOutput = buildList {
            add(
                if (expectJvmArgsFile) {
                    "Using JVM args file to run the compiler"
                } else {
                    "Using regular JVM arguments to run the compiler"
                }
            )
            add("Args size: ${args.size}")
            addAll(args.map { "Arg: $it" })
            add("OK")
            add(KotlinCompilerExecutionStrategy.OUT_OF_PROCESS.asFinishLogMessage)
        }
        assertEquals(expectedOutput, logger.messages)
        assertEquals(emptyList(), logger.exceptions)
        @Suppress("DEPRECATION")
        assertEquals(ExitCode.OK, exitCode)
    }
}