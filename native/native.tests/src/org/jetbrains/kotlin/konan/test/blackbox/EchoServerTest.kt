/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.konan.test.blackbox

import com.intellij.testFramework.TestDataPath
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.test.blackbox.support.*
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationFactory
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.group.FirPipeline
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestRunChecks
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Assumptions
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import java.io.File
import java.net.Socket
import kotlin.test.assertEquals
import kotlin.time.Duration

@TestMetadata("native/native.tests/testData/echoServer")
@TestDataPath("\$PROJECT_ROOT")
class ClassicEchoServerTest : EchoServerBaseTest()

@FirPipeline
@Tag("frontend-fir")
@TestMetadata("native/native.tests/testData/echoServer")
@TestDataPath("\$PROJECT_ROOT")
class FirEchoServerTest : EchoServerBaseTest()

abstract class EchoServerBaseTest : AbstractNativeSimpleTest() {
    private val testCompilationFactory = TestCompilationFactory()

    @Test
    fun test() {
        Assumptions.assumeFalse(targets.testTarget.family == Family.MINGW)
        Assumptions.assumeFalse(targets.areDifferentTargets(), "The test uses localhost networking")
        val rootDir = File("native/native.tests/testData/echoServer")

        val cinteropModule = TestModule.Exclusive("sockets", emptySet(), emptySet(), emptySet()).apply {
            files += TestFile.createCommitted(rootDir.resolve("sockets.def"), this)
        }
        val ktModule = TestModule.Exclusive("server", setOf(cinteropModule.name), emptySet(), emptySet()).apply {
            files += TestFile.createCommitted(rootDir.resolve("echo_server.kt"), this)
        }
        val testCase = TestCase(
            id = TestCaseId.Named("echo_server"),
            kind = TestKind.STANDALONE_NO_TR,
            modules = setOf(cinteropModule, ktModule),
            freeCompilerArgs = TestCompilerArgs.EMPTY,
            nominalPackageName = PackageName("echo_server"),
            checks = TestRunChecks.Default(Duration.INFINITE),
            extras = TestCase.NoTestRunnerExtras(),
        ).apply {
            initialize(null, null)
        }

        val compilationResult = testCompilationFactory.testCasesToExecutable(listOf(testCase), testRunSettings).result.assertSuccess()

        val process = ProcessBuilder(compilationResult.resultingArtifact.path).start()
        try {
            val port = process.inputStream.bufferedReader().readLine().toInt()
            Socket("localhost", port).use { socket ->
                val inputStream = socket.getInputStream().bufferedReader()
                val outputStream = socket.getOutputStream().writer()
                outputStream.write("Hello\n")
                outputStream.flush()
                val read = inputStream.readLine()
                assertEquals("Hello", read)
            }
        } finally {
            // Make sure the process is gone no matter what.
            process.destroyForcibly()
        }
    }
}