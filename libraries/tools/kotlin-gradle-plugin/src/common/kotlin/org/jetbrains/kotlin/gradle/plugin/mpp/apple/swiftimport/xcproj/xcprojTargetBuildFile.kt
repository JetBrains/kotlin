/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBacked
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBackedSerializer

@Serializable
internal data class XcprojTargetBuildFile(
    @SerialName("build-phase") val buildPhase: XcprojTargetBuildPhaseReference,
)

@Serializable(with = XcprojTargetBuildPhaseReferenceValueSerializer::class)
internal sealed class XcprojTargetBuildPhaseReference {
    internal data class Literal(
        val value: String,
    ) : XcprojTargetBuildPhaseReference()

    internal data class Structured(
        val kind: String,
        val name: String?,
    ) : XcprojTargetBuildPhaseReference()
}

/**
 * [XcprojTargetBuildPhaseReference] has 2 forms:
 * - a string literal
 * - an array of up to 2 components where first is a string literal and the second is an object with the "name" field or also a literal
 */
internal class XcprojTargetBuildPhaseReferenceValueSerializer : KSerializer<XcprojTargetBuildPhaseReference> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("XcprojTargetBuildPhaseReference")

    override fun serialize(encoder: Encoder, value: XcprojTargetBuildPhaseReference) {
        encoder as JsonEncoder
        encoder.encodeJsonElement(
            when (value) {
                is XcprojTargetBuildPhaseReference.Literal -> JsonPrimitive(value.value)
                is XcprojTargetBuildPhaseReference.Structured -> JsonArray(
                    listOfNotNull(
                        JsonPrimitive(value.kind),
                        value.name?.let { name -> JsonObject(mapOf("name" to JsonPrimitive(name))) },
                    )
                )
            }
        )
    }

    override fun deserialize(decoder: Decoder): XcprojTargetBuildPhaseReference {
        decoder as JsonDecoder
        val element = decoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> XcprojTargetBuildPhaseReference.Literal(element.content)
            is JsonArray -> deserializeStructured(element)
            else -> error("Expected target build phase reference, got $element")
        }
    }

    private fun deserializeStructured(elements: JsonArray): XcprojTargetBuildPhaseReference {
        if (elements.size !in 1..2) {
            error("Target build phase reference must have one or two components: $elements")
        }

        val components = elements.map { element ->
            when (element) {
                is JsonPrimitive -> element.content
                is JsonObject -> element["name"]?.jsonPrimitive?.content
                    ?: error("Missing name in target build phase reference: $elements")
                else -> error("Invalid target build phase component in reference: $elements")
            }
        }
        return XcprojTargetBuildPhaseReference.Structured(components.first(), components.getOrNull(1))
    }
}
