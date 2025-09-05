/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.util.GradleTestCapturingKotlinLogger
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.assertEquals

class RunToolInSeparateProcessTest {
    @Rule
    @JvmField
    var tmp: TemporaryFolder = TemporaryFolder.builder().assureDeletion().build();

    @Test
    fun testWhitespacesInCliArgumentsFile() {
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
        )
        assertEquals(ExitCode.OK, exitCode)
        val expectedOutput = buildList {
            add("Args size: ${args.size}")
            addAll(args.map { "Arg: $it" })
            add("OK")
            add(KotlinCompilerExecutionStrategy.OUT_OF_PROCESS.asFinishLogMessage)
        }
        assertEquals(expectedOutput, logger.messages)
        assertEquals(emptyList(), logger.exceptions)
    }
}