/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import java.io.ByteArrayInputStream
import java.io.OutputStream

internal open class JsonElementBacked {
    @Transient
    var jsonElement: JsonElement? = null
}

private class JsonElementBackedSerializer<T : JsonElementBacked>(
    private val typeSerializer: KSerializer<T>,
) : KSerializer<T> {

    override val descriptor: SerialDescriptor = typeSerializer.descriptor

    override fun deserialize(decoder: Decoder): T {
        decoder as JsonDecoder
        val rawElement = decoder.decodeJsonElement()
        val typedEntity = decoder.json.decodeFromJsonElement(typeSerializer, rawElement)
        typedEntity.jsonElement = rawElement
        return typedEntity
    }

    override fun serialize(encoder: Encoder, value: T) {
        encoder as JsonEncoder
        val originalJsonElement = value.jsonElement
        val entityJsonElement = encoder.json.encodeToJsonElement(typeSerializer, value)
        encoder.encodeJsonElement(originalJsonElement?.deepMergePreferOther(entityJsonElement) ?: entityJsonElement)
    }
}

private fun JsonElement.deepMergePreferOther(other: JsonElement): JsonElement =
    when {
        this is JsonObject && other is JsonObject -> {
            val merged = (this.keys + other.keys).associateWith { k ->
                val thisValue = this[k]
                val otherValue = other[k]
                when {
                    thisValue == null -> otherValue!!
                    otherValue == null -> thisValue
                    thisValue is JsonObject && otherValue is JsonObject -> thisValue.deepMergePreferOther(otherValue)
                    else -> otherValue
                }
            }
            JsonObject(merged)
        }
        else -> other
    }

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    explicitNulls = false
}

@Serializable(with = PbxObjectEntitySerializer::class)
internal sealed class PbxObject

internal class PbxObjectEntitySerializer : JsonContentPolymorphicSerializer<PbxObject>(PbxObject::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<out PbxObject> {
        val isa = element.jsonObject["isa"]?.jsonPrimitive?.content ?: error("Missing isa")
        return when (isa) {
            "PBXProject" -> PbxProject.serializer()
            "XCSwiftPackageProductDependency" -> XCSwiftPackageProductDependency.serializer()
            "XCLocalSwiftPackageReference" -> XCLocalSwiftPackageReference.serializer()
            else -> Opaque.serializer()
        }
    }
}

@Serializable
internal class PbxProject(
    val isa: String,
    val buildPhases: MutableList<String>?,
) : PbxObject()

@Serializable
internal class XCSwiftPackageProductDependency(
    val isa: String = "XCSwiftPackageProductDependency",
) : PbxObject()

@Serializable
internal class XCLocalSwiftPackageReference(
    val isa: String = "XCLocalSwiftPackageReference",
    var relativePath: String? = null,
) : PbxObject() {}

// This entity holds all the other entries we don't care about
@Serializable
internal class Opaque(
    val isa: String
) : PbxObject()

@Serializable
internal class XcodeProject(
    val objects: MutableMap<String, PbxObject>,
) : JsonElementBacked()

internal fun deserializeXcodeProject(byteArray: ByteArray) =
    json.decodeFromStream(
        JsonElementBackedSerializer(XcodeProject.serializer()),
        ByteArrayInputStream(byteArray)
    )

internal fun XcodeProject.serializeXcodeProject(outputStream: OutputStream) =
    json.encodeToStream(
        JsonElementBackedSerializer(XcodeProject.serializer()),
        this,
        outputStream
    )