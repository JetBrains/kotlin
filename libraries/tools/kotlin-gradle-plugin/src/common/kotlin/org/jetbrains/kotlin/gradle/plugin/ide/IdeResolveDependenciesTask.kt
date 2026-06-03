/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import kotlinx.serialization.json.*
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.artifacts.result.ArtifactResult
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependencyCoordinates
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupAction
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.appendLine
import org.jetbrains.kotlin.tooling.core.Extras
import java.io.File
import java.util.IdentityHashMap

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

    @get:Internal
    internal abstract val kotlinIdeMultiplatformImport: Property<IdeMultiplatformImport>

    @TaskAction
    fun resolveDependencies() {
        val outputDirectory = outputDirectory.get().asFile
        outputDirectory.deleteRecursively()
        val prettyJson = KgpJson.prettyPrinted

        kotlinExtension.sourceSets.forEach { sourceSet ->
            val dependencies = kotlinIdeMultiplatformImport.get().resolveDependencies(sourceSet)
            val jsonOutput = outputDirectory.resolve("json/${sourceSet.name}.json")
            jsonOutput.parentFile.mkdirs()
            jsonOutput.writeText(prettyJson.encodeToString(JsonElement.serializer(), toJsonElement(dependencies)))

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

    // --- Reflective JSON walker -----------------------------------------------------------------

    private fun toJsonElement(value: Any?, seen: IdentityHashMap<Any, Unit> = IdentityHashMap()): JsonElement {
        if (value == null) return JsonNull
        // Stable toString() types
        when (value) {
            is Boolean -> return JsonPrimitive(value)
            is Number -> return JsonPrimitive(value)
            is String -> return JsonPrimitive(value)
            is Enum<*> -> return JsonPrimitive(value.name)
        }
        // Special-cased types matching the previous Gson adapters
        if (value is IdeDependencyResolver) return JsonPrimitive(value.javaClass.name)
        if (value is IdeaKotlinDependencyCoordinates) return JsonPrimitive(value.toString())
        if (value is ArtifactResult) return JsonPrimitive(value.toString())
        if (value is File) return JsonPrimitive(fileToString(value))
        if (value is Extras) return extrasToJsonObject(value, seen)
        // Collections
        if (value is Iterable<*>) {
            if (seen.containsKey(value)) return JsonPrimitive("<cycle>")
            seen[value] = Unit
            val arr = JsonArray(value.mapNotNull { toJsonElement(it, seen) })
            seen.remove(value)
            return arr
        }
        if (value is Map<*, *>) {
            if (seen.containsKey(value)) return JsonPrimitive("<cycle>")
            seen[value] = Unit
            val obj = buildJsonObject {
                value.entries.forEach { (k, v) -> put(k.toString(), toJsonElement(v, seen)) }
            }
            seen.remove(value)
            return obj
        }
        // Arbitrary objects: reflect over declared fields
        if (seen.containsKey(value)) return JsonPrimitive("<cycle>")
        seen[value] = Unit
        val obj = reflectToJsonObject(value, seen)
        seen.remove(value)
        return obj
    }

    private fun fileToString(file: File): String {
        val projectDir = project.projectDir
        val rootDir = project.rootDir
        return when {
            file.startsWith(projectDir) -> file.relativeTo(projectDir).path
            file.startsWith(rootDir) -> file.relativeTo(rootDir).path
            else -> file.path
        }
    }

    private fun extrasToJsonObject(extras: Extras, seen: IdentityHashMap<Any, Unit>): JsonElement {
        return buildJsonObject {
            extras.entries.forEach { entry ->
                val valueElement = runCatching { toJsonElement(entry.value, seen) }.getOrElse { JsonPrimitive(entry.value.toString()) }
                put(entry.key.stableString, valueElement)
            }
        }
    }

    private fun reflectToJsonObject(value: Any, seen: IdentityHashMap<Any, Unit>): JsonElement {
        return buildJsonObject {
            put("@type", value.javaClass.name)
            var clazz: Class<*>? = value.javaClass
            while (clazz != null && clazz != Any::class.java) {
                clazz.declaredFields.forEach { field ->
                    if (java.lang.reflect.Modifier.isStatic(field.modifiers)) return@forEach
                    field.isAccessible = true
                    val fieldValue = runCatching { field.get(value) }.getOrNull()
                    put(field.name, toJsonElement(fieldValue, seen))
                }
                clazz = clazz.superclass
            }
        }
    }
}
