/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.mpp

import org.gradle.api.logging.LogLevel
import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.PublisherConfiguration
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.include
import org.jetbrains.kotlin.gradle.uklibs.setupMavenPublication
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@MppGradlePluginTests
@DisplayName("Separate KMP compilation a.k.a. the new KMP compilation scheme: KT-77546")
class SeparateKmpCompilationIT : KGPBaseTest() {
    @DisplayName("inaccessible symbols: local kmp library -> compile consumer")
    @GradleTest
    fun localKmpConsumer(gradleVersion: GradleVersion) {
        doTestInaccessibleSymbols(gradleVersion)
    }

    @DisplayName("inaccessible symbols: published kmp library -> compile consumer")
    @GradleTest
    fun remoteKmpConsumer(gradleVersion: GradleVersion, @TempDir localRepository: Path) {
        doTestInaccessibleSymbols(gradleVersion, localRepository)
    }

    private fun doTestInaccessibleSymbols(gradleVersion: GradleVersion, localRepository: Path? = null) {
        project("empty", gradleVersion, localRepoDir = localRepository) {
            plugins {
                kotlin("multiplatform")
            }
            val isNativeDisabled = true // TODO: remove this condition after KT-77716
            val sourceSetNames = setOfNotNull(
                "commonMain",
                "jvmMain",
                "jsMain",
                "linuxArm64Main".takeUnless { isNativeDisabled },
                "linuxX64Main".takeUnless { isNativeDisabled },
                "nativeMain".takeUnless { isNativeDisabled },
            )
            val repositoryPath = localRepository?.absolutePathString()
            val depGroup = if (localRepository != null) "org.example" else null
            val depVersion = if (localRepository != null) "1.0" else null
            buildScriptInjection {
                with(project) {
                    applyMultiplatform {
                        jvm()
                        js()
                        if (!isNativeDisabled) {
                            linuxArm64()
                            linuxX64()
                        }

                        with(sourceSets) {
                            commonMain {
                                dependencies {
                                    implementation(if (repositoryPath != null) "$depGroup:library:$depVersion" else project(":library"))
                                }
                                compileSource(
                                    """
                                    |fun main() {
                                    |    ${
                                        sourceSetNames.map { named(it) }.joinToString("\n    ") { sourceSet -> "${sourceSet.name}()" }
                                    }
                                    |}
                                """.trimMargin()
                                )
                            }
                        }
                    }
                }
            }

            gradleProperties.appendText(
                """
                |${org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.PropertyNames.KOTLIN_KMP_SEPARATE_COMPILATION}=true
                """.trimMargin()
            )

            val librarySubproject = project("empty", gradleVersion) {
                buildScriptInjection {
                    project.applyMultiplatform {
                        jvm()
                        js()
                        if (!isNativeDisabled) {
                            linuxArm64()
                            linuxX64()
                        }
                        with(sourceSets) {
                            for (sourceSet in sourceSetNames.map { named(it) }) {
                                sourceSet.get().compileStubSourceWithSourceSetName()
                            }
                        }
                    }
                    if (repositoryPath != null) {
                        require(depGroup != null && depVersion != null)
                        project.setupMavenPublication(
                            "Stub",
                            PublisherConfiguration(depGroup, depVersion, repositoryPath)
                        )
                    }
                }
            }

            include(librarySubproject, "library")

            if (localRepository != null) {
                build(":library:publish")
            }

            buildAndFail(
                ":assemble",
                buildOptions = defaultBuildOptions.copy(continueAfterFailure = true)
            ) {
                // ensures no unexpected task dependencies are added
                val libraryTasks = setOf(
                    ":library:allMetadataJar",
                    ":library:checkKotlinGradlePluginConfigurationErrors",
                    ":library:compileCommonMainKotlinMetadata",
                    ":library:compileJvmMainJava",
                    ":library:compileKotlinJs",
                    ":library:compileKotlinJvm",
                    ":library:exportCommonSourceSetsMetadataLocationsForMetadataApiElements",
                    ":library:exportRootPublicationCoordinatesForMetadataApiElements",
                    ":library:generateProjectStructureMetadata",
                    ":library:generateSourceIn_commonMain_0",
                    ":library:generateSourceIn_jsMain_2",
                    ":library:generateSourceIn_jvmMain_1",
                    ":library:jvmJar",
                    ":library:jvmMainClasses",
                    ":library:jvmProcessResources",
                    ":library:metadataCommonMainClasses",
                    ":library:metadataCommonMainProcessResources",
                    ":library:processJvmMainResources",
                    ":library:transformCommonMainDependenciesMetadata",
                )
                val thisProjectTasks = setOf(
                    ":checkKotlinGradlePluginConfigurationErrors",
                    ":compileCommonMainKotlinMetadata",
                    ":compileKotlinJs",
                    ":compileKotlinJvm",
                    ":generateProjectStructureMetadata",
                    ":generateSourceIn_commonMain_0",
                    ":jsProcessResources",
                    ":jvmProcessResources",
                    ":metadataCommonMainProcessResources",
                    ":processJvmMainResources",
                    ":transformCommonMainDependenciesMetadata",
                )
                assertExactTasksInGraph(
                    if (localRepository != null) {
                        thisProjectTasks
                    } else {
                        libraryTasks + thisProjectTasks
                    }
                )
                val compileTasks = setOfNotNull(
                    ":compileKotlinJvm",
                    ":compileKotlinJs",
                    ":compileKotlinLinuxArm64".takeUnless { isNativeDisabled },
                    ":compileKotlinLinuxX64".takeUnless { isNativeDisabled },
                )
                val specificSourceSets = sourceSetNames - "commonMain"
                val outputPerTask = compileTasks.associateWith { getOutputForTask(it, logLevel = LogLevel.INFO) }
                for ((task, taskOutput) in outputPerTask) {
                    assertFalse(
                        taskOutput.contains("generatedSource_commonMain_\\d+.kt:\\d+:\\d+ Unresolved reference 'commonMain'".toRegex()),
                        "$task should be able to resolve `commonMain()`\n$taskOutput"
                    )
                    for (sourceSet in specificSourceSets) {
                        assertTrue(
                            taskOutput.contains("generatedSource_commonMain_\\d+.kt:\\d+:\\d+ Unresolved reference '$sourceSet'".toRegex()),
                            "`$sourceSet()` should be unresolvable for $task\n$taskOutput"
                        )
                    }
                }
            }
        }
    }
}