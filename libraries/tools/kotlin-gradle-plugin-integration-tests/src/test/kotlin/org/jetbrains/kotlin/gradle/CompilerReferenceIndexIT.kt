/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.DATA_PATH
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.FILE_IDS_TO_PATHS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.LOOKUPS_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.SUBTYPES_FILENAME
import org.jetbrains.kotlin.buildtools.api.cri.CriToolchain.Companion.cri
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.io.path.*
import kotlin.test.assertTrue

@JvmGradlePluginTests
@DisplayName("Gradle / Compiler Reference Index")
class CompilerReferenceIndexIT : KGPBaseTest() {

    @GradleTest
    @DisplayName("Smoke test for Gradle / CRI data generation and deserialization")
    @OptIn(ExperimentalBuildToolsApi::class)
    fun smokeTestCriDataGeneration(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            gradleProperties.append(
                """
                kotlin.compiler.runViaBuildToolsApi=true
                kotlin.compiler.generateCompilerRefIndex=true
                kotlin.compiler.execution.strategy=in-process
                """.trimIndent() + "\n"
            )

            val srcDir = kotlinSourcesDir()
            srcDir.resolve("cri/Base.kt").apply {
                parent.createDirectories()
                writeText(
                    """
                    package cri
                    open class Base
                    class Derived: Base()
                    fun use(d: Derived) = d.toString()
                    """.trimIndent()
                )
            }

            build("assemble", "--info") {
                assertOutputContains("Generating Compiler Reference Index...")
            }

            val criDir = projectPath.resolve("build/kotlin/compileKotlin/cacheable").resolve(DATA_PATH)
            val lookups = criDir.resolve(LOOKUPS_FILENAME)
            val fileIdsToPaths = criDir.resolve(FILE_IDS_TO_PATHS_FILENAME)
            val subtypes = criDir.resolve(SUBTYPES_FILENAME)

            assertTrue(lookups.exists(), "There's no lookups data at $lookups")
            assertTrue(fileIdsToPaths.exists(), "There's no fileIdsToPaths data at $fileIdsToPaths")
            assertTrue(subtypes.exists(), "There's no subtypes data at $subtypes")

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
}
