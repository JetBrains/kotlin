/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi
import java.io.Serializable

internal class KotlinMutableExternalModelContainerImpl : KotlinMutableExternalModelContainer(), Serializable {
    private val values = mutableMapOf<KotlinExternalModelKey<*>, Any>()

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
        val serializedValues = values.filterKeys { it.serializer != null }
            .mapValues { (key, value) ->
                @Suppress("unchecked_cast")
                val serializer = checkNotNull(key.serializer) as KotlinExternalModelSerializer<Any>
                serializer.serialize(value)
            }.mapKeys { (key, _) -> key.id }
            .toMutableMap()

        return SerializedKotlinExternalModelContainer(serializedValues)
    }
}

private class SerializedKotlinExternalModelContainer(
    private val serializedValues: MutableMap<KotlinExternalModelId<*>, ByteArray>
) : KotlinExternalModelContainer(), Serializable {

    private val deserializedValues = mutableMapOf<KotlinExternalModelId<*>, Any>()

    override val ids: Set<KotlinExternalModelId<*>>
        @Synchronized get() = serializedValues.keys + deserializedValues.keys

    @Synchronized
    override fun <T : Any> contains(key: KotlinExternalModelKey<T>): Boolean {
        return key.id in deserializedValues || key.id in serializedValues
    }

    @Synchronized
    @Suppress("unchecked_cast")
    override fun <T : Any> get(key: KotlinExternalModelKey<T>): T? {
        deserializedValues[key.id]?.let { return it as T }
        val serializedValue = serializedValues.remove(key.id) ?: return null
        val deserializedValue = key.serializer?.deserialize(serializedValue) ?: return null
        deserializedValues[key.id] = deserializedValue
        return deserializedValue
    }

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
