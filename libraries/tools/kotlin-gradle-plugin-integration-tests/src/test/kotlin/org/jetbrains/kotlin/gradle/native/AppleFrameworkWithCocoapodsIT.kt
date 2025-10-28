/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@OptIn(EnvironmentalVariablesOverride::class)
@DisplayName("Tests for K/N nontrivial setup with embedAdSign and cocoapods")
@NativeGradlePluginTests
class AppleFrameworkWithCocoapodsIT : KGPBaseTest() {

    @BeforeAll
    fun setUp() {
        ensureCocoapodsInstalled()
    }

    @DisplayName("KT-80641 - Build app with cocoapods depenency and EXECUTABLE_DEBUG_DYLIB_PATH defined")
    @GradleTest
    fun testXcodeEmbedAndSignWithPodDependencyAndDebugDylib(gradleVersion: GradleVersion) {
        multiProjectWithCocoapodsDependency(gradleVersion) {
            val environmentVariables = mapOf(
                "CONFIGURATION" to "debug",
                "SDK_NAME" to "iphonesimulator",
                "ARCHS" to "arm64",
                "TARGET_BUILD_DIR" to "no use",
                "FRAMEWORKS_FOLDER_PATH" to "no use",
                "BUILT_PRODUCTS_DIR" to projectPath.resolve("build/builtProductsDir").toString(),
                // Failing env variables introduced with Xcode 16
                "ENABLE_DEBUG_DYLIB" to "YES",
                "EXECUTABLE_BLANK_INJECTION_DYLIB_PATH" to "MyApp.app/__preview.dylib",
                "EXECUTABLE_DEBUG_DYLIB_INSTALL_NAME" to "@rpath/MyApp.debug.dylib",
                "EXECUTABLE_DEBUG_DYLIB_PATH" to "MyApp.app/MyApp.debug.dylib",
            ) + cocoaPodsEnvironmentVariables()

            build(
                ":assembleDebugAppleFrameworkForXcodeIosSimulatorArm64",
                environmentVariables = EnvironmentalVariables(environmentVariables)
            ) {
                assertTasksExecuted(":podSetupBuildBase64IosSimulator")
                assertTasksExecuted(":assembleDebugAppleFrameworkForXcodeIosSimulatorArm64")
            }
        }
    }

    private fun multiProjectWithCocoapodsDependency(
        gradleVersion: GradleVersion,
        test: TestProject.() -> Unit = {}
    ) {
        project("empty", gradleVersion) {
            plugins {
                kotlin("multiplatform")
                kotlin("native.cocoapods")
            }
            settingsBuildScriptInjection {
                settings.rootProject.name = "app"
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    listOf(
                        @Suppress("DEPRECATION") // fixme: KT-81704 Cleanup tests after apple x64 family deprecation
                        iosX64(),
                        iosArm64(),
                        iosSimulatorArm64()
                    ).forEach {
                        it.binaries.framework {
                            baseName = "MyApp"
                            isStatic = true
                        }
                    }

                    with(cocoapods) {
                        version = "1.0.0"
                        ios.deploymentTarget = "16.0"
                        noPodspec()

                        pod("Base64") {
                            version = "1.1.2"
                        }
                    }

                    sourceSets.commonMain.get().compileStubSourceWithSourceSetName()
                }
            }

            test(this)
        }
    }
}