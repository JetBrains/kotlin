/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBacked
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBackedSerializer

@Serializable(with = XcprojTargetDependencySerializer::class)
internal sealed class XcprojTargetDependency : JsonElementBacked()

@Serializable
internal data class XcprojPackageTargetDependency(
    @EncodeDefault val kind: String = "package",
    @SerialName("product-name") val productName: String,
) : XcprojTargetDependency()

@Serializable
internal class XcprojOpaqueLiteralTargetDependency(val value: String) : XcprojTargetDependency()

@Serializable
internal class XcprojOpaqueTargetDependency : XcprojTargetDependency()

internal class XcprojTargetDependencySerializer : KSerializer<XcprojTargetDependency> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("XcprojTargetDependencySerializer")

    override fun serialize(
        encoder: Encoder,
        value: XcprojTargetDependency,
    ) {
        encoder as JsonEncoder
        when (value) {
            is XcprojPackageTargetDependency -> JsonElementBackedSerializer(XcprojPackageTargetDependency.serializer()).serialize(
                encoder,
                value
            )
            is XcprojOpaqueTargetDependency -> JsonElementBackedSerializer(XcprojOpaqueTargetDependency.serializer()).serialize(
                encoder,
                value
            )
            is XcprojOpaqueLiteralTargetDependency -> encoder.encodeString(value.value)
        }
    }

    override fun deserialize(decoder: Decoder): XcprojTargetDependency {
        decoder as JsonDecoder
        return when (val element = decoder.decodeJsonElement()) {
            is JsonPrimitive -> XcprojOpaqueLiteralTargetDependency(element.content)
            else -> {
                val kind = element.jsonObject["kind"]?.jsonPrimitive?.content ?: error("Missing kind")
                when (kind) {
                    "package" -> decoder.json.decodeFromJsonElement(
                        JsonElementBackedSerializer(XcprojPackageTargetDependency.serializer()),
                        element
                    )
                    else -> decoder.json.decodeFromJsonElement(
                        JsonElementBackedSerializer(XcprojOpaqueTargetDependency.serializer()),
                        element
                    )
                }
            }
        }
    }
}

