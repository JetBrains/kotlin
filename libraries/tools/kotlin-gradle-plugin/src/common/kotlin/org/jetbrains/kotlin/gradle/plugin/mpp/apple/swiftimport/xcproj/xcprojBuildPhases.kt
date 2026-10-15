/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBacked
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBackedSerializer
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.StringOrStringList

@Serializable(with = XcprojBuildPhaseSerializer::class)
internal sealed class XcprojBuildPhase : JsonElementBacked()

@Serializable
internal data class XcprojScriptBuildPhase(
    @EncodeDefault val kind: String = "script",
    val name: String? = null,
    val shell: String,
    val script: StringOrStringList,
    @SerialName("run-on-every-build") val runOnEveryBuild: Boolean = false,
) : XcprojBuildPhase()

@Serializable
internal data class XcprojFrameworkBuildPhase(
    @EncodeDefault val kind: String = "frameworks",
    val name: String? = null,
    val id: String? = null,
    @Transient
    val isLiteral: Boolean = false,
) : XcprojBuildPhase() {
    val reference = id?.let { "id:$it" } ?: name ?: kind

    fun encodeAsLiteral() = isLiteral && name == null && id == null
}

internal class XcprojOpaqueLiteralBuildPhase(val value: String) : XcprojBuildPhase()

@Serializable
internal class XcprojOpaqueBuildPhase : XcprojBuildPhase()

internal class XcprojBuildPhaseSerializer : KSerializer<XcprojBuildPhase> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("XcprojBuildPhaseSerializer")

    override fun serialize(
        encoder: Encoder,
        value: XcprojBuildPhase,
    ) {
        encoder as JsonEncoder
        when (value) {
            is XcprojScriptBuildPhase -> JsonElementBackedSerializer(XcprojScriptBuildPhase.serializer()).serialize(encoder, value)
            is XcprojFrameworkBuildPhase -> if (value.encodeAsLiteral()) {
                encoder.encodeJsonElement(encoder.json.encodeToJsonElement(value.kind))
            } else JsonElementBackedSerializer(XcprojFrameworkBuildPhase.serializer()).serialize(encoder, value)
            is XcprojOpaqueBuildPhase -> JsonElementBackedSerializer(XcprojOpaqueBuildPhase.serializer()).serialize(encoder, value)
            is XcprojOpaqueLiteralBuildPhase -> encoder.encodeJsonElement(encoder.json.encodeToJsonElement(value.value))
        }
    }

    override fun deserialize(decoder: Decoder): XcprojBuildPhase {
        decoder as JsonDecoder
        val element = decoder.decodeJsonElement()
        return when (element) {
            is JsonPrimitive -> when (element.content) {
                "frameworks" -> XcprojFrameworkBuildPhase(isLiteral = true)
                else -> XcprojOpaqueLiteralBuildPhase(element.content)
            }
            else -> {
                val kind = element.jsonObject["kind"]?.jsonPrimitive?.content ?: error("Missing kind")
                when (kind) {
                    "frameworks" -> decoder.json.decodeFromJsonElement(
                        JsonElementBackedSerializer(XcprojFrameworkBuildPhase.serializer()),
                        element
                    )
                    "script" -> decoder.json.decodeFromJsonElement(
                        JsonElementBackedSerializer(XcprojScriptBuildPhase.serializer()),
                        element
                    )
                    else -> decoder.json.decodeFromJsonElement(
                        JsonElementBackedSerializer(XcprojOpaqueBuildPhase.serializer()),
                        element
                    )
                }
            }
        }
    }
}


