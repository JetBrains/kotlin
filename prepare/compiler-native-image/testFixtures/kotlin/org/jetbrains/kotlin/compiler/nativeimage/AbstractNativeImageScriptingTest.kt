/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.nativeimage

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.RegisteredDirectives
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File

abstract class AbstractNativeImageScriptingTest : AbstractNativeImageCodegenTest() {
    private val runner: NativeImageCompilerRunner by lazy { NativeImageCompilerRunner(javaHome) }

    override fun runTest(filePath: String) {
        val testFile = ForTestCompileRuntime.transformTestDataPath(filePath)
        val source = testFile.readText()
        val directives = parseDirectives(source)

        val skipReason = shouldSkip(source, directives)
        assumeTrue(skipReason == null) { "skipped: $skipReason" }

        val withReflect = JvmEnvironmentConfigurationDirectives.WITH_REFLECT in directives
        val withFullJdk = JvmEnvironmentConfigurationDirectives.FULL_JDK in directives

        val outDir = File(workingDir, "ni-out").apply { mkdirs() }

        val [exitCode, compilerStdout] = runCompiler(
            arguments = buildCompilerArgs(testFile, outDir, directives, withFullJdk),
            classpath = buildClasspath(withReflect, withFullJdk),
        )
        assertCompilerOutput(exitCode, compilerStdout, directives)
    }

    override fun assertCompilerOutput(exitCode: Int, compilerStdout: String, directives: RegisteredDirectives) {
        assertEquals(0, exitCode, "compilation failed:\n$compilerStdout")
        assertEquals("OK", compilerStdout.trim(), "script output != 'OK'")
    }

    override fun runCompiler(
        arguments: List<String>,
        classpath: List<File>,
    ): CompilerInvocationResult = runner.run(
        workingDir = workingDir,
        arguments = arguments + listOf("-script", "-nowarn"),
        classpath = classpath,
        jvmArgs = listOf("-Dkotlinc.test.allow.testonly.language.features=true"),
    )
}
