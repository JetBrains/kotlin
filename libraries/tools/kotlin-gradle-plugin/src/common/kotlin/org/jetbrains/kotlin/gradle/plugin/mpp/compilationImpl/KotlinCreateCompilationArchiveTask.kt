/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.Zip
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.disambiguateName
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.mpp.isTest
import org.jetbrains.kotlin.gradle.utils.CurrentBuildIdentifier
import org.jetbrains.kotlin.gradle.utils.currentBuild
import org.jetbrains.kotlin.gradle.utils.libsDirectory

/**
 * **WARNING!** This is internal Kotlin Gradle Plugin API, it is not supported outside JetBrains.
 *
 * Extension DSL to access Archive Tasks for custom compilations
 * These archive tasks can be exported as artifacts via consumable configuration.
 * And IDE will correctly import them.
 *
 * Example:
 * ```kotlin
 * // project :lib
 * kotlin {
 *    jvm {
 *      val customCompilation by compilations.creating
 *      val archiveTask = kotlinCompilationArchiveTasks
 *          .getArchiveTaskOrNull(customCompilation)!!
 *
 *      val jvmCustomApiElements by configurations.creating {
 *        isCanBeConsumed = true
 *      }
 *      project.artifacts.add(jvmCustomApiElements.name, archiveTask)
 *    }
 * }
 *
 * // project :app
 * kotlin {
 *    jvm()
 *    sourceSets {
 *       jvmMain {
 *          dependencies {
 *             // This dependency will be correctly resolved in IDE
 *             // as Module dependency to `lib.jvmCustom` source set
 *             compileOnly(project(":lib", configuration = "jvmCustomApiElements"))
 *          }
 *       }
 *    }
 * }
 * ```
 */
@InternalKotlinGradlePluginApi
interface KotlinCompilationArchiveTasks {
    /**
     * Returns archive task associated with [kotlinCompilation] instance.
     *
     * This method searches via Identity class (Project path, target and compilation names) of Compilations not by reference.
     * So it is safe to pass decorated or wrapped instances.
     */
    fun getArchiveTaskOrNull(kotlinCompilation: KotlinCompilation<*>): TaskProvider<out AbstractArchiveTask>?
}

internal val KotlinRegisterCompilationArchiveTasksExtension = KotlinProjectSetupAction {
    if (!project.kotlinPropertiesProvider.createArchiveTasksForCustomCompilations) return@KotlinProjectSetupAction
    project.extensions.add(
        KotlinCompilationArchiveTasks::class.java,
        "kotlinCompilationsArchiveTasks",
        KotlinCompilationArchiveTasksImpl(project.currentBuild)
    )
}

private val Project.kotlinCompilationArchiveTasksImplOrNull: KotlinCompilationArchiveTasksImpl?
    get() = extensions.findByName("kotlinCompilationsArchiveTasks") as? KotlinCompilationArchiveTasksImpl?

internal val Project.kotlinCompilationArchiveTasksOrNull: KotlinCompilationArchiveTasks? get() = kotlinCompilationArchiveTasksImplOrNull

private class KotlinCompilationArchiveTasksImpl(
    private val currentBuild: CurrentBuildIdentifier
) : KotlinCompilationArchiveTasks {
    private val tasksMap: MutableMap<Key, TaskProvider<out AbstractArchiveTask>> = mutableMapOf()

    private data class Key(
        val targetName: String,
        val compilationName: String
    ) {
        constructor(kotlinCompilation: KotlinCompilation<*>) : this(
            targetName = kotlinCompilation.target.targetName,
            compilationName = kotlinCompilation.compilationName
        )
    }

    fun store(kotlinCompilation: KotlinCompilation<*>, task: TaskProvider<out AbstractArchiveTask>) {
        require(kotlinCompilation.project in currentBuild) { "Compilation $kotlinCompilation is not from current project" }
        tasksMap[Key(kotlinCompilation)] = task
    }

    override fun getArchiveTaskOrNull(kotlinCompilation: KotlinCompilation<*>): TaskProvider<out AbstractArchiveTask>? {
        if (kotlinCompilation.project !in currentBuild) return null
        return tasksMap[Key(kotlinCompilation)]
    }
}

internal val KotlinCreateCompilationArchivesTask = KotlinCompilationSideEffect { compilation ->
    if (compilation.isMain() || compilation.isTest()) return@KotlinCompilationSideEffect
    if (compilation.target is KotlinMetadataTarget) return@KotlinCompilationSideEffect
    // Pessimistically exclude Android target to avoid any conflicts
    if (compilation.target.platformType == KotlinPlatformType.androidJvm) return@KotlinCompilationSideEffect

    val project = compilation.project
    if (!project.kotlinPropertiesProvider.createArchiveTasksForCustomCompilations) return@KotlinCompilationSideEffect

    val archiveTask = if (compilation.target.platformType == KotlinPlatformType.jvm) {
        project.tasks.register(compilation.disambiguateName("jar"), Jar::class.java) { task ->
            task.from(compilation.output.allOutputs)
            task.archiveBaseName.convention(compilation.target.disambiguateName(compilation.name))
        }
    } else {
        project.tasks.register(compilation.disambiguateName("klib"), Zip::class.java) { task ->
            task.from(compilation.output.allOutputs)
            task.archiveBaseName.convention(compilation.target.disambiguateName(compilation.name))
            task.destinationDirectory.convention(project.libsDirectory)
            task.archiveExtension.set("klib")
        }
    }

    project.kotlinCompilationArchiveTasksImplOrNull?.store(compilation, archiveTask)
}
