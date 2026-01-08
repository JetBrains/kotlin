/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.plugin.test

import bsh.Interpreter
import groovy.lang.Binding
import groovy.util.GroovyScriptEngine
import org.apache.maven.shared.verifier.Verifier
import org.jetbrains.kotlin.maven.test.TestVersions
import org.jetbrains.kotlin.maven.test.checkOrWriteKotlinMavenTestSettingsXml
import org.jetbrains.kotlin.maven.test.isTeamCityRun
import org.jetbrains.kotlin.maven.test.printLog
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.StringReader
import java.nio.file.Path
import kotlin.io.bufferedReader
import kotlin.io.path.*

class MavenDistribution(val mavenHome: Path)

class MavenTestProject(
    val name: String,
    val context: MavenTestExecutionContext,
    val mavenDistribution: MavenDistribution,
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
            mavenDistribution.mavenHome.absolutePathString(),
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

data class MavenBuildOptions(
    val javaVersion: TestVersions.Java = TestVersions.Java.JDK_17,
    val useKotlinDaemon: Boolean? = null,
    val extraMavenProperties: Map<String, String> = emptyMap(),
) {
    fun asCliArgs(): List<String> = buildList {
        useKotlinDaemon?.let { add("-Dkotlin.compiler.daemon=$it") }
        extraMavenProperties.forEach { (key, value) -> add("-D$key=$value") }
    }
}

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class KotlinMavenTestBase {

    @TempDir
    lateinit var tmpDir: Path

    lateinit var context: MavenTestExecutionContext
    open val buildOptions: MavenBuildOptions = MavenBuildOptions()

    @BeforeEach
    fun setup() {
        context = createMavenTestExecutionContextFromEnvironment(tmpDir)
    }

    @AfterEach
    @EnabledOnOs(OS.WINDOWS)
    fun cleanup() {
        try {
            @OptIn(kotlin.io.path.ExperimentalPathApi::class)
            tmpDir.deleteRecursively()
            return
        } catch (_: Throwable) {
            System.gc();
        }

        // try again, and fail otherwise
        @OptIn(kotlin.io.path.ExperimentalPathApi::class)
        tmpDir.deleteRecursively()
    }

    fun testProject(
        projectDir: String,
        mavenVersion: String,
        buildOptions: MavenBuildOptions = this.buildOptions,
        code: (MavenTestProject.() -> Unit)? = null,
    ): MavenTestProject {
        val workDir = copyProjectDir(projectDir, mavenVersion)

        context.verifyCommonBshLocation.copyTo(workDir.resolve("verify-common.bsh"))

        val settingsXml = workDir.resolve("settings.xml")
        settingsXml.checkOrWriteKotlinMavenTestSettingsXml(context.kotlinBuildRepo)

        val mavenDistribution = context.mavenDistributionProvider(mavenVersion)
        val project = MavenTestProject(
            name = projectDir,
            context = context,
            mavenDistribution = mavenDistribution,
            workDir = workDir,
            settingsFile = settingsXml,
            buildOptions = buildOptions
        )

        if (code != null) code(project)
        return project
    }

    private fun copyProjectDir(projectDir: String, mavenVersion: String): Path {
        val originalProjectDir = context.testProjectsDir.resolve(projectDir)
        if (!originalProjectDir.exists()) error("Project dir $originalProjectDir does not exist")

        val copyTo = context.testWorkDir.resolve(projectDir).resolve(mavenVersion)
        copyTo.createDirectories()

        @OptIn(ExperimentalPathApi::class)
        originalProjectDir.copyToRecursively(copyTo, overwrite = false, followLinks = true)

        return copyTo
    }
}
