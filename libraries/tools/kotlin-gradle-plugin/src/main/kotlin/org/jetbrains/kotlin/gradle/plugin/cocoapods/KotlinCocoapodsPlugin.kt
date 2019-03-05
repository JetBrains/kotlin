/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.cocoapods

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.Sync
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.gradle.tasks.DefFileTask
import org.jetbrains.kotlin.gradle.tasks.DummyFrameworkTask
import org.jetbrains.kotlin.gradle.tasks.PodspecTask
import org.jetbrains.kotlin.gradle.utils.asValidTaskName
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.HostManager
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
}

internal fun String.asValidFrameworkName() = replace('-', '_')

open class KotlinCocoapodsPlugin: Plugin<Project> {

    private fun KotlinMultiplatformExtension.supportedTargets() = targets
        .withType(KotlinNativeTarget::class.java)
        .matching { it.konanTarget.family == Family.IOS || it.konanTarget.family == Family.OSX }

    /**
     * Splits a string using a whitespace characters as delimiters.
     * Ignores whitespaces in quotes and drops quotes, e.g. a string
     * `foo "bar baz" qux="quux"` will be split into ["foo", "bar baz", "qux=quux"].
     */
    private fun String.splitQuotedArgs(): List<String> =
        Regex("""(?:[^\s"]|(?:"[^"]*"))+""").findAll(this).map {
            it.value.replace("\"", "")
        }.toList()

    private fun createDefaultFrameworks(kotlinExtension: KotlinMultiplatformExtension) {
        kotlinExtension.supportedTargets().all { target ->
            target.binaries.framework {
                isStatic = true
            }
        }
    }

    private fun createSyncTask(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension
    ) = project.whenEvaluated {
        val requestedTargetName = project.findProperty(TARGET_PROPERTY)?.toString() ?: return@whenEvaluated
        val requestedBuildType = project.findProperty(CONFIGURATION_PROPERTY)?.toString()?.toUpperCase() ?: return@whenEvaluated

        val requestedTarget = HostManager().targetByName(requestedTargetName)

        val targets = kotlinExtension.supportedTargets().matching {
            it.konanTarget == requestedTarget
        }

        check(targets.isNotEmpty()) { "The project doesn't contain a target for the requested platform: $requestedTargetName" }
        check(targets.size == 1) { "The project has more than one targets for the requested platform: $requestedTargetName" }

        val framework =  targets.single().binaries.getFramework(requestedBuildType)
        project.tasks.create("syncFramework", Sync::class.java) {
            it.group = TASK_GROUP
            it.description = "Copies a framework for given platform and build type into the CocoaPods build directory"

            it.dependsOn(framework.linkTask)
            it.from(framework.linkTask.destinationDir)
            it.destinationDir = cocoapodsBuildDirs.framework
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
        }
    }

    private fun createInterops(
        project: Project,
        kotlinExtension: KotlinMultiplatformExtension,
        cocoapodsExtension: CocoapodsExtension
    ) {
        cocoapodsExtension.pods.all { pod ->
            kotlinExtension.supportedTargets().all { target ->
                target.compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME).cinterops.create(pod.name) { interop ->

                    val defTask = project.tasks.create(
                        lowerCamelCaseName("generateDef", pod.name, target.name).asValidTaskName(),
                        DefFileTask::class.java
                    ) {
                        it.pod = pod
                        it.description = "Generates a def file for CocoaPods dependency ${pod.name} for target ${target.name}"
                        // This task is an implementation detail so we don't add it in any group
                        // to avoid showing it in the `tasks` output.
                    }

                    project.tasks.getByPath(interop.interopProcessingTaskName).dependsOn(defTask)
                    interop.defFile = defTask.outputFile
                    interop.packageName = "cocoapods.${pod.moduleName}"

                    project.findProperty(CFLAGS_PROPERTY)?.toString()?.let { args ->
                        // Xcode quotes around paths with spaces.
                        // Here and below we need to split such paths taking this into account.
                        interop.compilerOpts.addAll(args.splitQuotedArgs())
                    }
                    project.findProperty(HEADER_PATHS_PROPERTY)?.toString()?.let { args->
                        interop.compilerOpts.addAll(args.splitQuotedArgs().map { "-I$it" })
                    }
                    project.findProperty(FRAMEWORK_PATHS_PROPERTY)?.toString()?.let { args ->
                        interop.compilerOpts.addAll(args.splitQuotedArgs().map { "-F$it" })
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

        // We don't move these properties in PropertiesProvider because
        // they are not intended to be overridden in local.properties.
        const val TARGET_PROPERTY = "kotlin.native.cocoapods.target"
        const val CONFIGURATION_PROPERTY = "kotlin.native.cocoapods.configuration"

        const val CFLAGS_PROPERTY = "kotlin.native.cocoapods.cflags"
        const val HEADER_PATHS_PROPERTY = "kotlin.native.cocoapods.paths.headers"
        const val FRAMEWORK_PATHS_PROPERTY = "kotlin.native.cocoapods.paths.frameworks"

    }
}