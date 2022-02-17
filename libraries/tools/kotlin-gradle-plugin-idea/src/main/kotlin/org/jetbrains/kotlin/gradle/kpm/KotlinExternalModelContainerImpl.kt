/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import java.io.Serializable

internal class KotlinMutableExternalModelContainerImpl private constructor(
    private val values: MutableMap<KotlinExternalModelKey<*>, Any>
) : KotlinMutableExternalModelContainer(), Serializable {

    constructor() : this(mutableMapOf())

    override val ids: Set<KotlinExternalModelId<*>>
        @Synchronized get() = values.keys.map { it.id }.toSet()

    @Synchronized
    override fun <T : Any> set(key: KotlinExternalModelKey<T>, value: T) {
        values[key] = value
    }

    @Synchronized
    override fun <T : Any> contains(key: KotlinExternalModelKey<T>): Boolean {
        return key in values
    }

    @Synchronized
    @Suppress("unchecked_cast")
    override fun <T : Any> get(key: KotlinExternalModelKey<T>): T? {
        return values[key]?.let { it as T }
    }

    @Synchronized
    private fun writeReplace(): Any {
        return SerializedKotlinExternalModelContainerCarrier(serialize(values))
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

private class SerializedKotlinExternalModelContainer(
    private val serializedValues: MutableMap<KotlinExternalModelId<*>, ByteArray>
) : KotlinExternalModelContainer(), Serializable {

    private val deserializedValues = mutableMapOf<KotlinExternalModelKey<*>, Any>()

    override val ids: Set<KotlinExternalModelId<*>>
        @Synchronized get() = serializedValues.keys + deserializedValues.keys.map { it.id }

    @Synchronized
    override fun <T : Any> contains(key: KotlinExternalModelKey<T>): Boolean {
        return key.id in serializedValues || key in deserializedValues
    }

    @Synchronized
    @Suppress("unchecked_cast")
    override fun <T : Any> get(key: KotlinExternalModelKey<T>): T? {
        deserializedValues[key]?.let { return it as T }
        val serializedValue = serializedValues[key.id] ?: return null
        val deserializedValue = key.serializer?.deserialize(serializedValue) ?: return null
        deserializedValues[key] = deserializedValue
        serializedValues.remove(key.id)
        return deserializedValue
    }

    @Synchronized
    private fun writeReplace(): Any {
        return SerializedKotlinExternalModelContainerCarrier(serializedValues + serialize(deserializedValues))
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

private class SerializedKotlinExternalModelContainerCarrier(
    private val serializedValues: Map<KotlinExternalModelId<*>, ByteArray>
) : Serializable {

    private fun readResolve(): Any {
        return SerializedKotlinExternalModelContainer(serializedValues.toMutableMap())
    }

    companion object {
        private const val serialVersionUID = 0L
    }
}

private fun serialize(values: Map<KotlinExternalModelKey<*>, Any>): Map<KotlinExternalModelId<*>, ByteArray> {
    return values.filterKeys { it.serializer != null }
        .mapValues { (key, value) ->
            @Suppress("unchecked_cast")
            val serializer = checkNotNull(key.serializer) as KotlinExternalModelSerializer<Any>
            serializer.serialize(value)
        }.mapKeys { (key, _) -> key.id }
}
