/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.tooling.core.Extras
import java.io.*

interface IdeaKotlinExtraSerializer<T : Any> : Extras.Key.Capability<T> {
    fun serialize(extra: T): ByteArray
    fun deserialize(data: ByteArray): T?

    companion object {
        fun <T : Serializable> serializable(clazz: Class<T>): IdeaKotlinExtraSerializer<T> =
            IdeaKotlinSerializableExtraSerializer(clazz)

        inline fun <reified T : Serializable> serializable(): IdeaKotlinExtraSerializer<T> =
            serializable(T::class.java)
    }
}

private class IdeaKotlinSerializableExtraSerializer<T : Serializable>(
    private val clazz: Class<T>
) : IdeaKotlinExtraSerializer<T> {
    override fun serialize(extra: T): ByteArray {
        return ByteArrayOutputStream().run {
            ObjectOutputStream(this).use { stream -> stream.writeObject(extra) }
            toByteArray()
        }
    }

    override fun deserialize(data: ByteArray): T {
        return ObjectInputStream(ByteArrayInputStream(data)).use { stream ->
            clazz.cast(stream.readObject())
        }
    }
}
