/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinProjectArtifactDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinResolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.extras.KlibExtra
import org.jetbrains.kotlin.gradle.internal.json.KgpJson
import org.jetbrains.kotlin.tooling.core.Extras
import java.io.File

/** JSON form of a dependency in the `resolveIdeDependencies` output. Coordinates are written with toString(). */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("kind")
internal sealed class IdeResolvedDependencyJson {
    @Serializable
    @SerialName("resolvedBinary")
    data class ResolvedBinary(
        val binaryType: String,
        val classpath: List<String>,
        val coordinates: String?,
        val extras: Map<String, JsonElement>,
    ) : IdeResolvedDependencyJson()

    @Serializable
    @SerialName("unresolvedBinary")
    data class UnresolvedBinary(
        val cause: String?,
        val coordinates: String?,
        val extras: Map<String, JsonElement>,
    ) : IdeResolvedDependencyJson()

    @Serializable
    @SerialName("source")
    data class Source(
        val type: String,
        val coordinates: String,
        val extras: Map<String, JsonElement>,
    ) : IdeResolvedDependencyJson()

    @Serializable
    @SerialName("projectArtifact")
    data class ProjectArtifact(
        val type: String,
        val coordinates: String,
        val extras: Map<String, JsonElement>,
    ) : IdeResolvedDependencyJson()
}

/** [path] formats files, e.g. relative to the project. */
internal fun IdeaKotlinDependency.toJson(path: (File) -> String): IdeResolvedDependencyJson {
    val extrasJson = extras.toJson(path)
    return when (this) {
        is IdeaKotlinResolvedBinaryDependency -> IdeResolvedDependencyJson.ResolvedBinary(
            binaryType = binaryType,
            classpath = classpath.map(path),
            coordinates = coordinates?.toString(),
            extras = extrasJson,
        )
        is IdeaKotlinUnresolvedBinaryDependency -> IdeResolvedDependencyJson.UnresolvedBinary(
            cause = cause,
            coordinates = coordinates?.toString(),
            extras = extrasJson,
        )
        is IdeaKotlinSourceDependency -> IdeResolvedDependencyJson.Source(
            type = type.name,
            coordinates = coordinates.toString(),
            extras = extrasJson,
        )
        is IdeaKotlinProjectArtifactDependency -> IdeResolvedDependencyJson.ProjectArtifact(
            type = type.name,
            coordinates = coordinates.toString(),
            extras = extrasJson,
        )
    }
}

private fun Extras.toJson(path: (File) -> String): Map<String, JsonElement> =
    entries.associate { entry -> entry.key.stableString to extraValueToJsonElement(entry.value, path) }

@Suppress("unused") // read by the generated serializer
@Serializable
private class KlibExtraJson(
    val builtInsPlatform: String?,
    val uniqueName: String?,
    val shortName: String?,
    val packageFqName: String?,
    val nativeTargets: List<String>?,
    val commonizerNativeTargets: List<String>?,
    val commonizerTarget: String?,
    val isInterop: Boolean?,
)

private fun KlibExtra.toJson() = KlibExtraJson(
    builtInsPlatform, uniqueName, shortName, packageFqName, nativeTargets, commonizerNativeTargets, commonizerTarget, isInterop,
)

/** Extras erase value types: known ones get a JSON form, the rest use toString(). */
private fun extraValueToJsonElement(value: Any?, path: (File) -> String): JsonElement = when (value) {
    null -> JsonNull
    is String -> JsonPrimitive(value)
    is Boolean -> JsonPrimitive(value)
    // JSON has no NaN or Infinity
    is Double -> if (value.isFinite()) JsonPrimitive(value) else JsonPrimitive(value.toString())
    is Float -> if (value.isFinite()) JsonPrimitive(value) else JsonPrimitive(value.toString())
    is Number -> JsonPrimitive(value)
    is File -> JsonPrimitive(path(value))
    is IdeDependencyResolver -> JsonPrimitive(value.javaClass.name)
    is IdeaKotlinDependency -> KgpJson.default.encodeToJsonElement(IdeResolvedDependencyJson.serializer(), value.toJson(path))
    is KlibExtra -> KgpJson.default.encodeToJsonElement(KlibExtraJson.serializer(), value.toJson())
    // Extras is a Collection, so it has to come before Iterable
    is Extras -> JsonObject(value.toJson(path))
    is Map<*, *> -> JsonObject(value.entries.associate { (k, v) -> k.toString() to extraValueToJsonElement(v, path) })
    is Iterable<*> -> JsonArray(value.map { extraValueToJsonElement(it, path) })
    is Array<*> -> JsonArray(value.map { extraValueToJsonElement(it, path) })
    else -> JsonPrimitive(value.toString())
}
