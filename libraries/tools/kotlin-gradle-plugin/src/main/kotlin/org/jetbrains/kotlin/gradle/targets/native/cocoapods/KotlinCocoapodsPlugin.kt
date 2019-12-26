/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.cocoapods

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.tasks.*
import org.jetbrains.kotlin.gradle.utils.asValidTaskName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

internal val Project.cocoapodsBuildDirs: CocoapodsBuildDirs
    get() = CocoapodsBuildDirs(this)

internal class CocoapodsBuildDirs(val project: Project) {
    val root: File
        get() = project.buildDir.resolve("cocoapods")

    val framework: File
        get() = root.resolve("framework")

    val defs: File
        get() = root.resolve("defs")

    fun fatFramework(buildType: String) =
        root.resolve("fat-frameworks/${buildType.toLowerCase()}")
}

internal fun String.asValidFrameworkName() = replace('-', '_')

open class KotlinCocoapodsPlugin : Plugin<Project> {

    private fun KotlinMultiplatformExtension.supportedTargets() = targets
        .withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget.family.isAppleFamily }

    /**
     * Splits a string using a whitespace characters as delimiters.
     * Ignores whitespaces in quotes and drops quotes, e.g. a string
     * `foo "bar baz" qux="quux"` will be split into ["foo", "bar baz", "qux=quux"].
     */
    private fun String.splitQuotedArgs(): List<String> =
        Regex("""(?:[^\s"]|(?:"[^"]*"))+""").findAll(this).map {
            it.value.replace("\"", "")
        }.toList()

    private fun KotlinMultiplatformExtension.targetsForPlatform(requestedPlatform: KonanTarget) =
        supportedTargets().matching { it.konanTarget == requestedPlatform }

    private fun createDefaultFrameworks(kotlinExtension: KotlinMultiplatformExtension) {
        kotlinExtension.supportedTargets().all { target ->
            target.binaries.framework {
                isStatic = true
            }
        }
    }

    private fun Project.createSyncFrameworkTask(originalDirectory: File, buildingTask: Task) =
        tasks.create(SYNC_TASK_NAME, Sync::class.java) {
            it.group = TASK_GROUP
            it.description = "Copies a framework for given platform and build type into the CocoaPods build directory"

            it.dependsOn(buildingTask)
            it.from(originalDirectory)
            it.destinationDir = cocoapodsBuildDirs.framework
        }

    private fun createSyncForFatFramework(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        requestedBuildType: String,
        requestedPlatforms: List<KonanTarget>
    ) {
        val fatTargets = requestedPlatforms.associate { it to kotlinExtension.targetsForPlatform(it) }

        check(fatTargets.values.any { it.isNotEmpty() }) {
            "The project must have a target for at least one of the following platforms: " +
                    "${requestedPlatforms.joinToString { it.visibleName }}."
        }
        fatTargets.forEach { platform, targets ->
            check(targets.size <= 1) {
                "The project has more than one target for the requested platform: `${platform.visibleName}`"
            }
        }

        val fatFrameworkTask = project.tasks.create("fatFramework", FatFrameworkTask::class.java) { task ->
            task.group = TASK_GROUP
            task.description = "Creates a fat framework for ARM32 and ARM64 architectures"
            task.destinationDir = project.cocoapodsBuildDirs.fatFramework(requestedBuildType)

            fatTargets.forEach { _, targets ->
                targets.singleOrNull()?.let {
                    task.from(it.binaries.getFramework(requestedBuildType))
                }
            }
        }

        project.createSyncFrameworkTask(fatFrameworkTask.destinationDir, fatFrameworkTask)
    }

    private fun createSyncForRegularFramework(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        requestedBuildType: String,
        requestedPlatform: KonanTarget
    ) {
        val targets = kotlinExtension.targetsForPlatform(requestedPlatform)

        check(targets.isNotEmpty()) { "The project doesn't contain a target for the requested platform: `${requestedPlatform.visibleName}`" }
        check(targets.size == 1) { "The project has more than one target for the requested platform: `${requestedPlatform.visibleName}`" }

        val frameworkLinkTask = targets.single().binaries.getFramework(requestedBuildType).linkTask
        project.createSyncFrameworkTask(frameworkLinkTask.destinationDir, frameworkLinkTask)
    }

    private fun createSyncTask(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension
    ) = project.whenEvaluated {
        val requestedTargetName = project.findProperty(TARGET_PROPERTY)?.toString() ?: return@whenEvaluated
        val requestedBuildType = project.findProperty(CONFIGURATION_PROPERTY)?.toString()?.toUpperCase() ?: return@whenEvaluated

        // We create a fat framework only for device platforms which have several
        // device architectures: iosArm64, iosArm32, watchosArm32 and watchosArm64.
        val frameworkPlatforms: List<KonanTarget> = when (requestedTargetName) {
            KOTLIN_TARGET_FOR_IOS_DEVICE -> listOf(KonanTarget.IOS_ARM64, KonanTarget.IOS_ARM32)
            KOTLIN_TARGET_FOR_WATCHOS_DEVICE -> listOf(KonanTarget.WATCHOS_ARM32, KonanTarget.WATCHOS_ARM64)
            else -> listOf(HostManager().targetByName(requestedTargetName)) // A requested target doesn't require building a fat framework.
        }

        val frameworkTargets = frameworkPlatforms.flatMap { kotlinExtension.targetsForPlatform(it) }
        if (frameworkTargets.size == 1) {
            // Fast path: there is only one device target. There is no need to build a fat framework.
            createSyncForRegularFramework(project, kotlinExtension, requestedBuildType, frameworkTargets.single().konanTarget)
        } else {
            // There are several device targets so we need to build a fat framework.
            createSyncForFatFramework(project, kotlinExtension, requestedBuildType, frameworkPlatforms)
        }
    }

    private fun createPodspecGenerationTask(
        project: Project,
        cocoapodsExtension: CocoapodsExtension
    ) {
        val dummyFrameworkTask = project.tasks.create("generateDummyFramework", DummyFrameworkTask::class.java)

        project.tasks.create("podspec", PodspecTask::class.java) {
            it.group = TASK_GROUP
            it.description = "Generates a podspec file for CocoaPods import"
            it.settings = cocoapodsExtension
            it.dependsOn(dummyFrameworkTask)
            val generateWrapper = project.findProperty(GENERATE_WRAPPER_PROPERTY)?.toString()?.toBoolean() ?: false
            if (generateWrapper) {
                it.dependsOn(":wrapper")
            }
        }
    }

    private fun createInterops(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        cocoapodsExtension: CocoapodsExtension
    ) {
        val moduleNames = mutableSetOf<String>()
        cocoapodsExtension.pods.all { pod ->
            if (moduleNames.contains(pod.moduleName)) {
                return@all
            }
            moduleNames.add(pod.moduleName)

            val defTask = project.tasks.create(
                lowerCamelCaseName("generateDef", pod.moduleName).asValidTaskName(),
                DefFileTask::class.java
            ) {
                it.pod = pod
                it.description = "Generates a def file for CocoaPods dependencies with module ${pod.moduleName}"
                // This task is an implementation detail so we don't add it in any group
                // to avoid showing it in the `tasks` output.
            }

            kotlinExtension.supportedTargets().all { target ->
                target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.create(pod.moduleName) { interop ->

                    val interopTask = project.tasks.getByPath(interop.interopProcessingTaskName)
                    interopTask.dependsOn(defTask)
                    interop.defFile = defTask.outputFile
                    interop.packageName = "cocoapods.${pod.moduleName}"

                    project.findProperty(CFLAGS_PROPERTY)?.toString()?.let { args ->
                        // Xcode quotes around paths with spaces.
                        // Here and below we need to split such paths taking this into account.
                        interop.compilerOpts.addAll(args.splitQuotedArgs())
                    }
                    project.findProperty(HEADER_PATHS_PROPERTY)?.toString()?.let { args ->
                        interop.compilerOpts.addAll(args.splitQuotedArgs().map { "-I$it" })
                    }
                    project.findProperty(FRAMEWORK_PATHS_PROPERTY)?.toString()?.let { args ->
                        interop.compilerOpts.addAll(args.splitQuotedArgs().map { "-F$it" })
                    }

                    // Show a human-readable error messages if the interop is created
                    // but there are no parameters set by Xcode or manually by user (KT-31062).
                    interopTask.doFirst { _ ->
                        val hasCompilerOpts = interop.compilerOpts.isNotEmpty()
                        val hasHeaderSearchPath = interop.includeDirs.let {
                            !it.headerFilterDirs.isEmpty || !it.allHeadersDirs.isEmpty
                        }

                        check(hasCompilerOpts || hasHeaderSearchPath) {
                            """
                                |Cannot perform cinterop processing for module ${pod.moduleName}: cannot determine headers location.
                                |
                                |Probably the build is executed from command line.
                                |Note that a Kotlin/Native module using CocoaPods dependencies can be built only from Xcode.
                                |
                                |See details at https://kotlinlang.org/docs/reference/native/cocoapods.html#interoperability.
                            """.trimMargin()
                        }
                    }
                }
            }
        }
    }

    override fun apply(project: Project): Unit = with(project) {
        pluginManager.withPlugin("kotlin-multiplatform") {
            val kotlinExtension = project.multiplatformExtension
            val cocoapodsExtension = CocoapodsExtension(this)

            kotlinExtension.addExtension(EXTENSION_NAME, cocoapodsExtension)
            createDefaultFrameworks(kotlinExtension)
            createSyncTask(project, kotlinExtension)
            createPodspecGenerationTask(project, cocoapodsExtension)
            createInterops(project, kotlinExtension, cocoapodsExtension)
        }
    }

    companion object {
        const val EXTENSION_NAME = "cocoapods"
        const val TASK_GROUP = "CocoaPods"
        const val SYNC_TASK_NAME = "syncFramework"

        // We don't move these properties in PropertiesProvider because
        // they are not intended to be overridden in local.properties.
        const val TARGET_PROPERTY = "kotlin.native.cocoapods.target"
        const val CONFIGURATION_PROPERTY = "kotlin.native.cocoapods.configuration"

        const val CFLAGS_PROPERTY = "kotlin.native.cocoapods.cflags"
        const val HEADER_PATHS_PROPERTY = "kotlin.native.cocoapods.paths.headers"
        const val FRAMEWORK_PATHS_PROPERTY = "kotlin.native.cocoapods.paths.frameworks"

        const val GENERATE_WRAPPER_PROPERTY = "kotlin.native.cocoapods.generate.wrapper"

        // Used in Xcode script phase to indicate that the framework is being built for a device
        // so we should generate a fat framework with arm32 and arm64 binaries.
        const val KOTLIN_TARGET_FOR_IOS_DEVICE = "ios_arm"
        const val KOTLIN_TARGET_FOR_WATCHOS_DEVICE = "watchos_arm"
    }
}