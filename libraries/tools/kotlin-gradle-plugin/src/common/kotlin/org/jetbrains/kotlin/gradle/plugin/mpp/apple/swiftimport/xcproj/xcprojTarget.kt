/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBacked
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.JsonElementBackedSerializer
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport.xcproj.XcprojNativeTarget.Companion.NATIVE_TARGET_KIND

@Serializable(with = XcprojTargetSerializer::class)
internal sealed class XcprojTarget : JsonElementBacked()

@Serializable
internal class XcprojNativeTarget(
    val kind: String = NATIVE_TARGET_KIND,
    val name: String = "",
    @SerialName("product-type") val productType: String? = null,
    @SerialName("full-product-type") val fullProductType: String? = null,
    @SerialName("build-settings") val buildSettings: MutableMap<String, XcprojBuildSettingValue> = mutableMapOf(),
    @SerialName("build-phases") val buildPhases: MutableList<XcprojBuildPhase> = mutableListOf(),
    val dependencies: MutableList<XcprojTargetDependency> = mutableListOf(),
    @SerialName("package-product-members") val packageProductMembers: MutableList<XcprojSwiftPackageProductMember> = mutableListOf(),
) : XcprojTarget() {
    val resolvedProductType: String?
        get() = fullProductType ?: productType?.let { "com.apple.product-type.$it" }

    fun hasEmbedAndSignIntegration(): Boolean = buildPhases.filterIsInstance<XcprojScriptBuildPhase>().any { phase ->
        phase.script.stringValue.contains("gradle")
    }

    companion object {
        const val NATIVE_TARGET_KIND = "native"
    }
}

@Serializable
internal class XcprojOpaqueTarget : XcprojTarget()

internal class XcprojTargetSerializer : KSerializer<XcprojTarget> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("XcprojTargetSerializer")

    override fun serialize(
        encoder: Encoder,
        value: XcprojTarget,
    ) {
        when (value) {
            is XcprojNativeTarget -> JsonElementBackedSerializer(XcprojNativeTarget.serializer()).serialize(encoder, value)
            is XcprojOpaqueTarget -> JsonElementBackedSerializer(XcprojOpaqueTarget.serializer()).serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): XcprojTarget {
        decoder as JsonDecoder
        val element = decoder.decodeJsonElement()
        val kind = element.jsonObject["kind"]?.jsonPrimitive?.content ?: NATIVE_TARGET_KIND
        return when (kind) {
            NATIVE_TARGET_KIND -> decoder.json.decodeFromJsonElement(JsonElementBackedSerializer(XcprojNativeTarget.serializer()), element)
            else -> decoder.json.decodeFromJsonElement(JsonElementBackedSerializer(XcprojOpaqueTarget.serializer()), element)
        }
    }
}
