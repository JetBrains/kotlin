/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.plugin.test

import bsh.Interpreter
import groovy.lang.Binding
import groovy.util.GroovyScriptEngine
import org.apache.maven.shared.verifier.Verifier
import org.jetbrains.kotlin.maven.test.checkOrWriteKotlinMavenTestSettingsXml
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.io.StringReader
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

class MavenDistribution(val mavenHome: Path)

class MavenTestProject(
    val context: MavenTestExecutionContext,
    val mavenDistribution: MavenDistribution,
    val workDir: Path,
    val settingsFile: Path,
    val buildOptions: MavenBuildOptions,
) {
    fun build(vararg args: String, buildOptions: MavenBuildOptions = this.buildOptions): Verifier {
        val verifier = Verifier(
            workDir.absolutePathString(),
            null, // settingsFile is used only to extract local repo from there, but we pass it explicitly below
            false,
            mavenDistribution.mavenHome.absolutePathString(),
        )

        val javaHome = context.javaHomeProvider(buildOptions.javaVersion).absolutePathString()
        verifier.setEnvironmentVariable("JAVA_HOME", javaHome)

        verifier.setLocalRepo(context.sharedMavenLocal.absolutePathString())

        verifier.logFileName = "build.log"

        verifier.setSystemProperty("kotlin.version", context.kotlinVersion)
        verifier.addCliArguments("--settings", settingsFile.absolutePathString())
        verifier.addCliArguments(*args)

        runCatching {
            verifier.execute()
        }

        return verifier
    }

    fun loadInvokerPropertiesOrNull(): Properties? {
        val invokerProperties = workDir.resolve("invoker.properties")
        if (!invokerProperties.exists()) return null
        val props = Properties()
        props.load(invokerProperties.inputStream())
        return props
    }

    fun invokerGoals(): List<String>? = loadInvokerPropertiesOrNull()?.getProperty("invoker.goals")?.split(" ")

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
}

data class MavenBuildOptions(
    val javaVersion: String = "17",
)

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class KotlinMavenTestBase {

    @TempDir
    lateinit var tmpDir: Path

    lateinit var context: MavenTestExecutionContext
    open val buildOptions: MavenBuildOptions = MavenBuildOptions()

    @BeforeEach
    fun setup() {
        context = createMavenTestExecutionContextFromEnvironment(tmpDir)
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
        val project = MavenTestProject(context, mavenDistribution, workDir, settingsXml, buildOptions)

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
