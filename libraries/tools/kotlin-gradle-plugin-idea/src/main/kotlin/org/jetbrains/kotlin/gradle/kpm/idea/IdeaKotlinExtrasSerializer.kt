/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.tooling.core.Extras
import java.io.*

interface IdeaKotlinExtrasSerializer<T : Any> : Extras.Key.Capability<T> {
    fun serialize(key: Extras.Key<T>, extra: T): ByteArray
    fun deserialize(key: Extras.Key<T>, data: ByteArray): T?

    interface ErrorHandler<T : Any> : Extras.Key.Capability<T> {
        fun onDeserializationFailure(key: Extras.Key<T>, error: Throwable): T?

        class ConsoleLogger<T : Any> : ErrorHandler<T> {
            override fun onDeserializationFailure(key: Extras.Key<T>, error: Throwable): T? {
                println("Failed to deserialize $key: ${error.message}")
                error.printStackTrace()
                return null
            }
        }
    }

    companion object {
        fun <T : Serializable> serializable(clazz: Class<T>): IdeaKotlinExtrasSerializer<T> =
            IdeaKotlinSerializableExtrasSerializer(clazz)

        inline fun <reified T : Serializable> serializable(): IdeaKotlinExtrasSerializer<T> =
            serializable(T::class.java)
    }
}

private class IdeaKotlinSerializableExtrasSerializer<T : Serializable>(
    private val clazz: Class<T>
) : IdeaKotlinExtrasSerializer<T> {
    override fun serialize(key: Extras.Key<T>, extra: T): ByteArray {
        return ByteArrayOutputStream().run {
            ObjectOutputStream(this).use { stream -> stream.writeObject(extra) }
            toByteArray()
        }
    }

    override fun deserialize(key: Extras.Key<T>, data: ByteArray): T? {
        try {
            return ObjectInputStream(ByteArrayInputStream(data)).use { stream ->
                clazz.cast(stream.readObject())
            }
        } catch (t: Throwable) {
            return key.capability<IdeaKotlinExtrasSerializer.ErrorHandler<T>>()
                ?.onDeserializationFailure(key, t)
        }
    }
}
