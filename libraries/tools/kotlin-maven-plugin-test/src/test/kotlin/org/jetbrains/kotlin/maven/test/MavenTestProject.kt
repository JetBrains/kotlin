/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.plugin.test

import bsh.Interpreter
import groovy.lang.Binding
import groovy.util.GroovyScriptEngine
import org.apache.maven.shared.verifier.Verifier
import org.jetbrains.kotlin.maven.test.MavenBuildOptions
import org.jetbrains.kotlin.maven.test.isTeamCityRun
import org.jetbrains.kotlin.maven.test.printLog
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import java.nio.file.Path
import kotlin.io.path.*

class MavenTestProject(
    val name: String,
    val context: MavenTestExecutionContext,
    val workDir: Path,
    val settingsFile: Path,
    val buildOptions: MavenBuildOptions,
) {
    fun build(
        vararg args: String,
        environmentVariables: Map<String, String> = emptyMap(),
        expectedToFail: Boolean = false,
        buildOptions: MavenBuildOptions = this.buildOptions,
        code: (Verifier.() -> Unit)? = null,
    ): Verifier {
        val verifier = Verifier(
            workDir.absolutePathString(),
            null, // settingsFile is used only to extract local repo from there, but we pass it explicitly below
            false,
        )

        val javaHome = context.javaHomeProvider(buildOptions.javaVersion).absolutePathString()
        verifier.setEnvironmentVariable("JAVA_HOME", javaHome)

        for ((key, value) in environmentVariables) {
            verifier.setEnvironmentVariable(key, value)
        }

        verifier.setLocalRepo(context.sharedMavenLocal.absolutePathString())

        verifier.logFileName = "build.log"

        verifier.setSystemProperty("kotlin.version", context.kotlinVersion)
        verifier.addCliArguments("--settings", settingsFile.absolutePathString())

        val buildOptionsArgs = buildOptions.asCliArgs().toTypedArray()

        verifier.addCliArguments(*buildOptionsArgs, *args)

        val res = runCatching {
            verifier.execute()
        }

        if (expectedToFail) {
            if (res.isSuccess) {
                println("Maven build succeeded unexpectedly")
                verifier.printLog()
                throw AssertionError("Maven build succeeded unexpectedly")
            }
        } else {
            if (res.isFailure) {
                println("Maven build failed with error: ${res.exceptionOrNull()?.message}")
                verifier.printLog()
                throw res.exceptionOrNull()!!
            }
        }

        try {
            code?.invoke(verifier)
        } catch (e: AssertionError) {
            verifier.printLog()
            throw e
        }
        return verifier
    }

    fun runVerifyScript() {
        if (workDir.resolve("verify.bsh").exists()) verifyBsh()
        if (workDir.resolve("verify.groovy").exists()) verifyGroovy()
    }

    fun verifyBsh() {
        val outputBuffer = ByteArrayOutputStream()
        val printStream = PrintStream(outputBuffer, true)
        val noReader = StringReader("")
        try {
            val bsh = Interpreter(
                noReader, printStream, printStream, false
            )
            bsh.set("basedir", this.workDir.toString())
            bsh.source(workDir.resolve("verify.bsh").absolutePathString())
        } catch (e: Exception) {
            val capturedOutput = outputBuffer.toString()
            throw RuntimeException("Verification script failed. Output:\n$capturedOutput", e)
        }
    }

    fun verifyGroovy() {
        val groovyScriptEngine = GroovyScriptEngine(arrayOf(workDir.toUri().toURL()))
        val args = Binding(
            mapOf(
                "basedir" to workDir.toFile(),
                "kotlinVersion" to context.kotlinVersion
            )
        )
        val res = groovyScriptEngine.run("verify.groovy", args)
        if (res is Boolean) {
            assertTrue(res) { "verify.groovy returned false" }
        }
    }

    @Suppress("unused")
    fun makeSnapshotTo(base: String) {
        check(!isTeamCityRun) { "Please remove `makeSnapshotTo()` call from test. It is utility for local debugging only!" }
        val newWorkDir = Path(base).resolve(name)
        newWorkDir.createDirectories()

        @OptIn(ExperimentalPathApi::class)
        workDir.copyToRecursively(newWorkDir, overwrite = true, followLinks = true)
    }
}

