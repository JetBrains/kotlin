/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import com.google.gson.*
import org.jetbrains.kotlin.tooling.KotlinToolingMetadataParsingResult.Failure
import org.jetbrains.kotlin.tooling.KotlinToolingMetadataParsingResult.Success

fun KotlinToolingMetadata.toJsonString(): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(toJsonObject())
}

internal fun KotlinToolingMetadata.toJsonObject(): JsonObject {
    return JsonObject().apply {
        addProperty("buildSystem", buildSystem)
        addProperty("buildSystemVersion", buildSystemVersion)
        addProperty("buildPlugin", buildPlugin)
        addProperty("buildPluginVersion", buildPluginVersion)
        add("projectSettings", projectSettings.toJsonObject())
        add("projectTargets", projectTargets.toJsonArray())
    }
}

internal fun KotlinToolingMetadata.ProjectSettings.toJsonObject(): JsonObject {
    return JsonObject().apply {
        addProperty("isHmppEnabled", isHmppEnabled)
        addProperty("isCompatibilityMetadataVariantEnabled", isCompatibilityMetadataVariantEnabled)
    }
}

internal fun List<KotlinToolingMetadata.ProjectTargetMetadata>.toJsonArray(): JsonArray {
    return JsonArray().apply {
        this@toJsonArray.forEach { targetMetadata ->
            add(targetMetadata.toJsonObject())
        }
    }
}

internal fun KotlinToolingMetadata.ProjectTargetMetadata.toJsonObject(): JsonObject {
    return JsonObject().apply {
        addProperty("target", target)
        addProperty("platformType", platformType)
        if (extras.isNotEmpty()) {
            add("extras", JsonObject().apply {
                for (extra in extras) {
                    addProperty(extra.key, extra.value)
                }
            })
        }
    }
}

sealed class KotlinToolingMetadataParsingResult {
    data class Success(val value: KotlinToolingMetadata) : KotlinToolingMetadataParsingResult()
    data class Failure(val reason: String) : KotlinToolingMetadataParsingResult()
}

fun KotlinToolingMetadata.Companion.parseJson(json: String): KotlinToolingMetadataParsingResult {
    val jsonElement = try {
        JsonParser.parseString(json)
    } catch (e: JsonParseException) {
        return Failure("Invalid json: ${e.message}")
    }
    if (jsonElement !is JsonObject) {
        return Failure("Expected JsonObject. Found ${json::class.java.canonicalName}")
    }
    return runCatching { jsonElement.toKotlinToolingMetadataOrThrow() }
        .fold(
            onSuccess = { Success(it) },
            onFailure = { Failure("Failed parsing JsonObject: ${it.message}") }
        )
}

fun KotlinToolingMetadata.Companion.parseJsonOrThrow(value: String): KotlinToolingMetadata {
    return when (val result = parseJson(value)) {
        is Success -> result.value
        is Failure -> throw IllegalArgumentException(result.reason)
    }
}

private fun JsonObject.toKotlinToolingMetadataOrThrow(): KotlinToolingMetadata {
    return KotlinToolingMetadata(
        buildSystem = getOrThrow("buildSystem").asString,
        buildSystemVersion = getOrThrow("buildSystemVersion").asString,
        buildPlugin = getOrThrow("buildPlugin").asString,
        buildPluginVersion = getOrThrow("buildPluginVersion").asString,
        projectSettings = getOrThrow("projectSettings").asJsonObject.toProjectSettingsOrThrow(),
        projectTargets = getOrThrow("projectTargets").asJsonArray.map { it.asJsonObject.toTargetMetadataOrThrow() }
    )
}

private fun JsonObject.toProjectSettingsOrThrow(): KotlinToolingMetadata.ProjectSettings {
    return KotlinToolingMetadata.ProjectSettings(
        isHmppEnabled = getOrThrow("isHmppEnabled").asBoolean,
        isCompatibilityMetadataVariantEnabled = getOrThrow("isCompatibilityMetadataVariantEnabled").asBoolean
    )
}

private fun JsonObject.toTargetMetadataOrThrow(): KotlinToolingMetadata.ProjectTargetMetadata {
    return KotlinToolingMetadata.ProjectTargetMetadata(
        target = getOrThrow("target").asString,
        platformType = getOrThrow("platformType").asString,
        extras = (get("extras") as? JsonObject)?.toTargetMetadataExtrasOrThrow() ?: emptyMap()
    )
}

private fun JsonObject.toTargetMetadataExtrasOrThrow(): Map<String, String> {
    val map = mutableMapOf<String, String>()
    this.keySet().forEach { key ->
        val primitive = this[key] as? JsonPrimitive
        if (primitive != null) {
            map[key] = primitive.asString
        }
    }
    return map.toMap()
}

private fun JsonObject.getOrThrow(key: String): JsonElement {
    return get(key) ?: throw IllegalArgumentException("Missing key: $key")
}
