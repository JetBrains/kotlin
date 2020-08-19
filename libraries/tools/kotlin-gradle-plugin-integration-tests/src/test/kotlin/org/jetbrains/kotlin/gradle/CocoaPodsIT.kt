/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_BUILD_DEPENDENCIES_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_GEN_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.POD_SETUP_BUILD_TASK_NAME
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.fail

class CocoaPodsIT : BaseGradleIT() {

    override val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.FOR_MPP_SUPPORT


    // We use Kotlin DSL. Earlier Gradle versions fail at accessors codegen.
    val gradleVersion = GradleVersionRequired.FOR_MPP_SUPPORT

    val PODFILE_IMPORT_DIRECTIVE_PLACEHOLDER = "<import_mode_directive>"

    val cocoapodsSingleKtPod = "new-mpp-cocoapods-single"
    val cocoapodsMultipleKtPods = "new-mpp-cocoapods-multiple"

    @Test
    fun testPodspecSingle() = doTestPodspec(
        cocoapodsSingleKtPod,
        mapOf("kotlin-library" to null),
        mapOf("kotlin-library" to kotlinLibraryPodspecContent())
    )

    @Test
    fun testPodspecCustomFrameworkNameSingle() = doTestPodspec(
        cocoapodsSingleKtPod,
        mapOf("kotlin-library" to "MultiplatformLibrary"),
        mapOf("kotlin-library" to kotlinLibraryPodspecContent("MultiplatformLibrary"))
    )

    @Test
    fun testXcodeUseFrameworksSingle() = doTestXcode(
        cocoapodsSingleKtPod,
        ImportMode.FRAMEWORKS,
        "ios-app", mapOf("kotlin-library" to null)
    )

    @Test
    fun testXcodeUseFrameworksWithCustomFrameworkNameSingle() = doTestXcode(
        cocoapodsSingleKtPod,
        ImportMode.FRAMEWORKS,
        "ios-app",
        mapOf("kotlin-library" to "MultiplatformLibrary")
    )

    @Test
    fun testXcodeUseModularHeadersSingle() = doTestXcode(
        cocoapodsSingleKtPod,
        ImportMode.MODULAR_HEADERS,
        "ios-app",
        mapOf("kotlin-library" to null)
    )

    @Test
    fun testXcodeUseModularHeadersWithCustomFrameworkNameSingle() = doTestXcode(
        cocoapodsSingleKtPod,
        ImportMode.MODULAR_HEADERS,
        "ios-app",
        mapOf("kotlin-library" to "MultiplatformLibrary")
    )

    @Test
    fun testPodImportUseFrameworksSingle() = doTestPodImport(
        cocoapodsSingleKtPod,
        "ios-app",
        ImportMode.FRAMEWORKS,
        mapOf("kotlin-library" to null)
    )

    @Test
    fun testPodImportUseModularHeadersSingle() =
        doTestPodImport(
            cocoapodsSingleKtPod,
            "ios-app",
            ImportMode.MODULAR_HEADERS,
            mapOf("kotlin-library" to null)
        )

    @Test
    fun testPodspecMupltiple() = doTestPodspec(
        cocoapodsMultipleKtPods,
        mapOf("kotlin-library" to null, "second-library" to null),
        mapOf("kotlin-library" to kotlinLibraryPodspecContent(), "second-library" to secondLibraryPodspecContent()),
    )

    @Test
    fun testPodspecCustomFrameworkNameMupltiple() = doTestPodspec(
        cocoapodsMultipleKtPods,
        mapOf("kotlin-library" to "FirstMultiplatformLibrary", "second-library" to "SecondMultiplatformLibrary"),
        mapOf(
            "kotlin-library" to kotlinLibraryPodspecContent("FirstMultiplatformLibrary"),
            "second-library" to secondLibraryPodspecContent("SecondMultiplatformLibrary")
        )
    )

    @Test
    fun testXcodeUseFrameworksMupltiple() = doTestXcode(
        cocoapodsMultipleKtPods,
        ImportMode.FRAMEWORKS,
        "ios-app",
        mapOf("kotlin-library" to null, "second-library" to null)
    )

    @Test
    fun testXcodeUseFrameworksWithCustomFrameworkNameMupltiple() = doTestXcode(
        cocoapodsMultipleKtPods,
        ImportMode.FRAMEWORKS,
        "ios-app",
        mapOf("kotlin-library" to "FirstMultiplatformLibrary", "second-library" to "SecondMultiplatformLibrary")
    )

    @Test
    fun testXcodeUseModularHeadersMupltiple() = doTestXcode(
        cocoapodsMultipleKtPods,
        ImportMode.MODULAR_HEADERS,
        "ios-app",
        mapOf("kotlin-library" to null, "second-library" to null)
    )

    @Test
    fun testXcodeUseModularHeadersWithCustomFrameworkNameMupltiple() = doTestXcode(
        cocoapodsMultipleKtPods,
        ImportMode.MODULAR_HEADERS,
        "ios-app",
        mapOf("kotlin-library" to "FirstMultiplatformLibrary", "second-library" to "SecondMultiplatformLibrary")
    )

    @Test
    fun testPodImportUseFrameworksMupltiple() = doTestPodImport(
        cocoapodsMultipleKtPods,
        "ios-app",
        ImportMode.FRAMEWORKS,
        mapOf("kotlin-library" to null, "second-library" to null)
    )

    @Test
    fun testPodImportUseModularHeadersMupltiple() =
        doTestPodImport(
            cocoapodsMultipleKtPods,
            "ios-app",
            ImportMode.MODULAR_HEADERS,
            mapOf("kotlin-library" to null, "second-library" to null)
        )

    @Test
    fun testPodImportCustomFrameworkName() = doTestPodImport(
        cocoapodsSingleKtPod,
        "ios-app",
        ImportMode.FRAMEWORKS,
        mapOf("kotlin-library" to "foobaz")
    )

    @Test
    fun testSkippingTasksOnOtherHosts() {
        assumeFalse(HostManager.hostIsMac)

        with(transformProjectWithPluginsDsl(cocoapodsSingleKtPod, gradleVersion)) {
            val syntheticTasks = arrayOf(
                ":podInstall",
                ":kotlin-library:generateDummyFramework",
                ":kotlin-library:podGenIOS",
                ":kotlin-library:podSetupBuildIOS",
                ":kotlin-library:podBuildDependenciesIOS",
                ":kotlin-library:podImport"
            )

            build(*syntheticTasks, "-Pkotlin.native.cocoapods.generate.wrapper=true") {
                // Check that tasks working with synthetic projects are skipped on non-mac hosts.
                assertSuccessful()
                assertTasksSkipped(*syntheticTasks)
            }
        }
    }

    private fun BaseGradleIT.Project.useCustomFrameworkName(subproject: String, frameworkName: String, iosAppLocation: String? = null) {
        // Change the name at the Gradle side.
        gradleBuildScript(subproject).appendText(
            """
                |kotlin {
                |    cocoapods {
                |        frameworkName = "$frameworkName"
                |    }
                |}
            """.trimMargin()
        )

        // Change swift sources import if needed.
        if (iosAppLocation != null) {
            val iosAppDir = projectDir.resolve(iosAppLocation)
            iosAppDir.resolve("ios-app/ViewController.swift").modify {
                it.replace("import ${subproject.validFrameworkName}", "import $frameworkName")
            }
        }
    }

    private fun doTestPodspec(
        projectName: String,
        subprojectsToFrameworkNamesMap: Map<String, String?>,
        subprojectsToPodspecContentMap: Map<String, String?>
    ) {
        assumeTrue(HostManager.hostIsMac)
        val gradleProject = transformProjectWithPluginsDsl(projectName, gradleVersion)

        gradleProject.build(":podspec") {
            assertSuccessful()
            assertTasksSkipped(":podspec")
            assertNoSuchFile("cocoapods.podspec")
        }

        for ((subproject, frameworkName) in subprojectsToFrameworkNamesMap) {
            frameworkName?.let {
                gradleProject.useCustomFrameworkName(subproject, it)
            }

            // Check that we can generate the wrapper along with the podspec if the corresponding property specified
            gradleProject.build(":$subproject:podspec", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
                assertSuccessful()
                assertTasksExecuted(":$subproject:podspec")

                // Check that the podspec file is correctly generated.
                val podspecFileName = "$subproject/${subproject.validFrameworkName}.podspec"

                assertFileExists(podspecFileName)
                val actualPodspecContentWithoutBlankLines = fileInWorkingDir(podspecFileName).readText()
                    .lineSequence()
                    .filter { it.isNotBlank() }
                    .joinToString("\n")

                assertEquals(subprojectsToPodspecContentMap[subproject], actualPodspecContentWithoutBlankLines)
            }
        }
    }

    private fun doTestPodImport(
        projectName: String,
        iosAppLocation: String,
        mode: ImportMode,
        subprojectsToFrameworkNamesMap: Map<String, String?>,
    ) {
        assumeTrue(HostManager.hostIsMac)
        assumeTrue(KotlinCocoapodsPlugin.isAvailableToProduceSynthetic)

        val subprojects = subprojectsToFrameworkNamesMap.keys
        val gradleProject = transformProjectWithPluginsDsl(projectName, gradleVersion)

        subprojectsToFrameworkNamesMap.forEach { subproject, frameworkName ->
            frameworkName?.let {
                gradleProject.useCustomFrameworkName(subproject, it, iosAppLocation)
            }
        }

        with(gradleProject) {
            preparePodfile(iosAppLocation, mode)
            podImportAsserts()
            subprojects.forEach { podImportAsserts(it) }
        }
    }

    private fun BaseGradleIT.Project.podImportAsserts(subproject: String? = null) {
        val buildScriptText = gradleBuildScript(subproject).readText()
        val taskPrefix = subproject?.let { ":$it" } ?: ""
        val podImport = "podImport"
        val podspec = "podspec"
        val podInstall = "podInstall"

        build("$taskPrefix:$podImport", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
            assertSuccessful()

            if ("noPodspec()" in buildScriptText) {
                assertTasksSkipped("$taskPrefix:$podspec")
            }

            if ("podfile" in buildScriptText) {
                assertTasksExecuted("$taskPrefix:$podInstall")
            } else {
                assertTasksSkipped("$taskPrefix:$podInstall")
            }
            assertTasksRegisteredByPrefix(listOf("$taskPrefix:$POD_GEN_TASK_NAME"))
            if (buildScriptText.matches("pod\\(.*\\)".toRegex())) {
                assertTasksExecutedByPrefix(listOf("$taskPrefix:$POD_GEN_TASK_NAME"))
            }

            with(listOf(POD_SETUP_BUILD_TASK_NAME, POD_BUILD_DEPENDENCIES_TASK_NAME).map { "$taskPrefix:$it" }) {
                if (buildScriptText.matches("pod\\(.*\\)".toRegex())) {
                    assertTasksRegisteredByPrefix(this)
                    assertTasksExecutedByPrefix(this)
                }
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

    private fun doTestXcode(
        projectName: String,
        mode: ImportMode,
        iosAppLocation: String,
        subprojectsToFrameworkNamesMap: Map<String, String?>
    ) {
        assumeTrue(HostManager.hostIsMac)
        val gradleProject = transformProjectWithPluginsDsl(projectName, gradleVersion)

        with(gradleProject) {
            setupWorkingDir()

            for ((subproject, frameworkName) in subprojectsToFrameworkNamesMap) {

                // Add property with custom framework name
                frameworkName?.let {
                    useCustomFrameworkName(subproject, it, iosAppLocation)
                }

                // Generate podspec.
                gradleProject.build(":$subproject:podspec", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
                    assertSuccessful()
                }
            }

            val iosAppDir = projectDir.resolve(iosAppLocation)

            // Set import mode for Podfile.
            iosAppDir.resolve("Podfile").modify {
                it.replace(PODFILE_IMPORT_DIRECTIVE_PLACEHOLDER, mode.directive)
            }

            // Install pods.
            gradleProject.build(":podInstall", "-Pkotlin.native.cocoapods.generate.wrapper=true") {
                assertSuccessful()
            }

            // Run Xcode build.
            runCommand(
                iosAppDir, "xcodebuild",
                "-sdk", "iphonesimulator",
                "-arch", "arm64",
                "-configuration", "Release",
                "-workspace", "${iosAppDir.name}.xcworkspace",
                "-scheme", iosAppDir.name,
                inheritIO = true // Xcode doesn't finish the process if the PIPE redirect is used.
            ) {
                assertEquals(
                    0, exitCode, """
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

    private fun Project.preparePodfile(iosAppLocation: String, mode: ImportMode) {
        val iosAppDir = projectDir.resolve(iosAppLocation)

        // Set import mode for Podfile.
        iosAppDir.resolve("Podfile").modify {
            it.replace(PODFILE_IMPORT_DIRECTIVE_PLACEHOLDER, mode.directive)
        }
    }

    private val String.validFrameworkName: String
        get() = replace('-', '_')

    private fun kotlinLibraryPodspecContent(frameworkName: String? = null) = """
                Pod::Spec.new do |spec|
                    spec.name                     = 'kotlin_library'
                    spec.version                  = '1.0'
                    spec.homepage                 = 'https://github.com/JetBrains/kotlin'
                    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
                    spec.authors                  = ''
                    spec.license                  = ''
                    spec.summary                  = 'CocoaPods test library'
                    spec.static_framework         = true
                    spec.vendored_frameworks      = "build/cocoapods/framework/${frameworkName ?: "kotlin_library"}.framework"
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

    private fun secondLibraryPodspecContent(frameworkName: String? = null) = """
                Pod::Spec.new do |spec|
                    spec.name                     = 'second_library'
                    spec.version                  = '1.0'
                    spec.homepage                 = 'https://github.com/JetBrains/kotlin'
                    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
                    spec.authors                  = ''
                    spec.license                  = ''
                    spec.summary                  = 'CocoaPods test library'
                    spec.static_framework         = true
                    spec.vendored_frameworks      = "build/cocoapods/framework/${frameworkName ?: "second_library"}.framework"
                    spec.libraries                = "c++"
                    spec.module_name              = "#{spec.name}_umbrella"
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
                            :name => 'Build second_library',
                            :execution_position => :before_compile,
                            :shell_path => '/bin/sh',
                            :script => <<-SCRIPT
                                set -ev
                                REPO_ROOT="${'$'}PODS_TARGET_SRCROOT"
                                "${'$'}REPO_ROOT/../gradlew" -p "${'$'}REPO_ROOT" :second-library:syncFramework \
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

}