/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import com.google.gson.*
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependencyCoordinates
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.gradle.utils.notCompatibleWithConfigurationCacheCompat
import org.jetbrains.kotlin.tooling.core.Extras
import java.io.File
import java.lang.reflect.Type

internal fun Project.locateOrRegisterIdeResolveDependenciesTask(): TaskProvider<IdeResolveDependenciesTask> {
    return locateOrRegisterTask("resolveIdeDependencies") { task ->
        task.description = "Debugging/Diagnosing task that will resolve dependencies for the IDE"
        task.group = "ide"
        task.notCompatibleWithConfigurationCacheCompat("Just a debugging util")
    }
}

/**
 * Task intended to be use for debugging/diagnosing purposes.
 * This will invoke the [IdeMultiplatformImport] to resolve all dependencies (like the IDE would).
 * Outputs are written as json and protobufs
 */
@DisableCachingByDefault(because = "Used for debugging/diagnostic purpose.")
internal open class IdeResolveDependenciesTask : DefaultTask() {

    @TaskAction
    fun resolveDependencies() {
        val outputDirectory = project.buildDir.resolve("ide/dependencies")
        outputDirectory.deleteRecursively()
        val gson = GsonBuilder().setLenient().setPrettyPrinting()
            .registerTypeHierarchyAdapter(IdeDependencyResolver::class.java, IdeDependencyResolverAdapter)
            .registerTypeHierarchyAdapter(Extras::class.java, ExtrasAdapter)
            .registerTypeHierarchyAdapter(IdeaKotlinDependencyCoordinates::class.java, IdeaKotlinDependencyCoordinatesAdapter)
            .registerTypeAdapter(File::class.java, FileAdapter(project))
            .create()

        val extension = project.kotlinExtension
        extension.sourceSets.forEach { sourceSet ->
            val dependencies = project.kotlinIdeMultiplatformImport.resolveDependencies(sourceSet)
            val jsonOutput = outputDirectory.resolve("json/${sourceSet.name}.json")
            jsonOutput.parentFile.mkdirs()
            jsonOutput.writeText(gson.toJson(dependencies))

            project.kotlinIdeMultiplatformImport.serialize(dependencies).forEachIndexed { index, proto ->
                val protoOutput = outputDirectory.resolve("proto/${sourceSet.name}/$index.bin")
                protoOutput.parentFile.mkdirs()
                protoOutput.writeBytes(proto)
            }
        }

        project.kotlinIdeMultiplatformImportStatistics.let { statistics ->
            val timeStatisticsFile = outputDirectory.resolve("times.txt")
            timeStatisticsFile.writeText(buildString {
                statistics.getExecutionTimes().forEach { (clazz, time) ->
                    appendLine("${clazz.name} $time.ms")
                }
            })
        }
    }

    private object IdeDependencyResolverAdapter : JsonSerializer<IdeDependencyResolver> {
        override fun serialize(src: IdeDependencyResolver, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonPrimitive(src.javaClass.name)
        }
    }

    private object IdeaKotlinDependencyCoordinatesAdapter : JsonSerializer<IdeaKotlinDependencyCoordinates> {
        override fun serialize(src: IdeaKotlinDependencyCoordinates, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return JsonPrimitive(src.toString())
        }
    }

    private object ExtrasAdapter : JsonSerializer<Extras> {
        override fun serialize(src: Extras, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            return JsonObject().apply {
                src.entries.forEach { entry ->
                    val valueElement = runCatching { context.serialize(entry.value) }.getOrElse { JsonPrimitive(entry.value.toString()) }
                    add(entry.key.stableString, valueElement)
                }
            }
        }
    }

    private class FileAdapter(private val project: Project) : JsonSerializer<File> {
        override fun serialize(src: File, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            return if (src.startsWith(project.projectDir)) {
                JsonPrimitive(src.relativeTo(project.projectDir).path)
            } else if (src.startsWith(project.rootDir)) {
                JsonPrimitive(src.relativeTo(project.rootDir).path)
            } else {
                JsonPrimitive(src.path)
            }
        }
    }
}
