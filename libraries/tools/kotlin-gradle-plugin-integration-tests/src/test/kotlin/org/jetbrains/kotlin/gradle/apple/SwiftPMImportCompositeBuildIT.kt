/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.EnvironmentalVariablesOverride
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OsCondition
import org.jetbrains.kotlin.gradle.testbase.SwiftPMImportGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildAndFail
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.compileSource
import org.jetbrains.kotlin.gradle.testbase.plugins
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.settingsBuildScriptInjection
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.uklibs.includeBuild
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.writeText
import kotlin.test.assertEquals

@OsCondition(
    supportedOn = [OS.MAC],
    enabledOnCI = [OS.MAC],
)
@OptIn(EnvironmentalVariablesOverride::class)
@SwiftPMImportGradlePluginTests
class SwiftPMImportCompositeBuildIT : KGPBaseTest() {

    @GradleTest
    fun `KT-84736 - composite build - uses full build tree paths`(version: GradleVersion) {
        val left = project("empty", version) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "left"
            }
            plugins {
                kotlin("multiplatform")
            }
            createLocalSwiftPackage(
                projectPath.resolve("Left"),
                packageName = "Left",
            )
            projectPath.resolve("Left/Sources/Left/Left.swift").writeText(
                swiftSourceContent("Left")
            )

            buildScriptInjection {
                project.group = "test"
                project.version = "1.0"
                project.applyMultiplatform {
                    iosArm64()

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir("Left"),
                            products = listOf("Left"),
                        )
                    }

                    sourceSets.commonMain.get().compileSource(
                        """
                            package left

                            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                            fun greeting(): String = swiftPMImport.test.left.Left.greeting()
                        """.trimIndent()
                    )
                }
            }
        }
        val right = project("empty", version) {
            settingsBuildScriptInjection {
                settings.rootProject.name = "right"
            }
            plugins {
                kotlin("multiplatform")
            }
            createLocalSwiftPackage(
                projectPath.resolve("Right"),
                packageName = "Right",
            )
            projectPath.resolve("Right/Sources/Right/Right.swift").writeText(
                swiftSourceContent("Right")
            )

            buildScriptInjection {
                project.group = "test"
                project.version = "1.0"
                project.applyMultiplatform {
                    iosArm64()

                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir("Right"),
                            products = listOf("Right"),
                        )
                    }

                    sourceSets.commonMain.get().compileSource(
                        """
                            package right

                            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                            fun greeting(): String = swiftPMImport.test.right.Right.greeting()
                        """.trimIndent()
                    )
                }
            }
        }

        project("empty", version) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64().binaries.framework {
                        isStatic = false
                    }
                    sourceSets.commonMain.dependencies {
                        implementation("test:left:1.0")
                        implementation("test:right:1.0")
                    }
                    sourceSets.commonMain.get().compileSource(
                        """
                            fun greeting(): String = left.greeting() + right.greeting()
                        """.trimIndent()
                    )
                }
            }

            includeBuild(left) { name = "left" }
            includeBuild(right) { name = "right" }

            // Check that we see symbols from left and right at link time
            build("linkDebugFrameworkIosArm64")

            assertEquals(
                listOf("_"),
                describeSwiftPackage(left.projectPath.resolve(".swiftpm-locks/default/swiftImport")).dependencies.map { it.identity },
            )
            assertEquals(
                listOf("_", "_left", "_right"),
                describeSwiftPackage(projectPath.resolve(".swiftpm-locks/default/swiftImport")).dependencies.map { it.identity },
            )
        }
    }
}
