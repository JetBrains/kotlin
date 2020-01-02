/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
import org.gradle.api.tasks.wrapper.Wrapper
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.GENERATE_WRAPPER_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.KOTLIN_TARGET_FOR_IOS_DEVICE
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.KOTLIN_TARGET_FOR_WATCHOS_DEVICE
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.SYNC_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import java.io.File

/**
 * The task generates a podspec file which allows a user to
 * integrate a Kotlin/Native framework into a CocoaPods project.
 */
open class PodspecTask : DefaultTask() {

    private val specName = project.name.asValidFrameworkName()

    @OutputFile
    val outputFile: File = project.projectDir.resolve("$specName.podspec")

    @Nested
    lateinit var settings: CocoapodsExtension

    // TODO: Handle Framework name customization - rename the framework during sync process.
    @TaskAction
    fun generate() {
        val frameworkDir = project.cocoapodsBuildDirs.framework.relativeTo(outputFile.parentFile).path
        val dependencies = settings.pods.map { pod ->
            val versionSuffix = if (pod.version != null) ", '${pod.version}'" else ""
            "|    spec.dependency '${pod.name}'$versionSuffix"
        }.joinToString(separator = "\n")

        val gradleWrapper = (project.rootProject.tasks.getByName("wrapper") as? Wrapper)?.scriptFile
        require(gradleWrapper != null && gradleWrapper.exists()) {
            """
            The Gradle wrapper is required to run the build from Xcode.

            Please run the same command with `-P$GENERATE_WRAPPER_PROPERTY=true` or run the `:wrapper` task to generate the wrapper manually.

            See details about the wrapper at https://docs.gradle.org/current/userguide/gradle_wrapper.html
            """.trimIndent()
        }

        val gradleCommand = "\$REPO_ROOT/${gradleWrapper!!.toRelativeString(project.projectDir)}"
        val syncTask = "${project.path}:$SYNC_TASK_NAME"


        outputFile.writeText(
            """
            |Pod::Spec.new do |spec|
            |    spec.name                     = '$specName'
            |    spec.version                  = '${settings.version}'
            |    spec.homepage                 = '${settings.homepage.orEmpty()}'
            |    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
            |    spec.authors                  = '${settings.authors.orEmpty()}'
            |    spec.license                  = '${settings.license.orEmpty()}'
            |    spec.summary                  = '${settings.summary.orEmpty()}'
            |
            |    spec.static_framework         = true
            |    spec.vendored_frameworks      = "$frameworkDir/#{spec.name}.framework"
            |    spec.libraries                = "c++"
            |    spec.module_name              = "#{spec.name}_umbrella"
            |
            $dependencies
            |
            |    spec.pod_target_xcconfig = {
            |        'KOTLIN_TARGET[sdk=iphonesimulator*]' => 'ios_x64',
            |        'KOTLIN_TARGET[sdk=iphoneos*]' => '$KOTLIN_TARGET_FOR_IOS_DEVICE',
            |        'KOTLIN_TARGET[sdk=watchsimulator*]' => 'watchos_x86',
            |        'KOTLIN_TARGET[sdk=watchos*]' => '$KOTLIN_TARGET_FOR_WATCHOS_DEVICE',
            |        'KOTLIN_TARGET[sdk=appletvsimulator*]' => 'tvos_x64',
            |        'KOTLIN_TARGET[sdk=appletvos*]' => 'tvos_arm64',
            |        'KOTLIN_TARGET[sdk=macosx*]' => 'macos_x64'
            |    }
            |
            |    spec.script_phases = [
            |        {
            |            :name => 'Build $specName',
            |            :execution_position => :before_compile,
            |            :shell_path => '/bin/sh',
            |            :script => <<-SCRIPT
            |                set -ev
            |                REPO_ROOT="${'$'}PODS_TARGET_SRCROOT"
            |                "$gradleCommand" -p "${'$'}REPO_ROOT" $syncTask \
            |                    -P${KotlinCocoapodsPlugin.TARGET_PROPERTY}=${'$'}KOTLIN_TARGET \
            |                    -P${KotlinCocoapodsPlugin.CONFIGURATION_PROPERTY}=${'$'}CONFIGURATION \
            |                    -P${KotlinCocoapodsPlugin.CFLAGS_PROPERTY}="${'$'}OTHER_CFLAGS" \
            |                    -P${KotlinCocoapodsPlugin.HEADER_PATHS_PROPERTY}="${'$'}HEADER_SEARCH_PATHS" \
            |                    -P${KotlinCocoapodsPlugin.FRAMEWORK_PATHS_PROPERTY}="${'$'}FRAMEWORK_SEARCH_PATHS"
            |            SCRIPT
            |        }
            |    ]
            |end
        """.trimMargin()
        )

        logger.quiet(
            """
            Generated a podspec file at: ${outputFile.absolutePath}.
            To include it in your Xcode project, add the following dependency snippet in your Podfile:

                pod '$specName', :path => '${outputFile.parentFile.absolutePath}'

            """.trimIndent()
        )
    }
}

/**
 * Creates a dummy framework in the target directory.
 *
 * We represent a Kotlin/Native module to CocoaPods as a vendored framework.
 * CocoaPods needs access to such frameworks during installation process to obtain
 * their type (static or dynamic) and configure the Xcode project accordingly.
 * But we cannot build the real framework before installation because it may
 * depend on CocoaPods libraries which are not downloaded and built at this stage.
 * So we create a dummy static framework to allow CocoaPods install our pod correctly
 * and then replace it with the real one during a real build process.
 */
open class DummyFrameworkTask : DefaultTask() {
    @OutputDirectory
    val destinationDir = project.cocoapodsBuildDirs.framework

    @get:Input
    val frameworkName
        get() = project.name.asValidFrameworkName()

    private val frameworkDir: File
        get() = destinationDir.resolve("$frameworkName.framework")

    private fun copyResource(from: String, to: File) {
        to.parentFile.mkdirs()
        to.outputStream().use { file ->
            javaClass.getResourceAsStream(from).use { resource ->
                resource.copyTo(file)
            }
        }
    }

    private fun copyTextResource(from: String, to: File, transform: (String) -> String = { it }) {
        to.parentFile.mkdirs()
        to.printWriter().use { file ->
            javaClass.getResourceAsStream(from).reader().forEachLine {
                file.println(transform(it))
            }
        }
    }

    private fun copyFrameworkFile(relativeFrom: String, relativeTo: String = relativeFrom) =
        copyResource(
            "/cocoapods/dummy.framework/$relativeFrom",
            frameworkDir.resolve(relativeTo)
        )

    private fun copyFrameworkTextFile(
        relativeFrom: String,
        relativeTo: String = relativeFrom,
        transform: (String) -> String = { it }
    ) = copyTextResource(
        "/cocoapods/dummy.framework/$relativeFrom",
        frameworkDir.resolve(relativeTo),
        transform
    )

    @TaskAction
    fun create() {
        // Reset the destination directory
        with(destinationDir) {
            deleteRecursively()
            mkdirs()
        }

        // Copy files for the dummy framework.
        copyFrameworkFile("Info.plist")
        copyFrameworkFile("dummy", frameworkName)
        copyFrameworkFile("Headers/dummy.h")
        copyFrameworkTextFile("Modules/module.modulemap") {
            if (it == "framework module dummy {") {
                it.replace("dummy", frameworkName)
            } else {
                it
            }
        }
    }
}

/**
 * Generates a def-file for the given CocoaPods dependency.
 */
open class DefFileTask : DefaultTask() {

    @Nested
    lateinit var pod: CocoapodsExtension.CocoapodsDependency

    @get:OutputFile
    val outputFile: File
        get() = project.cocoapodsBuildDirs.defs.resolve("${pod.moduleName}.def")

    @TaskAction
    fun generate() {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            language = Objective-C
            modules = ${pod.moduleName}
        """.trimIndent()
        )
    }
}