/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class, EnvironmentalVariablesOverride::class)

package org.jetbrains.kotlin.gradle.apple

import org.gradle.kotlin.dsl.kotlin
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.uklibs.applyMultiplatform
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.absolutePathString
import kotlin.io.path.writeText
import kotlin.test.assertContains

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@RequiredXCodeVersion(minSupportedMajor = 27, minSupportedMinor = 2)
@SwiftPMImportGradlePluginTests
@DisplayName("Xcode JSON project end-to-end integration")
class XcprojIntegrationIT : KGPBaseTest() {
    @GradleTest
    fun `wizard project builds after integrating embedAndSign and SwiftPM package`(version: GradleVersion) {
        // This fixture comes from the Xcode 27.2 wizard
        project("emptyxcproj", version) {
            val xcodeProject = projectPath.resolve("iosApp/iosApp.xcodeproj")

            val localPackagePath = "localSwiftPackage"
            createLocalSwiftPackage(projectPath.resolve(localPackagePath))
            plugins {
                kotlin("multiplatform")
            }
            buildScriptInjection {
                project.applyMultiplatform {
                    iosSimulatorArm64().binaries.framework {
                        isStatic = true
                        baseName = "Shared"
                    }
                    swiftPMDependencies {
                        localSwiftPackage(
                            directory = project.layout.projectDirectory.dir(localPackagePath),
                            products = listOf("LocalSwiftPackage"),
                        )
                    }

                    sourceSets.commonMain.get().compileSource(
                        """
                        import swiftPMImport.emptyxcproj.LocalHelper
    
                        class Greeting {
                            @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
                            fun message(): String = LocalHelper.greeting()
                        }
                        """.trimIndent()
                    )
                }
            }

            val swiftCompilationSanityCheck = "swiftCompilationSanityCheck"
            projectPath.resolve("iosApp/iosApp/ContentView.swift").writeText(
                """
                    import SwiftUI
                    import Shared
                    import LocalSwiftPackage

                    #warning("$swiftCompilationSanityCheck")
                    struct ContentView: View {
                        var body: some View {
                            VStack {
                                Text(Greeting().message())
                                Text(LocalHelper.greeting())
                            }
                        }
                    }
                """.trimIndent()
            )

            build(
                ":integrateEmbedAndSign", ":integrateLinkagePackage",
                environmentVariables = EnvironmentalVariables(
                    "XCODEPROJ_PATH" to "iosApp/iosApp.xcodeproj",
                    "GRADLEW_PATH" to projectPath.resolve("gradlew").absolutePathString(),
                    "GRADLE_PROJECT_PATH" to ":",
                ),
            )

            assertContains(
                buildXcodeProject(
                    xcodeproj = xcodeProject,
                    buildSettingOverrides = mapOf(
                        "ARCHS" to "arm64",
                    )
                ).output,
                swiftCompilationSanityCheck,
            )
        }
    }
}
