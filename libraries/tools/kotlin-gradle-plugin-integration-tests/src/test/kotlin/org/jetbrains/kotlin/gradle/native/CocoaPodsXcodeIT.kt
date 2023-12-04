/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.DUMMY_FRAMEWORK_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_INSTALL_TASK_NAME
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.assertProcessRunResult
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.name
import kotlin.test.assertTrue

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("K/N tests cocoapods with xcodebuild")
@NativeGradlePluginTests
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@OptIn(EnvironmentalVariablesOverride::class)
class CocoaPodsXcodeIT : KGPBaseTest() {

    private val cocoapodsSingleKtPod = "native-cocoapods-single"
    private val cocoapodsMultipleKtPods = "native-cocoapods-multiple"
    private val templateProjectName = "native-cocoapods-template"

    private val environmentVariables = EnvironmentalVariables(cocoaPodsEnvironmentVariables())

    @BeforeAll
    fun setUp() {
        ensureCocoapodsInstalled()
    }

    @DisplayName("Checks xcodebuild for ios-app with a single framework")
    @GradleTest
    fun testXcodeUseFrameworksSingle(gradleVersion: GradleVersion) = doTestXcode(
        cocoapodsSingleKtPod,
        gradleVersion,
        ImportMode.FRAMEWORKS,
        "ios-app",
        mapOf("kotlin-library" to null)
    )

    @DisplayName("Checks xcodebuild for ios-app with a single framework with manual pod install")
    @GradleTest
    fun testXcodeUseFrameworksSingleWithManualPodInstall(gradleVersion: GradleVersion) {
        nativeProject(cocoapodsSingleKtPod, gradleVersion, environmentVariables = environmentVariables) {
            doTestXcode(
                mode = ImportMode.FRAMEWORKS,
                iosAppLocation = "ios-app",
                subprojectsToFrameworkNamesMap = mapOf("kotlin-library" to null),
                podInstall = ::manualPodInstall
            )
        }
    }

    @DisplayName("Checks xcodebuild for ios-app with a single framework with custom name")
    @GradleTest
    fun testXcodeUseFrameworksWithCustomFrameworkNameSingle(gradleVersion: GradleVersion) = doTestXcode(
        cocoapodsSingleKtPod,
        gradleVersion,
        ImportMode.FRAMEWORKS,
        "ios-app",
        mapOf("kotlin-library" to "MultiplatformLibrary")
    )

    @DisplayName("Checks xcodebuild for ios-app using modular headers with a single framework")
    @GradleTest
    fun testXcodeUseModularHeadersSingle(gradleVersion: GradleVersion) = doTestXcode(
        cocoapodsSingleKtPod,
        gradleVersion,
        ImportMode.MODULAR_HEADERS,
        "ios-app",
        mapOf("kotlin-library" to null)
    )

    @DisplayName("Checks xcodebuild for ios-app using modular headers with a single framework with custom name")
    @GradleTest
    fun testXcodeUseModularHeadersWithCustomFrameworkNameSingle(gradleVersion: GradleVersion) = doTestXcode(
        cocoapodsSingleKtPod,
        gradleVersion,
        ImportMode.MODULAR_HEADERS,
        "ios-app",
        mapOf("kotlin-library" to "MultiplatformLibrary")
    )

    @DisplayName("Checks xcodebuild for ios-app with manual pod install and static framework")
    @GradleTest
    fun testXcodeWithManualPodInstallForStaticFramework(gradleVersion: GradleVersion) {
        nativeProject(cocoapodsSingleKtPod, gradleVersion, environmentVariables = environmentVariables) {
            buildGradleKts.addFrameworkBlock("isStatic = true")

            doTestXcode(
                mode = ImportMode.MODULAR_HEADERS,
                iosAppLocation = "ios-app",
                subprojectsToFrameworkNamesMap = mapOf("kotlin-library" to "MultiplatformLibrary"),
                podInstall = ::manualPodInstall
            )
        }
    }

    @DisplayName("Checks xcodebuild for ios-app with kotlin library from root project")
    @GradleTest
    fun testXcodeBuildsWithKotlinLibraryFromRootProject(gradleVersion: GradleVersion) {

        nativeProject(templateProjectName, gradleVersion, environmentVariables = environmentVariables) {

            buildGradleKts.addCocoapodsBlock(
                """
                    framework {
                        baseName = "kotlin-library"
                    }
                    name = "kotlin-library"
                    podfile = project.file("ios-app/Podfile")
                """.trimIndent()
            )

            doTestXcode(
                ImportMode.FRAMEWORKS,
                "ios-app",
                mapOf("" to null),
            )
        }
    }

    @DisplayName("Checks xcodebuild for two libraries with framework")
    @GradleTest
    fun testXcodeUseFrameworksMultiple(gradleVersion: GradleVersion) = doTestXcode(
        cocoapodsMultipleKtPods,
        gradleVersion,
        ImportMode.FRAMEWORKS,
        null,
        mapOf("kotlin-library" to null, "second-library" to null)
    )

    @DisplayName("Checks xcodebuild for two libraries with framework and custom names")
    @GradleTest
    fun testXcodeUseFrameworksWithCustomFrameworkNameMultiple(gradleVersion: GradleVersion) = doTestXcode(
        cocoapodsMultipleKtPods,
        gradleVersion,
        ImportMode.FRAMEWORKS,
        null,
        mapOf("kotlin-library" to "FirstMultiplatformLibrary", "second-library" to "SecondMultiplatformLibrary")
    )

    @DisplayName("Checks xcodebuild for two libraries with modular headers")
    @GradleTest
    fun testXcodeUseModularHeadersMultiple(gradleVersion: GradleVersion) = doTestXcode(
        cocoapodsMultipleKtPods,
        gradleVersion,
        ImportMode.MODULAR_HEADERS,
        null,
        mapOf("kotlin-library" to null, "second-library" to null)
    )

    @DisplayName("Checks xcodebuild for two libraries with modular headers and custom names")
    @GradleTest
    fun testXcodeUseModularHeadersWithCustomFrameworkNameMultiple(gradleVersion: GradleVersion) = doTestXcode(
        cocoapodsMultipleKtPods,
        gradleVersion,
        ImportMode.MODULAR_HEADERS,
        null,
        mapOf("kotlin-library" to "FirstMultiplatformLibrary", "second-library" to "SecondMultiplatformLibrary")
    )

    private fun doTestXcode(
        projectName: String,
        gradleVersion: GradleVersion,
        mode: ImportMode,
        iosAppLocation: String?,
        subprojectsToFrameworkNamesMap: Map<String, String?>,
    ) {

        nativeProject(projectName, gradleVersion, environmentVariables = environmentVariables) {
            doTestXcode(
                mode = mode,
                iosAppLocation = iosAppLocation,
                subprojectsToFrameworkNamesMap = subprojectsToFrameworkNamesMap,
            )
        }
    }

    private fun TestProject.doTestXcode(
        mode: ImportMode,
        iosAppLocation: String?,
        subprojectsToFrameworkNamesMap: Map<String, String?>,
        arch: String = "x86_64",
        podInstall: (taskPrefix: String, iosAppPath: Path) -> Unit = ::gradlePodInstall,
    ) {

        prepareForXcodebuild()

        for ((subproject, frameworkName) in subprojectsToFrameworkNamesMap) {

            val taskPrefix = if (subproject.isNotEmpty()) ":$subproject" else ""

            // Add property with custom framework name
            frameworkName?.let {
                useCustomCocoapodsFrameworkName(subproject, it, iosAppLocation)
            }

            // Generate podspec.
            build("$taskPrefix:podspec", buildOptions = buildOptions)
            if (iosAppLocation != null) {
                // Set import mode for Podfile.
                preparePodfile(iosAppLocation, mode)

                val iosAppPath = projectPath.resolve(iosAppLocation)

                // Install pods.
                podInstall(taskPrefix, iosAppPath)

                // Run Xcode build.
                xcodebuild(
                    workspace = Paths.get("${iosAppPath.name}.xcworkspace"),
                    scheme = iosAppPath.name,
                    configuration = "Release",
                    sdk = "iphonesimulator",
                    arch = arch,
                    workingDir = iosAppPath,
                )
            }
        }
    }
}

private fun TestProject.gradlePodInstall(taskPrefix: String, @Suppress("UNUSED_PARAMETER") iosAppPath: Path) {
    build("$taskPrefix:$POD_INSTALL_TASK_NAME", buildOptions = buildOptions)
}

private fun TestProject.manualPodInstall(taskPrefix: String, iosAppPath: Path) {
    build("$taskPrefix:$DUMMY_FRAMEWORK_TASK_NAME", buildOptions = buildOptions)

    val environmentalVariables = environmentVariables.environmentalVariables.toMutableMap()
    environmentalVariables.getOrPut("LC_ALL") { "en_US.UTF-8" }

    assertProcessRunResult(
        runProcess(
            cmd = listOf("env", "pod", "install"),
            environmentVariables = environmentalVariables,
            workingDir = iosAppPath.toFile(),
        )
    ) { assertTrue(isSuccessful) }
}
