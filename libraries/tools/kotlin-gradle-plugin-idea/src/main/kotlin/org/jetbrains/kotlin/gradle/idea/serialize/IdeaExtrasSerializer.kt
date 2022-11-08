/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.serialize

import kotlin.reflect.KClass


interface IdeaExtrasSerializer<T : Any> {
    fun serialize(context: IdeaSerializationContext, value: T): ByteArray?
    fun deserialize(context: IdeaSerializationContext, data: ByteArray): T?

    companion object {
        /**
         * Returns a [IdeaExtrasSerializer] based upon [java.io.Serializable]
         */
        fun <T : Any> javaIoSerializable(clazz: KClass<T>): IdeaExtrasSerializer<T> {
            return IdeaJavaIoSerializableExtrasSerializer(clazz)
        }

        inline fun <reified T : Any> javaIoSerializable(): IdeaExtrasSerializer<T> {
            return javaIoSerializable(T::class)
        }
    }
}
