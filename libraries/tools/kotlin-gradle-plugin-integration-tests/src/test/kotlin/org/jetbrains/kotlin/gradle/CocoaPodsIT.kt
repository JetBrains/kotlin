/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.fail

class CocoaPodsIT : BaseGradleIT() {

    // We use Kotlin DSL. Earlier Gradle versions fail at accessors codegen.
    val gradleVersion = GradleVersionRequired.None

    val PODFILE_IMPORT_DIRECTIVE_PLACEHOLDER = "<import_mode_directive>"

    @Test
    fun testPodspec() {
        assumeTrue(HostManager.hostIsMac)
        val gradleProject = transformProjectWithPluginsDsl("new-mpp-cocoapods", gradleVersion)

        // Check that the podspec task fails if there is no Gradle wrapper in the project.
        gradleProject.build(":kotlin-library:podspec") {
            assertFailed()
            assertContains("The Gradle wrapper is required to run the build from Xcode.")
            assertContains("Please run the same command with `-Pkotlin.native.cocoapods.generate.wrapper=true` " +
                                   "or run the `:wrapper` task to generate the wrapper manually.")
        }

        // Check that we can generate the wrapper along with the podspec if the corresponding property specified
        gradleProject.build(":kotlin-library:podspec", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
            assertSuccessful()
            assertTasksExecuted(":kotlin-library:podspec", ":wrapper")
            assertFileExists("gradlew")

            // Check that the podspec file is correctly generated.
            val podspecFileName = "kotlin-library/kotlin_library.podspec"
            val expectedPodspecContent = """
                Pod::Spec.new do |spec|
                    spec.name                     = 'kotlin_library'
                    spec.version                  = '1.0'
                    spec.homepage                 = 'https://github.com/JetBrains/kotlin'
                    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
                    spec.authors                  = ''
                    spec.license                  = ''
                    spec.summary                  = 'CocoaPods test library'

                    spec.static_framework         = true
                    spec.vendored_frameworks      = "build/cocoapods/framework/#{spec.name}.framework"
                    spec.libraries                = "c++"
                    spec.module_name              = "#{spec.name}_umbrella"

                    spec.dependency 'pod_dependency', '1.0'
                    spec.dependency 'subspec_dependency/Core', '1.0'

                    spec.pod_target_xcconfig = {
                        'KOTLIN_TARGET[sdk=iphonesimulator*]' => 'ios_x64',
                        'KOTLIN_TARGET[sdk=iphoneos*]' => 'ios_arm',
                        'KOTLIN_TARGET[sdk=watchsimulator*]' => 'watchos_x86',
                        'KOTLIN_TARGET[sdk=watchos*]' => 'watchos_arm',
                        'KOTLIN_TARGET[sdk=appletvsimulator*]' => 'tvos_x64',
                        'KOTLIN_TARGET[sdk=appletvos*]' => 'tvos_arm64',
                        'KOTLIN_TARGET[sdk=macosx*]' => 'macos_x64'
                    }

                    spec.script_phases = [
                        {
                            :name => 'Build kotlin_library',
                            :execution_position => :before_compile,
                            :shell_path => '/bin/sh',
                            :script => <<-SCRIPT
                                set -ev
                                REPO_ROOT="${'$'}PODS_TARGET_SRCROOT"
                                "${'$'}REPO_ROOT/../gradlew" -p "${'$'}REPO_ROOT" :kotlin-library:syncFramework \
                                    -Pkotlin.native.cocoapods.target=${'$'}KOTLIN_TARGET \
                                    -Pkotlin.native.cocoapods.configuration=${'$'}CONFIGURATION \
                                    -Pkotlin.native.cocoapods.cflags="${'$'}OTHER_CFLAGS" \
                                    -Pkotlin.native.cocoapods.paths.headers="${'$'}HEADER_SEARCH_PATHS" \
                                    -Pkotlin.native.cocoapods.paths.frameworks="${'$'}FRAMEWORK_SEARCH_PATHS"
                            SCRIPT
                        }
                    ]
                end
            """.trimIndent()

            assertFileExists(podspecFileName)
            assertEquals(expectedPodspecContent, fileInWorkingDir(podspecFileName).readText())
        }
    }

    @Test
    fun testInterop() {
        assumeTrue(HostManager.hostIsMac)
        val gradleProject = transformProjectWithPluginsDsl("new-mpp-cocoapods", gradleVersion)
        with(gradleProject) {
            // Check that a project with CocoaPods interop fails to be built from command line.
            build(":kotlin-library:build") {
                assertFailed()
                assertContains("Cannot perform cinterop processing for module pod_dependency: cannot determine headers location.")
            }

            // Check that a project without CocoaPods interop can be built from command line.
            gradleBuildScript("kotlin-library").modify {
                it.replace("""pod("pod_dependency", "1.0")""", "").replace("""pod("subspec_dependency/Core", "1.0")""", "")
            }
            projectDir.resolve("kotlin-library/src/iosMain/kotlin/A.kt").modify {
                it.replace("import cocoapods.pod_dependency.*", "").replace("println(foo())", "")
                    .replace("import cocoapods.subspec_dependency.*", "").replace("println(baz())", "")
            }
            build(":kotlin-library:linkReleaseFrameworkIOS") {
                assertSuccessful()
            }
        }
    }

    private enum class ImportMode(val directive: String) {
        FRAMEWORKS("use_frameworks!"),
        MODULAR_HEADERS("use_modular_headers!")
    }

    private data class CommandResult(
        val exitCode: Int,
        val stdOut: String,
        val stdErr: String
    )

    private fun runCommand(
        workingDir: File,
        command: String,
        vararg args: String,
        timeoutSec: Long = 120,
        inheritIO: Boolean = false,
        block: CommandResult.() -> Unit
    ) {
        val process = ProcessBuilder(command, *args).apply {
            directory(workingDir)
            if (inheritIO) {
                inheritIO()
            }
        }.start()

        val isFinished = process.waitFor(timeoutSec, TimeUnit.SECONDS)
        val stdOut = process.inputStream.bufferedReader().use { it.readText() }
        val stdErr = process.errorStream.bufferedReader().use { it.readText() }

        if (!isFinished) {
            process.destroyForcibly()
            println("Stdout:\n$stdOut")
            println("Stderr:\n$stdErr")
            fail("Command '$command ${args.joinToString(" ")}' killed by timeout.".trimIndent())
        }
        CommandResult(process.exitValue(), stdOut, stdErr).block()
    }

    private fun doTestXcode(mode: ImportMode) {
        assumeTrue(HostManager.hostIsMac)
        val gradleProject = transformProjectWithPluginsDsl("new-mpp-cocoapods", gradleVersion)

        with(gradleProject) {
            // Generate podspec.
            gradleProject.build(":kotlin-library:podspec", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
                assertSuccessful()
            }

            val iosAppDir = projectDir.resolve("ios-app")

            // Set import mode for Podfile.
            iosAppDir.resolve("Podfile").modify {
                it.replace(PODFILE_IMPORT_DIRECTIVE_PLACEHOLDER, mode.directive)
            }

            // Install pods.
            runCommand(iosAppDir, "pod", "install") {
                assertEquals(0, exitCode,  """
                        |Exit code mismatch for `pod install`.
                        |stdout:
                        |$stdOut
                        |
                        |stderr:
                        |$stdErr
                    """.trimMargin()
                )
            }

            // Run Xcode build.
            runCommand(iosAppDir, "xcodebuild",
                       "-sdk", "iphonesimulator",
                       "-arch", "arm64",
                       "-configuration", "Release",
                       "-workspace", "ios-app.xcworkspace",
                       "-scheme", "ios-app",
                       inheritIO = true // Xcode doesn't finish the process if the PIPE redirect is used.
            ) {
                assertEquals(0, exitCode, """
                        |Exit code mismatch for `xcodebuild`.
                        |stdout:
                        |$stdOut
                        |
                        |stderr:
                        |$stdErr
                    """.trimMargin()
                )
            }
        }
    }

    @Test
    fun testXcodeUseFrameworks() = doTestXcode(ImportMode.FRAMEWORKS)

    @Test
    fun testXcodeUseModularHeaders() = doTestXcode(ImportMode.MODULAR_HEADERS)
}