/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

@JsName("Map")
internal external class Es6Map<K, V> {
    constructor()
    constructor(entries: dynamic /*Es6Iterable<[K, V]>*/)

    val size: Int

    fun clear()
    fun delete(key: K): Boolean
    fun get(key: K): V // | undefined
    fun has(key: K): Boolean
    fun set(key: K, value: V): Es6Map<K, V>

    fun keys(): Es6Iterator<K>
    fun values(): Es6Iterator<V>
    fun entries(): Es6Iterator<dynamic /*[K, V]*/>

    fun forEach(callbackFn: () -> Unit, thisArg: Any? = definedExternally)
    fun forEach(callbackFn: (value: V) -> Unit, thisArg: Any? = definedExternally)
    fun forEach(callbackFn: (value: V, key: K) -> Unit, thisArg: Any? = definedExternally)
    fun forEach(callbackFn: (value: V, key: K, map: Es6Map<K, V>) -> Unit, thisArg: Any? = definedExternally)
}