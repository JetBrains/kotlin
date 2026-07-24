/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Custom serializer for [KotlinToolingMetadata.ProjectTargetMetadata].
 *
 * JSON-only serializer. Non-JSON encoders/decoders are not supported because
 * [KotlinToolingMetadata] is exclusively serialized to/from JSON strings via
 * [KotlinToolingMetadata.toJsonString] and [KotlinToolingMetadata.Companion.parseJson].
 *
 * The serialization contract requires that:
 * - The `extras` field is written to JSON only when it has at least one non-null sub-field
 *   (matches legacy Gson behavior: `add("extras", extrasJsonObject).takeIf { it.size() > 0 }`)
 * - The `extras` field defaults to [KotlinToolingMetadata.ProjectTargetMetadata.Extras] (all nulls)
 *   when the JSON object does not contain the `extras` key (required for back-compat with schema 1.0.0)
 */
internal object ProjectTargetMetadataSerializer :
    KSerializer<KotlinToolingMetadata.ProjectTargetMetadata> {

    override val descriptor: SerialDescriptor = buildClassSerialDescriptor(
        KotlinToolingMetadata.ProjectTargetMetadata::class.java.name
    ) {
        element<String>("target")
        element<String>("platformType")
        element<KotlinToolingMetadata.ProjectTargetMetadata.Extras>("extras", isOptional = true)
    }

    private val extrasSerializer = KotlinToolingMetadata.ProjectTargetMetadata.Extras.serializer()

    override fun serialize(encoder: Encoder, value: KotlinToolingMetadata.ProjectTargetMetadata) {
        val jsonEncoder = encoder as? JsonEncoder
        if (jsonEncoder != null) {
            // Use JSON-aware path to conditionally omit extras
            val extrasJson = jsonEncoder.json.encodeToJsonElement(extrasSerializer, value.extras).jsonObject
            val obj = buildJsonObject {
                put("target", jsonEncoder.json.encodeToJsonElement(value.target))
                put("platformType", jsonEncoder.json.encodeToJsonElement(value.platformType))
                if (extrasJson.isNotEmpty()) {
                    put("extras", extrasJson)
                }
            }
            jsonEncoder.encodeJsonElement(obj)
        } else {
            error("${ProjectTargetMetadataSerializer::class.simpleName} supports JSON only")
        }
    }

    override fun deserialize(decoder: Decoder): KotlinToolingMetadata.ProjectTargetMetadata {
        val jsonDecoder = decoder as? JsonDecoder
        if (jsonDecoder != null) {
            val obj = jsonDecoder.decodeJsonElement().jsonObject
            val target = obj.getValue("target").let {
                jsonDecoder.json.decodeFromJsonElement<String>(it)
            }
            val platformType = obj.getValue("platformType").let {
                jsonDecoder.json.decodeFromJsonElement<String>(it)
            }
            val extras = obj["extras"]?.let {
                jsonDecoder.json.decodeFromJsonElement(extrasSerializer, it)
            } ?: KotlinToolingMetadata.ProjectTargetMetadata.Extras()
            return KotlinToolingMetadata.ProjectTargetMetadata(target, platformType, extras)
        } else {
            error("${ProjectTargetMetadataSerializer::class.simpleName} supports JSON only")
        }
    }
}
