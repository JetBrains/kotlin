/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.util

import kotlin.reflect.KProperty

data class TypedKey<T>(val name: String)

class TypedKeyDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): TypedKey<T> = TypedKey(property.name)
}

fun <T> typedKey() = TypedKeyDelegate<T>()

class ChainedPropertyBag(private val parent: ChainedPropertyBag? = null, pairs: Iterable<Pair<TypedKey<*>, Any?>>) {
    constructor(pairs: Iterable<Pair<TypedKey<*>, Any?>>) : this(null, pairs)
    constructor(parent: ChainedPropertyBag, vararg pairs: Pair<TypedKey<*>, Any?>) : this(parent, pairs.asIterable())
    constructor(vararg pairs: Pair<TypedKey<*>, Any?>) : this(null, pairs.asIterable())

    private val data = HashMap<TypedKey<*>, Any?>().also { it.putAll(pairs) }

    inline operator fun <reified T> get(key: TypedKey<T>): T = getUnchecked(key) as T

    fun <T> getUnchecked(key: TypedKey<T>): Any? =
        when {
            data.containsKey(key) -> data[key]
            parent != null -> parent.getUnchecked(key)
            else -> throw IllegalArgumentException("Unknown key $key")
        }

    inline fun <reified T> getOrNull(key: TypedKey<T>): T? = getOrNullUnchecked(key)?.let { it as T }

    fun <T> getOrNullUnchecked(key: TypedKey<T>): Any? = data[key] ?: parent?.getOrNullUnchecked(key)
}

