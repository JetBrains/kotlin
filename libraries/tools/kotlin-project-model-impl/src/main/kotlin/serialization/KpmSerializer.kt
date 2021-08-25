/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.serialization

import kotlinx.serialization.json.*

interface KpmSerializer {
    fun serialize(kpm: KotlinModuleDto): ByteArray
    fun deserialize(content: ByteArray): KotlinModuleDto

    fun serializeVariantInfo(info: KpmVariantInfo): ByteArray
    fun deserializeVariantInfo(content: ByteArray): KpmVariantInfo
}

class JsonKpmSerializer(config: JsonBuilder.() -> Unit): KpmSerializer {
    private val json: Json = Json {
        config()

        allowStructuredMapKeys = true
    }

    override fun serialize(kpm: KotlinModuleDto): ByteArray {
        return json.encodeToString(KotlinModuleDto.serializer(), kpm).toByteArray()
    }

    override fun deserialize(content: ByteArray): KotlinModuleDto {
        return json.decodeFromString(KotlinModuleDto.serializer(), String(content))
    }

    override fun serializeVariantInfo(info: KpmVariantInfo): ByteArray {
        return json.encodeToString(KpmVariantInfo.serializer(), info).toByteArray()
    }

    override fun deserializeVariantInfo(content: ByteArray): KpmVariantInfo {
        return json.decodeFromString(KpmVariantInfo.serializer(), String(content))
    }
}