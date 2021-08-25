/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.serialization

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.serializer
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.project.modelx.JvmTargetAttribute
import org.jetbrains.kotlin.project.modelx.Platform
import org.jetbrains.kotlin.project.modelx.Platforms

object PlatformsSerializer : KSerializer<Platforms> {
    private val primitive = serializer<List<String>>()
    private val values = Platform.values().associateBy { it.name.lowercase() }

    override fun deserialize(decoder: Decoder): Platforms {
        val platforms = decoder
            .decodeSerializableValue(primitive)
            .map { values[it.lowercase()] ?: error("Unknown Platform: $it") }
            .toSet()

        return Platforms(platforms)
    }

    override val descriptor: SerialDescriptor = primitive.descriptor

    override fun serialize(encoder: Encoder, value: Platforms) {
        val list = value.platforms.map { it.name }
        encoder.encodeSerializableValue(primitive, list)
    }
}

object JvmTargetSerializer : KSerializer<JvmTargetAttribute> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("jvm.target", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder) = decoder
        .decodeString()
        .let { JvmTarget.fromString(it) }
        .let(::checkNotNull)
        .let(::JvmTargetAttribute)

    override fun serialize(encoder: Encoder, value: JvmTargetAttribute) {
        encoder.encodeString(value.value.description)
    }
}