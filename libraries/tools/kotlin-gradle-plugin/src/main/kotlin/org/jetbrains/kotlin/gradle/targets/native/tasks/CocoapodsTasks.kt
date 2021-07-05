/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.api.tasks.wrapper.Wrapper
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension
import org.jetbrains.kotlin.gradle.plugin.cocoapods.CocoapodsExtension.*
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.COCOAPODS_EXTENSION_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.GENERATE_WRAPPER_PROPERTY
import org.jetbrains.kotlin.gradle.plugin.cocoapods.KotlinCocoapodsPlugin.Companion.SYNC_TASK_NAME
import org.jetbrains.kotlin.gradle.plugin.cocoapods.asValidFrameworkName
import org.jetbrains.kotlin.gradle.plugin.cocoapods.cocoapodsBuildDirs
import java.io.File

/**
 * The task generates a podspec file which allows a user to
 * integrate a Kotlin/Native framework into a CocoaPods project.
 */
open class PodspecTask : DefaultTask() {

    @get:Input
    internal val specName = project.name.asValidFrameworkName()

    @get:OutputFile
    internal val outputFileProvider: Provider<File>
        get() = project.provider { project.file("$specName.podspec") }

    @get:Input
    internal lateinit var needPodspec: Provider<Boolean>

    @get:Nested
    val pods = project.objects.listProperty(CocoapodsDependency::class.java)

    @get:Input
    internal lateinit var version: Provider<String>

    @get:Input
    @get:Optional
    internal val homepage = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val license = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val authors = project.objects.property(String::class.java)

    @get:Input
    @get:Optional
    internal val summary = project.objects.property(String::class.java)

    @get:Input
    internal lateinit var frameworkName: Provider<String>

    @get:Nested
    internal lateinit var ios: Provider<PodspecPlatformSettings>

    @get:Nested
    internal lateinit var osx: Provider<PodspecPlatformSettings>

    @get:Nested
    internal lateinit var tvos: Provider<PodspecPlatformSettings>

    @get:Nested
    internal lateinit var watchos: Provider<PodspecPlatformSettings>

    init {
        onlyIf { needPodspec.get() }
    }

    @TaskAction
    fun generate() {

        val frameworkDir = project.cocoapodsBuildDirs.framework.relativeTo(outputFileProvider.get().parentFile).path
        val dependencies = pods.get().map { pod ->
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

        val gradleCommand = "\$REPO_ROOT/${gradleWrapper.toRelativeString(project.projectDir)}"
        val syncTask = "${project.path}:$SYNC_TASK_NAME"

        val deploymentTargets = run {
            listOf(ios, osx, tvos, watchos).map { it.get() }.filter { it.deploymentTarget != null }.joinToString("\n") {
                "|    spec.${it.name}.deployment_target = '${it.deploymentTarget}'"
            }
        }

        with(outputFileProvider.get()) {
            writeText(
                """
                |Pod::Spec.new do |spec|
                |    spec.name                     = '$specName'
                |    spec.version                  = '${version.get()}'
                |    spec.homepage                 = '${homepage.getOrEmpty()}'
                |    spec.source                   = { :git => "Not Published", :tag => "Cocoapods/#{spec.name}/#{spec.version}" }
                |    spec.authors                  = '${authors.getOrEmpty()}'
                |    spec.license                  = '${license.getOrEmpty()}'
                |    spec.summary                  = '${summary.getOrEmpty()}'
                |
                |    spec.vendored_frameworks      = "$frameworkDir/${frameworkName.get()}.framework"
                |    spec.libraries                = "c++"
                |    spec.module_name              = "#{spec.name}_umbrella"
                |
                $deploymentTargets
                |
                $dependencies
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
                |                    -P${KotlinCocoapodsPlugin.PLATFORM_PROPERTY}=${'$'}PLATFORM_NAME \
                |                    -P${KotlinCocoapodsPlugin.ARCHS_PROPERTY}="${'$'}ARCHS" \
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

            if (hasPodfileOwnOrParent(project)) {
                logger.quiet(
                    """
                    Generated a podspec file at: ${absolutePath}.
                    To include it in your Xcode project, check that the following dependency snippet exists in your Podfile:

                    pod '$specName', :path => '${parentFile.absolutePath}'

            """.trimIndent()
                )
            }

        }
    }

    fun Provider<String>.getOrEmpty() = getOrElse("")

    companion object {
        private val KotlinMultiplatformExtension?.cocoapodsExtensionOrNull: CocoapodsExtension?
            get() = (this as? ExtensionAware)?.extensions?.findByName(COCOAPODS_EXTENSION_NAME) as? CocoapodsExtension

        private fun hasPodfileOwnOrParent(project: Project): Boolean =
            if (project.rootProject == project) project.multiplatformExtensionOrNull?.cocoapodsExtensionOrNull?.podfile != null
            else project.multiplatformExtensionOrNull?.cocoapodsExtensionOrNull?.podfile != null
                    || (project.parent?.let { hasPodfileOwnOrParent(it) } ?: false)

        internal fun retrieveSpecRepos(project: Project): SpecRepos? = project.multiplatformExtensionOrNull?.cocoapodsExtensionOrNull?.specRepos
        internal fun retrievePods(project: Project): List<CocoapodsDependency>? = project.multiplatformExtensionOrNull?.cocoapodsExtensionOrNull?.podsAsTaskInput
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

    @Input
    lateinit var frameworkName: Provider<String>

    @Input
    lateinit var useDynamicFramework: Provider<Boolean>

    private val frameworkDir: File
        get() = destinationDir.resolve("${frameworkName.get()}.framework")

    private val dummyFrameworkPath: String
        get() {
            val staticOrDynamic = if (useDynamicFramework.get()) "dynamic" else "static"
            return "/cocoapods/$staticOrDynamic/dummy.framework/"
        }

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
            javaClass.getResourceAsStream(from).use {
                it.reader().forEachLine { str ->
                    file.println(transform(str))
                }
            }
        }
    }

    private fun copyFrameworkFile(relativeFrom: String, relativeTo: String = relativeFrom) =
        copyResource(
            "$dummyFrameworkPath$relativeFrom",
            frameworkDir.resolve(relativeTo)
        )

    private fun copyFrameworkTextFile(
        relativeFrom: String,
        relativeTo: String = relativeFrom,
        transform: (String) -> String = { it }
    ) = copyTextResource(
        "$dummyFrameworkPath$relativeFrom",
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
        copyFrameworkFile("dummy", frameworkName.get())
        copyFrameworkFile("Headers/placeholder.h")
        copyFrameworkTextFile("Modules/module.modulemap") {
            if (it == "framework module dummy {") {
                it.replace("dummy", frameworkName.get())
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

    @get:Nested
    lateinit var pod: Provider<CocoapodsDependency>

    @get:OutputFile
    val outputFile: File
        get() = project.cocoapodsBuildDirs.defs.resolve("${pod.get().moduleName}.def")

    @TaskAction
    fun generate() {
        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            language = Objective-C
            modules = ${pod.get().moduleName}
        """.trimIndent()
        )
    }
}