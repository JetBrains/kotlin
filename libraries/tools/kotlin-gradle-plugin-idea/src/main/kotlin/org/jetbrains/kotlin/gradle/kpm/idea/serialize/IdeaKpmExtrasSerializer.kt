/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.serialize

import kotlin.reflect.KClass

interface IdeaKpmExtrasSerializer<T : Any> {
    fun serialize(context: IdeaKpmSerializationContext, value: T): ByteArray?
    fun deserialize(context: IdeaKpmSerializationContext, data: ByteArray): T?

    companion object {
        /**
         * Returns a [IdeaKpmExtrasSerializer] based upon [java.io.Serializable]
         */
        fun <T : Any> javaIoSerializable(clazz: KClass<T>): IdeaKpmExtrasSerializer<T> {
            return IdeaKpmJavaIoSerializableExtrasSerializer(clazz)
        }

        inline fun <reified T : Any> javaIoSerializable(): IdeaKpmExtrasSerializer<T> {
            return javaIoSerializable(T::class)
        }
    }
}


