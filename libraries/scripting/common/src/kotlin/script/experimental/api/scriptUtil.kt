/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KProperty
import kotlin.reflect.full.primaryConstructor

data class TypedKey<T>(val name: String)

class TypedKeyDelegate<T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): TypedKey<T> = TypedKey(property.name)
}

fun <T> typedKey() = TypedKeyDelegate<T>()

class ChainedPropertyBag(private val parent: ChainedPropertyBag? = null, private val data: Map<TypedKey<*>, Any?> = hashMapOf()) {
    constructor(data: Map<TypedKey<*>, Any?>) : this(null, data)
    constructor(parent: ChainedPropertyBag?, pairs: Iterable<Pair<TypedKey<*>, Any?>>) : this(
        parent,
        HashMap<TypedKey<*>, Any?>().also {
            it.putAll(pairs)
        })

    constructor(parent: ChainedPropertyBag?, vararg pairs: Pair<TypedKey<*>, Any?>) : this(parent, hashMapOf(*pairs))

    operator fun <T> get(key: TypedKey<T>): T =
        when {
            data.containsKey(key) -> data[key] as T
            parent != null -> parent[key]
            else -> throw IllegalArgumentException("Unknown key $key")
        }

    fun <T> getOrNull(key: TypedKey<T>): T? = data[key] as T? ?: parent?.getOrNull(key)
}

open class PropertyBagBuilder(private val parentBuilder: PropertyBagBuilder? = null) {
    val pairs: MutableList<Pair<TypedKey<*>, Any?>> = arrayListOf()

    open operator fun <T> TypedKey<T>.invoke(v: T) {
        pairs.add(this to v)
    }

    fun add(pair: Pair<TypedKey<*>, Any?>) {
        pairs.add(pair)
    }

    fun getAllPairs() = if (parentBuilder == null) pairs else parentBuilder.pairs + pairs
}

inline fun <T : PropertyBagBuilder> T.build(parent: ChainedPropertyBag? = null, body: T.() -> Unit): ChainedPropertyBag {
    body()
    return ChainedPropertyBag(parent, getAllPairs())
}
