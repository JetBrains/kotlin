/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.tooling.internal.consumer.ConnectorServices
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.DATA_PATH
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.FILE_IDS_TO_PATHS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.LOOKUPS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.SUBTYPES_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.cri
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilerExecutionStrategy
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.extension.AfterTestExecutionCallback
import org.junit.jupiter.api.extension.RegisterExtension
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.test.assertTrue

@JvmGradlePluginTests
@DisplayName("Gradle / Compiler Reference Index")
class CompilerReferenceIndexIT : KGPBaseTest() {

    private val kotlinDaemonRunFilesDir: Path
        get() = kgpTestInfraWorkingDirectory.resolve("kotlin-daemon-run-files")

    override val defaultBuildOptions: BuildOptions = super.defaultBuildOptions.copy(
        runViaBuildToolsApi = true,
        generateCompilerRefIndex = true,
    )

    private val defaultInProcessBuildOptions: BuildOptions = defaultBuildOptions.copy(
        compilerExecutionStrategy = KotlinCompilerExecutionStrategy.IN_PROCESS,
    )

    private val defaultDaemonBuildOptions: BuildOptions = defaultBuildOptions.copy(
        compilerExecutionStrategy = KotlinCompilerExecutionStrategy.DAEMON,
        customKotlinDaemonRunFilesDirectory = kotlinDaemonRunFilesDir.toFile(),
    )

    @GradleTest
    @DisplayName("Smoke test for Gradle / CRI data generation and deserialization")
    @OptIn(ExperimentalBuildToolsApi::class)
    @GradleTestExtraStringArguments("in-process", "daemon")
    fun smokeTestCriDataGeneration(gradleVersion: GradleVersion, strategy: String) {
        project(
            "kotlinProject",
            gradleVersion,
            buildOptions = when (strategy) {
                "in-process" -> defaultInProcessBuildOptions
                "daemon" -> defaultDaemonBuildOptions
                else -> return // `out-of-process` strategy is not supported by BTA
            }
        ) {
            kotlinSourcesDir().source("main.kt") {
                //language=kotlin
                """
                open class Base
                class Derived: Base()
                fun use(d: Derived) = d.toString()
                """.trimIndent()
            }

            build("assemble") {
                if (strategy == "in-process") assertOutputContains("Generating Compiler Reference Index...")
            }

            val criDir = projectPath.resolve("build/kotlin/compileKotlin/cacheable").resolve(DATA_PATH)
            val lookups = criDir.resolve(LOOKUPS_FILENAME)
            val fileIdsToPaths = criDir.resolve(FILE_IDS_TO_PATHS_FILENAME)
            val subtypes = criDir.resolve(SUBTYPES_FILENAME)

            assertFilesExist(lookups, fileIdsToPaths, subtypes)

            val toolchain = KotlinToolchains.loadImplementation(this::class.java.classLoader)
            toolchain.createBuildSession().use { session ->
                val lookupEntries = session.executeOperation(
                    toolchain.cri.createCriLookupDataDeserializationOperation(lookups.readBytes())
                ).toList()
                val fileIdToPathEntries = session.executeOperation(
                    toolchain.cri.createCriFileIdToPathDataDeserializationOperation(fileIdsToPaths.readBytes())
                ).toList()
                val subtypeEntries = session.executeOperation(
                    toolchain.cri.createCriSubtypeDataDeserializationOperation(subtypes.readBytes())
                ).toList()

                assertTrue(lookupEntries.isNotEmpty(), "Expected non-empty CRI lookup entries")
                assertTrue(fileIdToPathEntries.isNotEmpty(), "Expected non-empty CRI fileIdToPath entries")
                assertTrue(subtypeEntries.isNotEmpty(), "Expected non-empty CRI subtype entries")
            }
        }
    }

    @Suppress("unused")
    @RegisterExtension
    private val afterTestExecutionCallback: AfterTestExecutionCallback = AfterTestExecutionCallback {
        ConnectorServices.reset()
        awaitKotlinDaemonTermination(kotlinDaemonRunFilesDir)
    }
}
