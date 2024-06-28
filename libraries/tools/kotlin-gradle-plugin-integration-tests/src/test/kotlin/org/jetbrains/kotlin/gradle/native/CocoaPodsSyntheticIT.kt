/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.native

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.OS
import kotlin.io.path.appendText
import kotlin.test.assertTrue

@OptIn(EnvironmentalVariablesOverride::class)
@OsCondition(supportedOn = [OS.MAC], enabledOnCI = [OS.MAC])
@DisplayName("K/N tests with synthetic cocoapods")
@GradleTestVersions(minVersion = TestVersions.Gradle.G_7_0)
@NativeGradlePluginTests
class CocoaPodsSyntheticIT : KGPBaseTest() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        nativeOptions = super.defaultBuildOptions.nativeOptions.copy(
            cocoapodsGenerateWrapper = true
        )
    )

    private val cocoapodsSingleKtPod = "native-cocoapods-single"
    private val templateProjectName = "native-cocoapods-template"

    private val defaultPodInstallSyntheticTaskName = ":podInstallSyntheticIos"

    private val environmentVariables = EnvironmentalVariables(cocoaPodsEnvironmentVariables())

    @BeforeAll
    fun setUp() {
        ensureCocoapodsInstalled()
    }

    @DisplayName("Synthetic project podfile generation")
    @GradleTest
    fun testSyntheticProjectPodfileGeneration(gradleVersion: GradleVersion) {
        nativeProject(cocoapodsSingleKtPod, gradleVersion, environmentVariables = environmentVariables) {
            buildGradleKts.addCocoapodsBlock(
                """
                ios.deploymentTarget = "14.1"
                pod("SSZipArchive")
                pod("AFNetworking", "~> 4.0.1")
                pod("Alamofire") {
                    source = git("https://github.com/Alamofire/Alamofire.git") {
                        tag = "5.6.1"
                    }
                }
                """.trimIndent()
            )
            build(defaultPodInstallSyntheticTaskName) {
                assertTasksExecuted(":podGenIos")

                val podfile = "build/cocoapods/synthetic/ios/Podfile"
                assertFileInProjectContains(
                    podfile,
                    "platform :ios, '14.1'",
                    "pod 'SSZipArchive'",
                    "pod 'AFNetworking', '~> 4.0.1'",
                    "pod 'Alamofire', :git => 'https://github.com/Alamofire/Alamofire.git', :tag => '5.6.1'",
                    "config.build_settings['EXPANDED_CODE_SIGN_IDENTITY'] = \"\"",
                    "config.build_settings['CODE_SIGNING_REQUIRED'] = \"NO\"",
                    "config.build_settings['CODE_SIGNING_ALLOWED'] = \"NO\""
                )
            }
        }
    }

    @DisplayName("Synthetic project podfile postprocessing")
    @GradleTest
    fun testSyntheticProjectPodfilePostprocessing(gradleVersion: GradleVersion) {
        nativeProject(templateProjectName, gradleVersion, environmentVariables = environmentVariables) {
            preparePodfile("ios-app", ImportMode.FRAMEWORKS)
            buildGradleKts.addCocoapodsBlock("""pod("ChatSDK", version = "5.2.1")""")
            buildGradleKts.appendText(
                """
                
                tasks.withType<org.jetbrains.kotlin.gradle.targets.native.tasks.PodGenTask>().configureEach {
                    doLast {
                        podfile.get().appendText("ENV['SWIFT_VERSION'] = '5'")
                    }
                }
                """.trimIndent()
            )

            build(defaultPodInstallSyntheticTaskName) {
                assertFileInProjectContains("build/cocoapods/synthetic/ios/Podfile", "ENV['SWIFT_VERSION'] = '5'")
            }
        }
    }

    @DisplayName("Reinstalling pod invalidates UTD")
    @GradleTest
    fun testPodInstallInvalidatesUTD(gradleVersion: GradleVersion) {
        nativeProject(templateProjectName, gradleVersion, environmentVariables = environmentVariables) {
            preparePodfile("ios-app", ImportMode.FRAMEWORKS)
            buildGradleKts.addPod("AFNetworking")

            build(defaultPodInstallSyntheticTaskName) {
                assertTasksExecuted(defaultPodInstallSyntheticTaskName)
                assertTrue { projectPath.resolve("build/cocoapods/synthetic/ios/Pods/AFNetworking").toFile().deleteRecursively() }
            }

            build(defaultPodInstallSyntheticTaskName) {
                assertTasksExecuted(defaultPodInstallSyntheticTaskName)
            }
        }
    }
}