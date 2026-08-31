/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.export.ExperimentalExportDsl
import org.jetbrains.kotlin.gradle.swiftexport.ExperimentalSwiftExportDsl
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.jetbrains.kotlin.gradle.util.swiftExportEmbedAndSignEnvVariables
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("Tests for the export DSL")
@SwiftExportGradlePluginTests
@OptIn(ExperimentalExportDsl::class, ExperimentalSwiftExportDsl::class)
class ExportDslIT : KGPBaseTest() {

    @DisplayName("embedSwiftExportForXcode is registered when the Xcode integration is activated")
    @GradleTest
    fun testXcodeIntegrationRegistersEmbedTask(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    moduleName.set("Shared")
                    xcodeIntegration()
                }
            }

            assertTrue(isEmbedSwiftExportTaskRegistered())
        }
    }

    @DisplayName("embedSwiftExportForXcode is not registered when the export DSL is used without the Xcode integration")
    @GradleTest
    fun testExportDslWithoutXcodeIntegrationDoesNotRegisterEmbedTask(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    moduleName.set("Shared")
                }
            }

            assertFalse(isEmbedSwiftExportTaskRegistered())
        }
    }

    @DisplayName("The Xcode integration can be activated independently of the module configuration order")
    @GradleTest
    fun testXcodeIntegrationActivationIsOrderIndependent(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                export.swift {
                    xcodeIntegration()
                }
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    moduleName.set("Shared")
                }
            }

            assertTrue(isEmbedSwiftExportTaskRegistered())
        }
    }

    @DisplayName("embedSwiftExportForXcode fails with an actionable error outside of Xcode in an activated project")
    @GradleTest
    fun testActivatedProjectFailsOutsideOfXcode(
        gradleVersion: GradleVersion,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    xcodeIntegration()
                }
            }

            buildAndFail(":$EMBED_SWIFT_EXPORT_TASK_NAME") {
                assertOutputContains("Please run the $EMBED_SWIFT_EXPORT_TASK_NAME task from Xcode")
            }
        }
    }

    @DisplayName("embedSwiftExportForXcode executes normally in an activated project")
    @GradleTest
    fun testActivatedProjectExecutesEmbedTask(
        gradleVersion: GradleVersion,
        @TempDir testBuildDir: Path,
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "shared"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosArm64()
                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
                export.swift {
                    xcodeIntegration()
                }
            }

            build(
                ":$EMBED_SWIFT_EXPORT_TASK_NAME",
                environmentVariables = swiftExportEmbedAndSignEnvVariables(testBuildDir)
            ) {
                assertTasksExecuted(":iosArm64DebugSwiftExport")
                assertTasksExecuted(":$EMBED_SWIFT_EXPORT_TASK_NAME")
            }
        }
    }

    private fun TestProject.isEmbedSwiftExportTaskRegistered(): Boolean = buildScriptReturn {
        project.tasks.findByName(EMBED_SWIFT_EXPORT_TASK_NAME) != null
    }.buildAndReturn()

    private companion object {
        const val EMBED_SWIFT_EXPORT_TASK_NAME = "embedSwiftExportForXcode"
    }
}
