/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.serialize

import org.jetbrains.kotlin.tooling.core.Extras

interface IdeaExtrasSerializationExtension {
    fun <T : Any> serializer(key: Extras.Key<T>): IdeaExtrasSerializer<T>?

    object Empty : IdeaExtrasSerializationExtension {
        override fun <T : Any> serializer(key: Extras.Key<T>): IdeaExtrasSerializer<T>? = null
    }
}

interface IdeaExtrasSerializationExtensionBuilder {
    fun <T : Any> register(key: Extras.Key<T>, serializer: IdeaExtrasSerializer<T>)
}

fun IdeaExtrasSerializationExtension(
    builder: IdeaExtrasSerializationExtensionBuilder.() -> Unit
): IdeaExtrasSerializationExtension {
    return IdeaExtrasSerializationExtensionImpl(
        IdeaExtrasSerializationExtensionBuilderImpl().apply(builder).serializers
    )
}

private class IdeaExtrasSerializationExtensionBuilderImpl : IdeaExtrasSerializationExtensionBuilder {
    val serializers = mutableMapOf<Extras.Key<*>, IdeaExtrasSerializer<*>>()

    override fun <T : Any> register(key: Extras.Key<T>, serializer: IdeaExtrasSerializer<T>) {
        serializers[key] = serializer
    }
}


private class IdeaExtrasSerializationExtensionImpl(
    private val map: Map<Extras.Key<*>, IdeaExtrasSerializer<*>>
) : IdeaExtrasSerializationExtension {
    override fun <T : Any> serializer(key: Extras.Key<T>): IdeaExtrasSerializer<T>? {
        @Suppress("unchecked_cast")
        return map[key] as? IdeaExtrasSerializer<T>
    }
}
