/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.serialize

import org.jetbrains.kotlin.tooling.core.Extras

interface IdeaKpmExtrasSerializationExtension {
    fun <T : Any> serializer(key: Extras.Key<T>): IdeaKpmExtrasSerializer<T>?

    object Empty : IdeaKpmExtrasSerializationExtension {
        override fun <T : Any> serializer(key: Extras.Key<T>): IdeaKpmExtrasSerializer<T>? = null
    }
}

interface IdeaKpmExtrasSerializationExtensionBuilder {
    fun <T : Any> register(key: Extras.Key<T>, serializer: IdeaKpmExtrasSerializer<T>)
}

fun IdeaKpmExtrasSerializationExtension(
    builder: IdeaKpmExtrasSerializationExtensionBuilder.() -> Unit
): IdeaKpmExtrasSerializationExtension {
    return IdeaKpmExtrasSerializationExtensionImpl(
        IdeaKpmExtrasSerializationExtensionBuilderImpl().apply(builder).serializers
    )
}

private class IdeaKpmExtrasSerializationExtensionBuilderImpl : IdeaKpmExtrasSerializationExtensionBuilder {
    val serializers = mutableMapOf<Extras.Key<*>, IdeaKpmExtrasSerializer<*>>()

    override fun <T : Any> register(key: Extras.Key<T>, serializer: IdeaKpmExtrasSerializer<T>) {
        serializers[key] = serializer
    }
}


private class IdeaKpmExtrasSerializationExtensionImpl(
    private val map: Map<Extras.Key<*>, IdeaKpmExtrasSerializer<*>>
) : IdeaKpmExtrasSerializationExtension {
    override fun <T : Any> serializer(key: Extras.Key<T>): IdeaKpmExtrasSerializer<T>? {
        @Suppress("unchecked_cast")
        return map[key] as? IdeaKpmExtrasSerializer<T>
    }
}
