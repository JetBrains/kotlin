/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.serialize

import kotlin.reflect.KClass


interface IdeaKotlinExtrasSerializer<T> {
    fun serialize(context: IdeaKotlinSerializationContext, value: T): ByteArray?
    fun deserialize(context: IdeaKotlinSerializationContext, data: ByteArray): T?

    companion object {
        /**
         * Returns a [IdeaKotlinExtrasSerializer] based upon [java.io.Serializable]
         */
        fun <T : Any> javaIoSerializable(clazz: KClass<T>): IdeaKotlinExtrasSerializer<T> {
            return IdeaKotlinJavaIoSerializableExtrasSerializer(clazz)
        }

        inline fun <reified T : Any> javaIoSerializable(): IdeaKotlinExtrasSerializer<T> {
            return javaIoSerializable(T::class)
        }
    }
}
