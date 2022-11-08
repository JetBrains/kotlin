/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.jetbrains.kotlin.tooling.core.Extras

interface IdeExtrasSerializationExtension {
    fun <T : Any> serializer(key: Extras.Key<T>): IdeExtrasSerializer<T>?

    object Empty : IdeExtrasSerializationExtension {
        override fun <T : Any> serializer(key: Extras.Key<T>): IdeExtrasSerializer<T>? = null
    }
}

interface IdeExtrasSerializationExtensionBuilder {
    fun <T : Any> register(key: Extras.Key<T>, serializer: IdeExtrasSerializer<T>)
}

fun IdeExtrasSerializationExtension(
    builder: IdeExtrasSerializationExtensionBuilder.() -> Unit
): IdeExtrasSerializationExtension {
    return IdeaKpmExtrasSerializationExtensionImpl(
        IdeaExtrasSerializationExtensionBuilderImpl().apply(builder).serializers
    )
}

private class IdeaExtrasSerializationExtensionBuilderImpl : IdeExtrasSerializationExtensionBuilder {
    val serializers = mutableMapOf<Extras.Key<*>, IdeExtrasSerializer<*>>()

    override fun <T : Any> register(key: Extras.Key<T>, serializer: IdeExtrasSerializer<T>) {
        serializers[key] = serializer
    }
}


private class IdeaKpmExtrasSerializationExtensionImpl(
    private val map: Map<Extras.Key<*>, IdeExtrasSerializer<*>>
) : IdeExtrasSerializationExtension {
    override fun <T : Any> serializer(key: Extras.Key<T>): IdeExtrasSerializer<T>? {
        @Suppress("unchecked_cast")
        return map[key] as? IdeExtrasSerializer<T>
    }
}
