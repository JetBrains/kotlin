/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import kotlin.reflect.KClass


interface IdeExtrasSerializer<T : Any> {
    fun serialize(context: IdeSerializationContext, value: T): ByteArray?
    fun deserialize(context: IdeSerializationContext, data: ByteArray): T?

    companion object {
        /**
         * Returns a [IdeExtrasSerializer] based upon [java.io.Serializable]
         */
        fun <T : Any> javaIoSerializable(clazz: KClass<T>): IdeExtrasSerializer<T> {
            return IdeJavaIoSerializableExtrasSerializer(clazz)
        }

        inline fun <reified T : Any> javaIoSerializable(): IdeExtrasSerializer<T> {
            return javaIoSerializable(T::class)
        }
    }
}
