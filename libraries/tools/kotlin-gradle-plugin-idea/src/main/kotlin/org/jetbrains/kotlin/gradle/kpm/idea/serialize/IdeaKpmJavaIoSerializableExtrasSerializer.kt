/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.serialize

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import kotlin.reflect.KClass

internal class IdeaKpmJavaIoSerializableExtrasSerializer<T : Any>(
    private val clazz: KClass<T>
) : IdeaKpmExtrasSerializer<T> {

    override fun serialize(context: IdeaKpmSerializationContext, value: T): ByteArray? {
        return try {
            ByteArrayOutputStream().use { byteArrayOutputStream ->
                ObjectOutputStream(byteArrayOutputStream).use { oos -> oos.writeObject(value) }
                byteArrayOutputStream.toByteArray()
            }
        } catch (t: Throwable) {
            context.logger.error("${ErrorMessages.SERIALIZATION_FAILURE} $value", t)
            null
        }
    }

    override fun deserialize(context: IdeaKpmSerializationContext, data: ByteArray): T? {
        return try {
            ObjectInputStream(ByteArrayInputStream(data)).use { stream -> clazz.java.cast(stream.readObject()) }
        } catch (t: Throwable) {
            context.logger.error("${ErrorMessages.DESERIALIZATION_FAILURE} ${clazz.java}", t)
            null
        }
    }

    object ErrorMessages {
        const val SERIALIZATION_FAILURE = "Failed to serialize"
        const val DESERIALIZATION_FAILURE = "Failed to deserialize"
    }
}
