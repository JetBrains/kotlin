/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.project.modelx.languageSetting

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonPrimitive

/**
 * Language Setting Value Serializer
 */
interface LSValueSerializer<T> {
    fun serialize(settingValue: T): JsonElement
    fun deserialize(json: JsonElement): T
}

object StringSerializer : LSValueSerializer<String> {
    override fun serialize(settingValue: String) = JsonPrimitive(settingValue)
    override fun deserialize(json: JsonElement) = json.jsonPrimitive.content
}

object BoolSerializer : LSValueSerializer<Boolean> {
    override fun serialize(settingValue: Boolean) = JsonPrimitive(settingValue)
    override fun deserialize(json: JsonElement) = json.jsonPrimitive.boolean
}

class EnumSerializer<E: Enum<E>>(
    private val valueMap: Map<E, String>
) : LSValueSerializer<E> {
    private val inverseMap by lazy { valueMap.entries.associate { it.value to it.key } }

    override fun serialize(settingValue: E) = JsonPrimitive(valueMap[settingValue] ?: error("Unknown value: $settingValue"))
    override fun deserialize(json: JsonElement): E = inverseMap[json.jsonPrimitive.content] ?: error("Unknown value: $json")
}

inline fun <reified E: Enum<E>> EnumSerializer() = EnumSerializer(
    valueMap = enumValues<E>().associateWith { it.name }
)
