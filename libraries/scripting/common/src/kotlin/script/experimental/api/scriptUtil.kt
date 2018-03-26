/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KProperty

// copy placed into package org.jetbrains.kotlin.utils.addToStdlib as well

data class TypedKey<T>(val name: String)

class TypedKeyDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): TypedKey<T> = TypedKey(property.name)
}

fun <T> typedKey() = TypedKeyDelegate<T>()

class HeterogeneousMap(private val data: Map<TypedKey<*>, Any?> = hashMapOf()) {
    constructor(vararg pairs: Pair<TypedKey<*>, Any?>) : this(hashMapOf(*pairs))
    constructor(from: HeterogeneousMap, vararg pairs: Pair<TypedKey<*>, Any?>) : this(HashMap(from.data).apply { putAll(pairs) })
    constructor(from: HeterogeneousMap, pairs: Iterable<Pair<TypedKey<*>, Any?>>) : this(HashMap(from.data).apply { putAll(pairs) })

    operator fun <T> get(key: TypedKey<T>): T =
        if (data.containsKey(key)) data[key] as T
        else throw IllegalArgumentException("Unknown key $key")

    fun <T> getOrNull(key: TypedKey<T>): T? = data[key] as T?
}

fun HeterogeneousMap.cloneWith(vararg pairs: Pair<TypedKey<*>, Any?>) = HeterogeneousMap(this, *pairs)

fun HeterogeneousMap.cloneWith(pairs: Iterable<Pair<TypedKey<*>, Any?>>) = HeterogeneousMap(this, pairs)

open class HeterogeneousMapBuilder {
    val pairs: MutableList<Pair<TypedKey<*>, Any?>> = arrayListOf()

    open operator fun <T> TypedKey<T>.invoke(v: T) {
        pairs.add(this to v)
    }

    fun add(pair: Pair<TypedKey<*>, Any?>) {
        pairs.add(pair)
    }
}

inline fun <T : HeterogeneousMapBuilder> T.build(from: HeterogeneousMap = HeterogeneousMap(), body: T.() -> Unit): HeterogeneousMap {
    body()
    return HeterogeneousMap(from, pairs)
}
