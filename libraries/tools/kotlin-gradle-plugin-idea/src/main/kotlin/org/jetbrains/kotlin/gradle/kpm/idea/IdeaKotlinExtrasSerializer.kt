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

        class StreamLogger<T : Any>(private val stream: PrintStream = System.err) : ErrorHandler<T> {

            override fun onDeserializationFailure(key: Extras.Key<T>, error: Throwable): T? {
                stream.println("Failed to deserialize $key: ${error.message}")
                error.printStackTrace(stream)
                return null
            }

            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (javaClass != other?.javaClass) return false
                other as StreamLogger<*>
                if (stream != other.stream) return false
                return true
            }

            override fun hashCode(): Int {
                return stream.hashCode()
            }
        }
    }

    companion object {
        fun <T : Serializable> serializable(
            clazz: Class<T>, errorHandler: ErrorHandler<T>
        ): IdeaKotlinExtrasSerializer<T> = IdeaKotlinSerializableExtrasSerializer(clazz, errorHandler)

        inline fun <reified T : Serializable> serializable(
            errorHandler: ErrorHandler<T> = ErrorHandler.StreamLogger(System.err)
        ): IdeaKotlinExtrasSerializer<T> = serializable(T::class.java, errorHandler)
    }
}

private data class IdeaKotlinSerializableExtrasSerializer<T : Serializable>(
    private val clazz: Class<T>,
    private val errorHandler: IdeaKotlinExtrasSerializer.ErrorHandler<T>
) : IdeaKotlinExtrasSerializer<T>,
    IdeaKotlinExtrasSerializer.ErrorHandler<T> by errorHandler {
    override fun serialize(key: Extras.Key<T>, extra: T): ByteArray {
        return ByteArrayOutputStream().run {
            ObjectOutputStream(this).use { stream -> stream.writeObject(extra) }
            toByteArray()
        }
    }

    override fun deserialize(key: Extras.Key<T>, data: ByteArray): T? {
        return try {
            ObjectInputStream(ByteArrayInputStream(data)).use { stream ->
                clazz.cast(stream.readObject())
            }
        } catch (t: Throwable) {
            key.capability<IdeaKotlinExtrasSerializer.ErrorHandler<T>>()
                ?.onDeserializationFailure(key, t)
        }
    }
}
