/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused")

package kotlin.script.experimental.util

import kotlin.reflect.KProperty

data class TypedKey<T>(val name: String, val defaultValue: T? = null)

class TypedKeyDelegate<T>(val defaultValue: T? = null) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): TypedKey<T> = TypedKey(property.name, defaultValue)
}

fun <T> typedKey(defaultValue: T? = null) = TypedKeyDelegate(defaultValue)

open class ChainedPropertyBag private constructor(private val parent: ChainedPropertyBag?, private val data: Map<TypedKey<*>, Any?>) {
    constructor(parent: ChainedPropertyBag? = null, pairs: Iterable<Pair<TypedKey<*>, Any?>>) :
            this(parent, HashMap<TypedKey<*>, Any?>().also { it.putAll(pairs) })

    constructor(pairs: Iterable<Pair<TypedKey<*>, Any?>>) : this(null, pairs)
    constructor(parent: ChainedPropertyBag, vararg pairs: Pair<TypedKey<*>, Any?>) : this(parent, pairs.asIterable())
    constructor(vararg pairs: Pair<TypedKey<*>, Any?>) : this(null, pairs.asIterable())

    fun cloneWithNewParent(newParent: ChainedPropertyBag?): ChainedPropertyBag = when {
        newParent == null -> this
        parent == null -> ChainedPropertyBag(newParent, data)
        else -> ChainedPropertyBag(parent.cloneWithNewParent(newParent), data)
    }

    inline operator fun <reified T> get(key: TypedKey<T>): T = getRaw(key) as T

    fun <T> getRaw(key: TypedKey<T>): Any? =
        when {
            data.containsKey(key) -> data[key]
            parent != null -> parent.getRaw(key)
            key.defaultValue != null -> key.defaultValue
            else -> throw IllegalArgumentException("Unknown key $key")
        }

    inline fun <reified T> getOrNull(key: TypedKey<T>): T? = getOrNullRaw(key)?.let { it as T }

    fun <T> getOrNullRaw(key: TypedKey<T>): Any? = data[key] ?: parent?.getOrNullRaw(key) ?: key.defaultValue
}
