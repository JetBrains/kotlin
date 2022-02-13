/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.kpm

inline fun <reified T : Any> KotlinExternalModelKey(
    disambiguationName: String? = null, serializer: KotlinExternalModelSerializer<T>? = null
): KotlinExternalModelKey<T> = KotlinExternalModelKey(KotlinExternalModelId(disambiguationName), serializer)

inline fun <reified T : Any> KotlinExternalModelKey(serializer: KotlinExternalModelSerializer<T>): KotlinExternalModelKey<T> =
    KotlinExternalModelKey(KotlinExternalModelId(), serializer)

class KotlinExternalModelKey<T : Any> @PublishedApi internal constructor(
    internal val id: KotlinExternalModelId<T>,
    internal val serializer: KotlinExternalModelSerializer<T>? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KotlinExternalModelKey<*>

        if (id != other.id) return false
        if (serializer != other.serializer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (serializer?.hashCode() ?: 0)
        return result
    }
}
