/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.serialize

import org.jetbrains.kotlin.tooling.core.Extras

interface IdeaKotlinExtrasSerializationExtension {
    fun <T : Any> serializer(key: Extras.Key<T>): IdeaKotlinExtrasSerializer<T>?

    object Empty : IdeaKotlinExtrasSerializationExtension {
        override fun <T : Any> serializer(key: Extras.Key<T>): IdeaKotlinExtrasSerializer<T>? = null
    }
}

interface IdeaKotlinExtrasSerializationExtensionBuilder {
    fun <T : Any> register(key: Extras.Key<T>, serializer: IdeaKotlinExtrasSerializer<T>)
}

fun IdeaKotlinExtrasSerializationExtension(
    builder: IdeaKotlinExtrasSerializationExtensionBuilder.() -> Unit
): IdeaKotlinExtrasSerializationExtension {
    return IdeaKotlinExtrasSerializationExtensionImpl(
        IdeaKotlinExtrasSerializationExtensionBuilderImpl().apply(builder).serializers
    )
}

private class IdeaKotlinExtrasSerializationExtensionBuilderImpl : IdeaKotlinExtrasSerializationExtensionBuilder {
    val serializers = mutableMapOf<Extras.Key<*>, IdeaKotlinExtrasSerializer<*>>()

    override fun <T : Any> register(key: Extras.Key<T>, serializer: IdeaKotlinExtrasSerializer<T>) {
        serializers[key] = serializer
    }
}

private class IdeaKotlinExtrasSerializationExtensionImpl(
    private val map: Map<Extras.Key<*>, IdeaKotlinExtrasSerializer<*>>
) : IdeaKotlinExtrasSerializationExtension {
    override fun <T : Any> serializer(key: Extras.Key<T>): IdeaKotlinExtrasSerializer<T>? {
        @Suppress("unchecked_cast")
        return map[key] as? IdeaKotlinExtrasSerializer<T>
    }
}
