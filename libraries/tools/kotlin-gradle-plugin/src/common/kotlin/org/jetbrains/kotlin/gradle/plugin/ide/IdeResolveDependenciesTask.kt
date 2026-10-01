/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import kotlinx.serialization.encodeToString
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.appendLine
import java.io.File

internal val IdeResolveDependenciesTaskSetupAction = KotlinProjectSetupAction {
    locateOrRegisterIdeResolveDependenciesTask()
}

internal fun Project.locateOrRegisterIdeResolveDependenciesTask(): TaskProvider<IdeResolveDependenciesTask> {
    return locateOrRegisterTask("resolveIdeDependencies") { task ->
        task.description = "Debugging/Diagnosing task that will resolve dependencies for the IDE"
        task.group = "ide"
        task.notCompatibleWithConfigurationCache("Just a debugging util")
        task.kotlinIdeMultiplatformImport.value(project.kotlinIdeMultiplatformImport).finalizeValue()
        // Fixes circular dependency on eager tasks initialization
        task.kotlinIdeMultiplatformImport.get().addDependencyOnResolvers(task)
    }
}

/**
 * Task intended to be use for debugging/diagnosing purposes.
 * This will invoke the [IdeMultiplatformImport] to resolve all dependencies (like the IDE would).
 * Outputs are written as json and protobufs
 */
@DisableCachingByDefault(because = "Used for debugging/diagnostic purpose.")
internal abstract class IdeResolveDependenciesTask : DefaultTask() {
    private val outputDirectory = project.layout.buildDirectory.dir("ide/dependencies")
    private val kotlinExtension = project.kotlinExtension
    private val kotlinIdeMultiplatformImportStatistics = project.kotlinIdeMultiplatformImportStatistics
    private val projectDir = project.projectDir
    private val rootDir = project.rootDir

    @get:Internal
    internal abstract val kotlinIdeMultiplatformImport: Property<IdeMultiplatformImport>

    @TaskAction
    fun resolveDependencies() {
        val outputDirectory = outputDirectory.get().asFile
        outputDirectory.deleteRecursively()

        kotlinExtension.sourceSets.forEach { sourceSet ->
            val dependencies = kotlinIdeMultiplatformImport.get().resolveDependencies(sourceSet)
            val jsonOutput = outputDirectory.resolve("json/${sourceSet.name}.json")
            jsonOutput.parentFile.mkdirs()
            val dependenciesJson = dependencies.map { it.toJson(::relativePath) }
            jsonOutput.writeText(KgpJson.prettyPrinted.encodeToString(dependenciesJson))

            kotlinIdeMultiplatformImport.get().serialize(dependencies).forEachIndexed { index, proto ->
                val protoOutput = outputDirectory.resolve("proto/${sourceSet.name}/$index.bin")
                protoOutput.parentFile.mkdirs()
                protoOutput.writeBytes(proto)
            }
        }

        kotlinIdeMultiplatformImportStatistics.let { statistics ->
            val timeStatisticsFile = outputDirectory.resolve("times.txt")
            timeStatisticsFile.writeText(buildString {
                statistics.getExecutionTimes().forEach { (clazz, time) ->
                    appendLine("${clazz.name} $time.ms")
                }
            })
        }
    }

    private fun relativePath(file: File): String = when {
        file.startsWith(projectDir) -> file.relativeTo(projectDir).invariantSeparatorsPath
        file.startsWith(rootDir) -> file.relativeTo(rootDir).invariantSeparatorsPath
        else -> file.path
    }
}
