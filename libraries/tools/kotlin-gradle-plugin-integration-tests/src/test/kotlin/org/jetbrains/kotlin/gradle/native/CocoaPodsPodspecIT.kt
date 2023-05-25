/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.readText
import kotlin.test.assertEquals

@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("K/N tests with cocoapods' podspec")
@NativeGradlePluginTests
class CocoaPodsPodspecIT : KGPBaseTest() {

    private val cocoapodsSingleKtPod = "native-cocoapods-single"
    private val cocoapodsMultipleKtPods = "native-cocoapods-multiple"

    @DisplayName("Build project with a single podspec")
    @GradleTest
    fun testPodspecSingle(gradleVersion: GradleVersion) = doTestPodspec(
        cocoapodsSingleKtPod,
        gradleVersion,
        mapOf("kotlin-library" to null),
        mapOf("kotlin-library" to kotlinLibraryPodspecContent())
    )

    @DisplayName("Build project with a single podspec with custom framework name")
    @GradleTest
    fun testPodspecCustomFrameworkNameSingle(gradleVersion: GradleVersion) = doTestPodspec(
        cocoapodsSingleKtPod,
        gradleVersion,
        mapOf("kotlin-library" to "MultiplatformLibrary"),
        mapOf("kotlin-library" to kotlinLibraryPodspecContent("MultiplatformLibrary"))
    )

    @DisplayName("Build project with two podspecs")
    @GradleTest
    fun testPodspecMultiple(gradleVersion: GradleVersion) = doTestPodspec(
        cocoapodsMultipleKtPods,
        gradleVersion,
        mapOf("kotlin-library" to null, "second-library" to null),
        mapOf("kotlin-library" to kotlinLibraryPodspecContent(), "second-library" to secondLibraryPodspecContent("second_library")),
    )

    @DisplayName("Build project with two podspecs with custom framework name")
    @GradleTest
    fun testPodspecCustomFrameworkNameMultiple(gradleVersion: GradleVersion) = doTestPodspec(
        cocoapodsMultipleKtPods,
        gradleVersion,
        mapOf("kotlin-library" to "FirstMultiplatformLibrary", "second-library" to "SecondMultiplatformLibrary"),
        mapOf(
            "kotlin-library" to kotlinLibraryPodspecContent("FirstMultiplatformLibrary"),
            "second-library" to secondLibraryPodspecContent("SecondMultiplatformLibrary")
        )
    )

    private fun doTestPodspec(
        projectName: String,
        gradleVersion: GradleVersion,
        subprojectsToFrameworkNamesMap: Map<String, String?>,
        subprojectsToPodspecContentMap: Map<String, String?>,
    ) {
        nativeProject(projectName, gradleVersion) {
            for ((subproject, frameworkName) in subprojectsToFrameworkNamesMap) {
                frameworkName?.let {
                    useCustomCocoapodsFrameworkName(subproject, it)
                }

                // Check that we can generate the wrapper along with the podspec if the corresponding property specified
                build(
                    ":$subproject:podspec",
                    buildOptions = defaultBuildOptions.copy(
                        nativeOptions = defaultBuildOptions.nativeOptions.copy(
                            cocoapodsGenerateWrapper = true
                        )
                    )
                ) {
                    assertTasksExecuted(":$subproject:podspec")

                    // Check that the podspec file is correctly generated.
                    val podspecFileName = "$subproject/${subproject.normalizeCocoapadsFrameworkName}.podspec"

                    assertFileInProjectExists(podspecFileName)
                    val actualPodspecContentWithoutBlankLines = projectPath.resolve(podspecFileName).readText()
                        .lineSequence()
                        .filter { it.isNotBlank() }
                        .joinToString("\n")
                    assertEquals(subprojectsToPodspecContentMap[subproject], actualPodspecContentWithoutBlankLines)
                }
            }
        }
    }

    private fun kotlinLibraryPodspecContent(frameworkName: String? = null) = """
                Pod::Spec.new do |spec|
                    spec.name                     = 'kotlin_library'
                    spec.version                  = '1.0'
                    spec.homepage                 = 'https://github.com/JetBrains/kotlin'
                    spec.source                   = { :http=> ''}
                    spec.authors                  = ''
                    spec.license                  = ''
                    spec.summary                  = 'CocoaPods test library'
                    spec.vendored_frameworks      = 'build/cocoapods/framework/${frameworkName ?: "kotlin_library"}.framework'
                    spec.libraries                = 'c++'
                    spec.ios.deployment_target = '11.0'
                    spec.dependency 'pod_dependency', '1.0'
                    spec.dependency 'subspec_dependency/Core', '1.0'
                    spec.pod_target_xcconfig = {
                        'KOTLIN_PROJECT_PATH' => ':kotlin-library',
                        'PRODUCT_MODULE_NAME' => '${frameworkName ?: "kotlin_library"}',
                    }
                    spec.script_phases = [
                        {
                            :name => 'Build kotlin_library',
                            :execution_position => :before_compile,
                            :shell_path => '/bin/sh',
                            :script => <<-SCRIPT
                                if [ "YES" = "${'$'}OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                                  exit 0
                                fi
                                set -ev
                                REPO_ROOT="${'$'}PODS_TARGET_SRCROOT"
                                "${'$'}REPO_ROOT/../gradlew" -p "${'$'}REPO_ROOT" ${'$'}KOTLIN_PROJECT_PATH:syncFramework \
                                    -Pkotlin.native.cocoapods.platform=${'$'}PLATFORM_NAME \
                                    -Pkotlin.native.cocoapods.archs="${'$'}ARCHS" \
                                    -Pkotlin.native.cocoapods.configuration="${'$'}CONFIGURATION"
                            SCRIPT
                        }
                    ]
                end
            """.trimIndent()

    private fun secondLibraryPodspecContent(frameworkName: String? = null) = """
                Pod::Spec.new do |spec|
                    spec.name                     = 'second_library'
                    spec.version                  = '1.0'
                    spec.homepage                 = 'https://github.com/JetBrains/kotlin'
                    spec.source                   = { :http=> ''}
                    spec.authors                  = ''
                    spec.license                  = ''
                    spec.summary                  = 'CocoaPods test library'
                    spec.vendored_frameworks      = 'build/cocoapods/framework/${frameworkName ?: "second_library"}.framework'
                    spec.libraries                = 'c++'
                    spec.pod_target_xcconfig = {
                        'KOTLIN_PROJECT_PATH' => ':second-library',
                        'PRODUCT_MODULE_NAME' => '${frameworkName ?: "kotlin_library"}',
                    }
                    spec.script_phases = [
                        {
                            :name => 'Build second_library',
                            :execution_position => :before_compile,
                            :shell_path => '/bin/sh',
                            :script => <<-SCRIPT
                                if [ "YES" = "${'$'}OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED" ]; then
                                  echo "Skipping Gradle build task invocation due to OVERRIDE_KOTLIN_BUILD_IDE_SUPPORTED environment variable set to \"YES\""
                                  exit 0
                                fi
                                set -ev
                                REPO_ROOT="${'$'}PODS_TARGET_SRCROOT"
                                "${'$'}REPO_ROOT/../gradlew" -p "${'$'}REPO_ROOT" ${'$'}KOTLIN_PROJECT_PATH:syncFramework \
                                    -Pkotlin.native.cocoapods.platform=${'$'}PLATFORM_NAME \
                                    -Pkotlin.native.cocoapods.archs="${'$'}ARCHS" \
                                    -Pkotlin.native.cocoapods.configuration="${'$'}CONFIGURATION"
                            SCRIPT
                        }
                    ]
                end
            """.trimIndent()
}