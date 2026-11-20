/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBacked
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBackedSerializer

@Serializable(with = XcprojBuildSettingsValueSerializer::class)
internal sealed class XcprojBuildSettingValue : JsonElementBacked() {
    abstract val values: List<String>
}

@Serializable
internal data class XcprojBuildSettingList(override val values: List<String>) : XcprojBuildSettingValue()

@Serializable
internal data class XcprojBuildSettingString(val value: String) : XcprojBuildSettingValue() {
    override val values: List<String>
        get() = listOf(value)
}

@Serializable
internal class XcprojBuildSettingOpaque : XcprojBuildSettingValue() {
    override val values: List<String>
        get() = emptyList()
}

internal class XcprojBuildSettingsValueSerializer : KSerializer<XcprojBuildSettingValue> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("XcprojBuildSettingsValueSerializer")

    override fun serialize(encoder: Encoder, value: XcprojBuildSettingValue) {
        (encoder as JsonEncoder).encodeJsonElement(
            when (value) {
                is XcprojBuildSettingList -> encoder.json.encodeToJsonElement<List<String>>(value.values)
                is XcprojBuildSettingString -> encoder.json.encodeToJsonElement<String>(value.value)
                is XcprojBuildSettingOpaque -> encoder.json.encodeToJsonElement(
                    JsonElementBackedSerializer(XcprojBuildSettingOpaque.serializer()),
                    value
                )
            }
        )
    }

    override fun deserialize(decoder: Decoder): XcprojBuildSettingValue {
        decoder as JsonDecoder
        return when (val element = decoder.decodeJsonElement()) {
            is JsonArray -> XcprojBuildSettingList(decoder.json.decodeFromJsonElement<List<String>>(element))
            is JsonPrimitive -> XcprojBuildSettingString(decoder.json.decodeFromJsonElement<String>(element))
            else -> decoder.json.decodeFromJsonElement(JsonElementBackedSerializer(XcprojBuildSettingOpaque.serializer()), element)
        }
    }
}
