/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Custom serializer for [KotlinToolingMetadata.ProjectTargetMetadata].
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
        "org.jetbrains.kotlin.tooling.KotlinToolingMetadata.ProjectTargetMetadata"
    ) {
        element<String>("target")
        element<String>("platformType")
        element<KotlinToolingMetadata.ProjectTargetMetadata.Extras>("extras", isOptional = true)
    }

    private val extrasSerializer = KotlinToolingMetadata.ProjectTargetMetadata.Extras.serializer()

    @OptIn(ExperimentalSerializationApi::class)
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
            // Fallback for non-JSON encoders: always write extras
            encoder.encodeStructure(descriptor) {
                encodeStringElement(descriptor, 0, value.target)
                encodeStringElement(descriptor, 1, value.platformType)
                encodeSerializableElement(descriptor, 2, extrasSerializer, value.extras)
            }
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
            // Fallback for non-JSON decoders
            var target = ""
            var platformType = ""
            var extras = KotlinToolingMetadata.ProjectTargetMetadata.Extras()
            decoder.decodeStructure(descriptor) {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> target = decodeStringElement(descriptor, 0)
                        1 -> platformType = decodeStringElement(descriptor, 1)
                        2 -> extras = decodeSerializableElement(descriptor, 2, extrasSerializer)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index $index")
                    }
                }
            }
            return KotlinToolingMetadata.ProjectTargetMetadata(target, platformType, extras)
        }
    }
}
