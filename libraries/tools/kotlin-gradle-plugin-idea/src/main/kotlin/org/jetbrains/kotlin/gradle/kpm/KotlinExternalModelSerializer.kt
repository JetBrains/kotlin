/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.kpm.idea.InternalKotlinGradlePluginApi
import java.io.*

interface KotlinExternalModelSerializer<T : Any> {
    fun serialize(value: T): ByteArray
    fun deserialize(data: ByteArray): T

    @InternalKotlinGradlePluginApi
    companion object {
        inline fun <reified T : Serializable> serializable(): KotlinExternalModelSerializer<T> =
            KotlinSerializableExternalModelSerializer(T::class.java)
    }
}

@PublishedApi
internal class KotlinSerializableExternalModelSerializer<T : Serializable>(
    private val clazz: Class<T>
) : KotlinExternalModelSerializer<T> {
    override fun serialize(value: T): ByteArray {
        return ByteArrayOutputStream().run {
            ObjectOutputStream(this).use { stream -> stream.writeObject(value) }
            toByteArray()
        }
    }

    override fun deserialize(data: ByteArray): T {
        return ObjectInputStream(ByteArrayInputStream(data)).use { stream ->
            clazz.cast(stream.readObject())
        }
    }
}
