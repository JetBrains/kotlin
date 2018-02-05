/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KProperty

data class TypedKey<T>(val name: String)

class TypedKeyDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): TypedKey<T> = TypedKey(property.name)
}

fun <T> typedKey() = TypedKeyDelegate<T>()

class HeterogeneousMap(val data: Map<TypedKey<*>, Any?> = hashMapOf()) {
    constructor(vararg pairs: Pair<TypedKey<*>, Any?>) : this(hashMapOf(*pairs))
}

fun HeterogeneousMap.cloneWith(vararg pairs: Pair<TypedKey<*>, Any?>) = HeterogeneousMap(HashMap(data).apply { putAll(pairs) })

operator fun <T> HeterogeneousMap.get(key: TypedKey<T>): T =
    if (data.containsKey(key)) data[key] as T
    else throw IllegalArgumentException("Unknown key $key")

fun <T> HeterogeneousMap.getOrNull(key: TypedKey<T>): T? = data[key] as T?

