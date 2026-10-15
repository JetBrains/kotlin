/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBacked
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBackedSerializer

@Serializable(with = XcprojPackageLocationSerializer::class)
internal sealed class XcprojPackageLocation : JsonElementBacked()

@Serializable
internal data class XcprojPackageLocationLocal(
    @EncodeDefault val kind: String = "local",
    val path: String,
) : XcprojPackageLocation()

@Serializable
internal data class XcprojPackageLocationRemote(
    @EncodeDefault val kind: String = "remote",
    val repositoryURL: String,
) : XcprojPackageLocation()

@Serializable
internal class XcprojPackageLocationOpaque : XcprojPackageLocation()

internal class XcprojPackageLocationSerializer : KSerializer<XcprojPackageLocation> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("XcprojPackageLocationSerializer")

    override fun serialize(encoder: Encoder, value: XcprojPackageLocation) {
        encoder as JsonEncoder
        encoder.encodeJsonElement(
            when (value) {
                is XcprojPackageLocationLocal -> encoder.json.encodeToJsonElement(
                    JsonElementBackedSerializer(XcprojPackageLocationLocal.serializer()),
                    value
                )
                is XcprojPackageLocationRemote -> encoder.json.encodeToJsonElement(
                    JsonElementBackedSerializer(XcprojPackageLocationRemote.serializer()),
                    value
                )
                is XcprojPackageLocationOpaque -> encoder.json.encodeToJsonElement(
                    JsonElementBackedSerializer(XcprojPackageLocationOpaque.serializer()),
                    value
                )
            }
        )
    }

    override fun deserialize(decoder: Decoder): XcprojPackageLocation {
        decoder as JsonDecoder
        val element = decoder.decodeJsonElement()
        val kind = element.jsonObject["kind"]?.jsonPrimitive?.content ?: error("Missing kind")
        return when (kind) {
            "local" -> decoder.json.decodeFromJsonElement(JsonElementBackedSerializer(XcprojPackageLocationLocal.serializer()), element)
            "remote" -> decoder.json.decodeFromJsonElement(JsonElementBackedSerializer(XcprojPackageLocationRemote.serializer()), element)
            else -> decoder.json.decodeFromJsonElement(JsonElementBackedSerializer(XcprojPackageLocationOpaque.serializer()), element)
        }
    }
}

