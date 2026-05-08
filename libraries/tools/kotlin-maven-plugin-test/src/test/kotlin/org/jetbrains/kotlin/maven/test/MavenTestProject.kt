/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven.test

import org.apache.maven.shared.verifier.Verifier
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.nio.file.Path
import kotlin.io.path.*

private const val EXTRA_MAVEN_OPTS =
    ""

private val NESTED_MAVEN_CLI_ARGUMENTS = arrayOf("-B", "-ntp", "-nsu")

class MavenTestProject(
    val name: String,
    val context: MavenTestExecutionContext,
    val workDir: Path,
    val settingsFile: Path,
    val buildOptions: MavenBuildOptions,
    val mavenVersion: String,
) {
    val jdk8: String get() = context.getJavaHomeString(TestVersions.Java.JDK_1_8)
    val jdk11: String get() = context.getJavaHomeString(TestVersions.Java.JDK_11)
    val jdk17: String get() = context.getJavaHomeString(TestVersions.Java.JDK_17)
    val jdk21: String get() = context.getJavaHomeString(TestVersions.Java.JDK_21)

    fun build(
        vararg args: String,
        environmentVariables: Map<String, String> = emptyMap(),
        expectedToFail: Boolean = false,
        buildOptions: MavenBuildOptions = this.buildOptions,
        code: (Verifier.() -> Unit)? = null,
    ): Verifier {
        // Maven 4+ requires JDK 17+ as runtime
        if (mavenVersion.startsWith("4")) {
            assumeTrue(
                buildOptions.javaVersion.numericVersion >= 17,
                "Maven $mavenVersion requires JDK 17+ as runtime, but ${buildOptions.javaVersion} was requested"
            )
        }

        val verifier = Verifier(
            workDir.absolutePathString(),
            null, // settingsFile is used only to extract local repo from there, but we pass it explicitly below
            false,
        )

        verifier.isAutoclean = false

        val javaHome = context.jdkProvider.getJavaHome(buildOptions.javaVersion) ?:
            throw RuntimeException("Can't find path for ${buildOptions.javaVersion}")
        verifier.setEnvironmentVariable("JAVA_HOME", javaHome.absolutePathString())

        for ((key, value) in environmentVariables) {
            verifier.setEnvironmentVariable(key, value)
        }

        // Append shared Maven opts that speedup tests and have no semantic-changing value
        val mavenOpts = listOf(EXTRA_MAVEN_OPTS, environmentVariables["MAVEN_OPTS"])
            .filterNot { it.isNullOrBlank() }
            .joinToString(" ")
        verifier.setEnvironmentVariable("MAVEN_OPTS", mavenOpts)

        verifier.setLocalRepo(context.sharedMavenLocal.absolutePathString())

        verifier.logFileName = "build.log"

        verifier.setSystemProperty("kotlin.version", context.kotlinVersion)
        verifier.addCliArguments(*NESTED_MAVEN_CLI_ARGUMENTS)
        verifier.addCliArguments("--settings", settingsFile.absolutePathString())

        if (buildOptions.toolchains.isNotEmpty()) {
            val toolchainsXml = workDir.resolve("toolchains.xml")
            val entries = buildOptions.toolchains.map { context.toolchain(it) }
            toolchainsXml.writeToolchainsXml(entries)
            verifier.addCliArguments("--global-toolchains", toolchainsXml.absolutePathString())
        }

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

    @Suppress("unused")
    fun makeSnapshotTo(base: String) {
        check(!isTeamCityRun) { "Please remove `makeSnapshotTo()` call from test. It is utility for local debugging only!" }
        val newWorkDir = Path(base).resolve(name)
        newWorkDir.createDirectories()

        @OptIn(ExperimentalPathApi::class)
        workDir.copyToRecursively(newWorkDir, overwrite = true, followLinks = true)
    }
}
