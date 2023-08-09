/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections


@JsName("Array")
internal external abstract class JsMutableArray<E> : JsImmutableArray<E>

@JsName("ReadonlyArray")
internal external interface JsImmutableArray<out E>

private class JsArrayView<E> : JsMutableArray<E>()

private fun UNSUPPORTED_OPERATION() { throw UnsupportedOperationException() }

internal fun <E> createJsArrayImmutableViewFrom(list: List<E>): JsImmutableArray<E> =
    createJsArrayMutableViewWith(
        listSize = { list.size },
        listGet = { i -> list[i] },
        listSet = ::UNSUPPORTED_OPERATION.asDynamic(),
        listAdd = ::UNSUPPORTED_OPERATION.asDynamic(),
        listDecreaseSize = ::UNSUPPORTED_OPERATION.asDynamic()
    )

internal fun <E> createJsArrayMutableViewFrom(list: MutableList<E>): JsMutableArray<E> =
    createJsArrayMutableViewWith(
        listSize = { list.size },
        listGet = { i -> list[i] },
        listSet = { i, v -> list[i] = v },
        listAdd = { v -> list.add(v) },
        listDecreaseSize = { size -> list.subList(list.size - size, list.size).clear() }
    )

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
private fun <E> createJsArrayMutableViewWith(
    listSize: () -> Int,
    listGet: (Int) -> E,
    listSet: (Int, E) -> Unit,
    listAdd: (E) -> Unit,
    listDecreaseSize: (Int) -> Unit,
): dynamic {
    val arrayView = objectCreate<JsArrayView<*>>()

    return js("""
       new Proxy(arrayView, {
           get: function(target, prop, receiver) {
               if (prop === "length") return listSize();
               if (typeof prop !== "symbol" && !isNaN(prop)) return listGet(prop);
               return target[prop]
           },
           has: function(target, key) { return !isNaN(key) && key < listSize() },
           set: function(obj, prop, value) {
                if (prop === "length") {
                    var size = listSize();
                    if (value < size) listDecreaseSize(size - value)
                } else if (!isNaN(value)) {
                    var size = listSize();
                    if (prop >= size) listAdd(value)
                    else listSet(prop, value)
                }
                return true
           },
       }) 
    """)
}


@JsName("ReadonlySet")
internal external interface JsImmutableSet<out E>

@JsName("Set")
internal external abstract class JsMutableSet<E> : JsImmutableArray<E>

private class JsSetView<E> : JsMutableSet<E>()

internal fun <E> createJsSetImmutableViewFrom(set: Set<E>): JsImmutableSet<E> =
    createJsSetImmutableViewWith<E>(
        setSize = { set.size },
        setAdd = ::UNSUPPORTED_OPERATION.asDynamic(),
        setRemove = ::UNSUPPORTED_OPERATION.asDynamic(),
        setClear = ::UNSUPPORTED_OPERATION.asDynamic(),
        setContains = { v -> set.contains(v) },
        valuesIterator = { createJsIteratorFrom(set.iterator()) },
        entriesIterator = { createJsIteratorFrom(set.iterator()) { arrayOf(it, it) } },
        forEach = { cb, t -> forEach(cb, t ?: set) }
    )

internal fun <E> createJsSetMutableViewFrom(set: MutableSet<E>): JsMutableSet<E> =
    createJsSetImmutableViewWith<E>(
        setSize = { set.size },
        setAdd = { v -> set.add(v) },
        setRemove = { v -> set.remove(v) },
        setClear = { set.clear() },
        setContains = { v -> set.contains(v) },
        valuesIterator = { createJsIteratorFrom(set.iterator()) },
        entriesIterator = { createJsIteratorFrom(set.iterator()) { arrayOf(it, it) } },
        forEach = { cb, t -> forEach(cb, t) }
    )

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
private fun <E> createJsSetImmutableViewWith(
    setSize: () -> Int,
    setAdd: (E) -> Unit,
    setRemove: (E) -> Boolean,
    setClear: () -> Unit,
    setContains: (E) -> Boolean,
    valuesIterator: () -> dynamic,
    entriesIterator: () -> dynamic,
    forEach: (dynamic, dynamic) -> Unit
): dynamic {
    val setView = objectCreate<JsSetView<E>>().also {
        js("it[Symbol.iterator] = valuesIterator")
        defineProp(it, "size", setSize, VOID)
    }
    
    return js("""
       Object.assign(setView, {
            add: function(value) { setAdd(value); return this },
            'delete': setRemove,
            clear: setClear,
            has: setContains,
            keys: valuesIterator,
            values: valuesIterator,
            entries: entriesIterator,
            forEach: function (cb, thisArg) { forEach(cb, thisArg || setView) }
       })
    """)
}


@JsName("ReadonlyMap")
internal external interface JsImmutableMap<K, out V>

@JsName("Map")
internal external abstract class JsMutableMap<K, V> : JsImmutableMap<K, V>

private class JsMapView<K, V> : JsMutableMap<K, V>()

internal fun <K, V> createJsMapImmutableViewFrom(map: Map<K, V>): JsImmutableMap<K, V> =
    createJsMapImmutableViewWith<K, V>(
        mapSize = { map.size },
        mapGet = { k -> map[k] },
        mapContains = { k -> map.containsKey(k) },
        mapPut = ::UNSUPPORTED_OPERATION.asDynamic(),
        mapRemove = ::UNSUPPORTED_OPERATION.asDynamic(),
        mapClear = ::UNSUPPORTED_OPERATION.asDynamic(),
        keysIterator = { createJsIteratorFrom(map.keys.iterator()) },
        valuesIterator = { createJsIteratorFrom(map.values.iterator()) },
        entriesIterator = { createJsIteratorFrom(map.entries.iterator()) { arrayOf(it.key, it.value) } },
        forEach = { cb, t -> forEach(cb, t ?: map) }
    )

internal fun <K, V> createJsMapMutableViewFrom(map: MutableMap<K, V>): JsMutableMap<K, V> =
    createJsMapImmutableViewWith<K, V>(
        mapSize = { map.size },
        mapGet = { k -> map[k] },
        mapContains = { k -> map.containsKey(k) },
        mapPut = { k, v -> map.put(k, v) },
        mapRemove = { k -> map.remove(k) },
        mapClear = { map.clear() },
        keysIterator = { createJsIteratorFrom(map.keys.iterator()) },
        valuesIterator = { createJsIteratorFrom(map.values.iterator()) },
        entriesIterator = { createJsIteratorFrom(map.entries.iterator()) { arrayOf(it.key, it.value) } },
        forEach = { cb, t -> forEach(cb, t) }
    )

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
private fun <K, V> createJsMapImmutableViewWith(
   mapSize: () -> Int,
   mapGet: (K) -> V?,
   mapContains: (K) -> Boolean,
   mapPut: (K, V) -> Unit,
   mapRemove: (K) -> Unit,
   mapClear: () -> Unit,
   keysIterator: () -> dynamic,
   valuesIterator: () -> dynamic,
   entriesIterator: () -> dynamic,
   forEach: (dynamic, dynamic) -> Unit
): dynamic {
    val mapView = objectCreate<JsMapView<K, V>>().also {
        js("it[Symbol.iterator] = entriesIterator")
        defineProp(it, "size", mapSize, VOID)
    }
    
    return js("""
       Object.assign(mapView, {
            get: mapGet,
            set: function(key, value) { mapPut(key, value); return this },
            'delete': mapRemove,
            clear: mapClear,
            has: mapContains,
            keys: valuesIterator,
            values: valuesIterator,
            entries: entriesIterator,
            forEach: function (cb, thisArg) { forEach(cb, thisArg || mapView) }
       })
    """)
}

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
private fun <T> createJsIteratorFrom(iterator: Iterator<T>, transform: (T) -> dynamic = { it }): dynamic {
    val iteratorNext = { iterator.next() }
    val iteratorHasNext = { iterator.hasNext() }
    return js("""{
        next: function() {
            var result = { done: !iteratorHasNext() };
            if (!result.done) result.value = transform(iteratorNext());
            return result;
        }
    }""")
}

private fun forEach(cb: (dynamic, dynamic, dynamic) -> Unit, thisArg: dynamic) {
    val iterator = thisArg.entries()
    var result = iterator.next()
    
    while (!result.done) {
        val value = result.value
        cb(value[0], value[1], thisArg)
        result = iterator.next()
    }
}