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
        addProperty("schemaVersion", schemaVersion)
        addProperty("buildSystem", buildSystem)
        addProperty("buildSystemVersion", buildSystemVersion)
        addProperty("buildPlugin", buildPlugin)
        addProperty("buildPluginVersion", buildPluginVersion)
        add("projectSettings", projectSettings.toJsonObject())
        add("projectTargets", projectTargets.toJsonArray())
    }
}

private fun KotlinToolingMetadata.ProjectSettings.toJsonObject(): JsonObject {
    return JsonObject().apply {
        addProperty("isHmppEnabled", isHmppEnabled)
        addProperty("isCompatibilityMetadataVariantEnabled", isCompatibilityMetadataVariantEnabled)
        addProperty("isKPMEnabled", isKPMEnabled)
    }
}

private fun List<KotlinToolingMetadata.ProjectTargetMetadata>.toJsonArray(): JsonArray {
    return JsonArray().apply {
        this@toJsonArray.forEach { targetMetadata ->
            add(targetMetadata.toJsonObject())
        }
    }
}

private fun KotlinToolingMetadata.ProjectTargetMetadata.toJsonObject(): JsonObject {
    return JsonObject().apply {
        addProperty("target", target)
        addProperty("platformType", platformType)
        extras.toJsonObject().takeIf { it.size() > 0 }?.let { extrasJsonObject ->
            add("extras", extrasJsonObject)
        }
    }
}

private fun KotlinToolingMetadata.ProjectTargetMetadata.Extras.toJsonObject(): JsonObject {
    return JsonObject().apply {
        jvm?.let { add("jvm", it.toJsonObject()) }
        android?.let { add("android", it.toJsonObject()) }
        js?.let { add("js", it.toJsonObject()) }
        native?.let { add("native", it.toJsonObject()) }
    }
}

private fun KotlinToolingMetadata.ProjectTargetMetadata.JvmExtras.toJsonObject(): JsonObject {
    return JsonObject().apply {
        jvmTarget?.let { addProperty("jvmTarget", it) }
        addProperty("withJavaEnabled", withJavaEnabled)
    }
}

private fun KotlinToolingMetadata.ProjectTargetMetadata.JsExtras.toJsonObject(): JsonObject {
    return JsonObject().apply {
        addProperty("isBrowserConfigured", isBrowserConfigured)
        addProperty("isNodejsConfigured", isNodejsConfigured)
    }
}

private fun KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras.toJsonObject(): JsonObject {
    return JsonObject().apply {
        addProperty("konanTarget", konanTarget)
        addProperty("konanVersion", konanVersion)
        addProperty("konanAbiVersion", konanAbiVersion)
    }
}

private fun KotlinToolingMetadata.ProjectTargetMetadata.AndroidExtras.toJsonObject(): JsonObject {
    return JsonObject().apply {
        addProperty("sourceCompatibility", sourceCompatibility)
        addProperty("targetCompatibility", targetCompatibility)
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
    val schemaVersion = SchemaVersion.parseStringOrThrow(getOrThrow("schemaVersion").asString)
    if (!SchemaVersion.current.isCompatible(schemaVersion)) {
        throw IllegalArgumentException(
            "Incompatible schemaVersion='$schemaVersion' found. Current schemaVersion='${SchemaVersion.current}'"
        )
    }
    return KotlinToolingMetadata(
        schemaVersion = schemaVersion.toString(),
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
        isCompatibilityMetadataVariantEnabled = getOrThrow("isCompatibilityMetadataVariantEnabled").asBoolean,
        isKPMEnabled = get("isKPMEnabled")?.asBoolean ?: false
    )
}

private fun JsonObject.toTargetMetadataOrThrow(): KotlinToolingMetadata.ProjectTargetMetadata {
    return KotlinToolingMetadata.ProjectTargetMetadata(
        target = getOrThrow("target").asString,
        platformType = getOrThrow("platformType").asString,
        extras = (get("extras") as? JsonObject)?.toTargetMetadataExtrasOrThrow()
            ?: KotlinToolingMetadata.ProjectTargetMetadata.Extras()
    )
}

private fun JsonObject.toTargetMetadataExtrasOrThrow(): KotlinToolingMetadata.ProjectTargetMetadata.Extras {
    return KotlinToolingMetadata.ProjectTargetMetadata.Extras(
        jvm = get("jvm")?.asJsonObject?.toJvmExtrasOrThrow(),
        android = get("android")?.asJsonObject?.toAndroidExtrasOrThrow(),
        js = get("js")?.asJsonObject?.toJsExtrasOrThrow(),
        native = get("native")?.asJsonObject?.toNativeExtrasOrThrow()
    )
}

private fun JsonObject.toJvmExtrasOrThrow(): KotlinToolingMetadata.ProjectTargetMetadata.JvmExtras {
    return KotlinToolingMetadata.ProjectTargetMetadata.JvmExtras(
        jvmTarget = get("jvmTarget")?.asString,
        withJavaEnabled = getOrThrow("withJavaEnabled").asBoolean
    )
}

private fun JsonObject.toJsExtrasOrThrow(): KotlinToolingMetadata.ProjectTargetMetadata.JsExtras {
    return KotlinToolingMetadata.ProjectTargetMetadata.JsExtras(
        isBrowserConfigured = getOrThrow("isBrowserConfigured").asBoolean,
        isNodejsConfigured = getOrThrow("isNodejsConfigured").asBoolean
    )
}

private fun JsonObject.toNativeExtrasOrThrow(): KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras {
    return KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras(
        konanTarget = getOrThrow("konanTarget").asString,
        konanVersion = getOrThrow("konanVersion").asString,
        konanAbiVersion = getOrThrow("konanAbiVersion").asString
    )
}

private fun JsonObject.toAndroidExtrasOrThrow(): KotlinToolingMetadata.ProjectTargetMetadata.AndroidExtras {
    return KotlinToolingMetadata.ProjectTargetMetadata.AndroidExtras(
        sourceCompatibility = getOrThrow("sourceCompatibility").asString,
        targetCompatibility = getOrThrow("targetCompatibility").asString
    )
}

private fun JsonObject.getOrThrow(key: String): JsonElement {
    return get(key) ?: throw IllegalArgumentException("Missing key: $key")
}
