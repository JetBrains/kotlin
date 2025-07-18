/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.addPublishedProjectToRepositories
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.publishMultiplatformLibrary
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.readText

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for exporting Kdocs for Apple Framework")
@NativeGradlePluginTests
class ObjCExportKdocIT : KGPBaseTest() {

    @OptIn(EnvironmentalVariablesOverride::class)
    @DisplayName("Framework contains Kdoc documentation")
    @GradleTest
    fun shouldGenerateKdoc(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }

            val kdocFooMethod = """
                /**
                 * This is a KDoc for the foo method.
                 */
            """.trimIndent()

            val kdocBarMethod = """
                /**
                 * This is a KDoc for the bar method.
                 */
            """.trimIndent()

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64().binaries.framework {
                        baseName = "Shared"
                    }
                    sourceSets.commonMain.get().compileSource(
                        """
                                package com.project.shared
                                $kdocFooMethod
                                fun foo() = "foo"
                                $kdocBarMethod
                                fun bar() = "bar"
                            """.trimIndent()
                    )
                }
            }

            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("shared/build/builtProductsDir").toString(),
            )

            build(":assembleDebugAppleFrameworkForXcodeIosArm64", environmentVariables = environmentVariables) {
                assertTasksExecuted(":assembleDebugAppleFrameworkForXcodeIosArm64")

                val headerText = projectPath
                    .resolve("build/xcode-frameworks/debug/iphoneos123/Shared.framework/Headers/Shared.h")
                    .readText()

                assert(headerText.contains(kdocFooMethod)) {
                    "Expected Kdoc for foo function in Shared.h"
                }

                assert(headerText.contains(kdocBarMethod)) {
                    "Expected Kdoc for bar function in Shared.h"
                }
            }
        }
    }

    @OptIn(EnvironmentalVariablesOverride::class)
    @DisplayName("Framework contains no Kdoc documentation")
    @GradleTest
    fun shouldNotGenerateKdoc(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }

            val kdocFooMethod = """
                /**
                 * This is a KDoc for the foo method.
                 */
            """.trimIndent()

            val kdocBarMethod = """
                /**
                 * This is a KDoc for the bar method.
                 */
            """.trimIndent()

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64().binaries.framework {
                        baseName = "Shared"
                        exportKdoc.set(false)
                    }
                    sourceSets.commonMain.get().compileSource(
                        """
                                package com.project.shared
                                $kdocFooMethod
                                fun foo() = "foo"
                                $kdocBarMethod
                                fun bar() = "bar"
                            """.trimIndent()
                    )
                }
            }

            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("shared/build/builtProductsDir").toString(),
            )

            build(":assembleDebugAppleFrameworkForXcodeIosArm64", environmentVariables = environmentVariables) {
                assertTasksExecuted(":assembleDebugAppleFrameworkForXcodeIosArm64")

                val headerText = projectPath
                    .resolve("build/xcode-frameworks/debug/iphoneos123/Shared.framework/Headers/Shared.h")
                    .readText()

                assert(headerText.contains(kdocFooMethod).not()) {
                    "Expected no Kdoc for foo function in Shared.h"
                }

                assert(headerText.contains(kdocBarMethod).not()) {
                    "Expected no Kdoc for bar function in Shared.h"
                }
            }
        }
    }

    @OptIn(EnvironmentalVariablesOverride::class)
    @DisplayName("Framework contains Kdoc documentation for klib with sources jar")
    @GradleTest
    fun shouldGenerateKdocInKlibPublication(gradleVersion: GradleVersion) {

        val kdocLibFooClass = """
            /**
             * This is a KDoc for the LibFoo class.
             */
        """.trimIndent()

        val kdocLibBarClass = """
            /**
             * This is a KDoc for the LibBar class.
             */
        """.trimIndent()

        // Library with sources jar
        val multiplatformLibraryWithSources = publishMultiplatformLibrary(gradleVersion, "lib1") {
            iosArm64()
            sourceSets.commonMain.get().compileSource(
                """
                                package com.subproject.lib1
                                $kdocLibFooClass
                                class LibFoo()
                            """.trimIndent()
            )
        }

        // Library without sources jar
        val multiplatformLibraryWithoutSources = publishMultiplatformLibrary(gradleVersion, "lib2") {
            iosArm64()
            sourceSets.commonMain.get().compileSource(
                """
                                package com.subproject.lib2
                                $kdocLibBarClass
                                class LibBar()
                            """.trimIndent()
            )

            withSourcesJar(false)
        }

        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            addPublishedProjectToRepositories(multiplatformLibraryWithSources)
            addPublishedProjectToRepositories(multiplatformLibraryWithoutSources)

            val kdocLibFooMethod = """
                /**
                 * This is a KDoc for the LibFoo method.
                 */
            """.trimIndent()

            val kdocLibBarMethod = """
                /**
                 * This is a KDoc for the LibBar method.
                 */
            """.trimIndent()

            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64().binaries.framework {
                        baseName = "Shared"
                    }
                    sourceSets.commonMain {
                        compileSource(
                            """
                                package com.project.shared
                                import com.subproject.lib1.LibFoo
                                import com.subproject.lib2.LibBar
                                $kdocLibFooMethod
                                fun libFoo() = LibFoo()
                                $kdocLibBarMethod
                                fun libBar() = LibBar()
                            """.trimIndent()
                        )

                        dependencies {
                            implementation(multiplatformLibraryWithSources.rootCoordinate)
                            implementation(multiplatformLibraryWithoutSources.rootCoordinate)
                        }
                    }
                }
            }

            val environmentVariables = EnvironmentalVariables(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphoneos123",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("build/builtProductsDir").toString(),
            )

            build(":assembleDebugAppleFrameworkForXcodeIosArm64", environmentVariables = environmentVariables) {
                assertTasksExecuted(":assembleDebugAppleFrameworkForXcodeIosArm64")

                val headerText = projectPath
                    .resolve("build/xcode-frameworks/debug/iphoneos123/Shared.framework/Headers/Shared.h")
                    .readText()

                assert(headerText.contains(kdocLibFooClass)) {
                    "Expected Kdoc for LibFoo class found in Shared.h"
                }

                assert(headerText.contains(kdocLibBarClass).not()) {
                    "Expected no Kdoc for LibBar class in Shared.h"
                }

                assert(headerText.contains(kdocLibFooMethod)) {
                    "Expected Kdoc for libFoo method found in Shared.h"
                }

                assert(headerText.contains(kdocLibBarMethod)) {
                    "Expected Kdoc for libBar method found in Shared.h"
                }
            }
        }
    }
}