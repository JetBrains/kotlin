/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

@OptIn(ExperimentalSerializationApi::class)
private val kgpJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    encodeDefaults = true
    explicitNulls = false
}

fun KotlinToolingMetadata.toJsonString(): String =
    kgpJson.encodeToString(KotlinToolingMetadata.serializer(), this)

sealed class KotlinToolingMetadataParsingResult {
    data class Success(val value: KotlinToolingMetadata) : KotlinToolingMetadataParsingResult()
    data class Failure(val reason: String) : KotlinToolingMetadataParsingResult()
}

fun KotlinToolingMetadata.Companion.parseJson(json: String): KotlinToolingMetadataParsingResult {
    val parsed = try {
        kgpJson.decodeFromString(KotlinToolingMetadata.serializer(), json)
    } catch (e: SerializationException) {
        return KotlinToolingMetadataParsingResult.Failure("Failed parsing JsonObject: ${e.message}")
    } catch (e: IllegalArgumentException) {
        return KotlinToolingMetadataParsingResult.Failure("Invalid json: ${e.message}")
    }

    val schemaVersion = try {
        SchemaVersion.parseStringOrThrow(parsed.schemaVersion)
    } catch (e: IllegalArgumentException) {
        return KotlinToolingMetadataParsingResult.Failure("Failed parsing JsonObject: ${e.message}")
    }

    if (!SchemaVersion.current.isCompatible(schemaVersion)) {
        return KotlinToolingMetadataParsingResult.Failure(
            "Incompatible schemaVersion='$schemaVersion' found. Current schemaVersion='${SchemaVersion.current}'"
        )
    }

    return KotlinToolingMetadataParsingResult.Success(parsed)
}

fun KotlinToolingMetadata.Companion.parseJsonOrThrow(value: String): KotlinToolingMetadata {
    return when (val result = parseJson(value)) {
        is KotlinToolingMetadataParsingResult.Success -> result.value
        is KotlinToolingMetadataParsingResult.Failure -> throw IllegalArgumentException(result.reason)
    }
}
