/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(ExperimentalJsCollectionsApi::class)

package kotlin.collections

import kotlin.js.collections.*

private class JsArrayView<E> : JsArray<E>()

private fun UNSUPPORTED_OPERATION() {
    throw UnsupportedOperationException()
}

@PublishedApi
internal fun <E> createListFrom(array: JsReadonlyArray<E>): List<E> =
    ArrayList<E>(array.asDynamic().slice().unsafeCast<Array<Any?>>()).build()

@PublishedApi
internal fun <E> createMutableListFrom(array: JsReadonlyArray<E>): MutableList<E> =
    ArrayList<E>(array.asDynamic().slice().unsafeCast<Array<Any?>>())

@PublishedApi
internal fun <E> createSetFrom(set: JsReadonlySet<E>): Set<E> =
    buildSetInternal { forEach({ _, value, _ -> add(value) }, set) }

@PublishedApi
internal fun <E> createMutableSetFrom(set: JsReadonlySet<E>): MutableSet<E> =
    LinkedHashSet<E>().apply { forEach({ _, value, _ -> add(value) }, set) }

@PublishedApi
internal fun <K, V> createMapFrom(map: JsReadonlyMap<K, V>): Map<K, V> =
    buildMapInternal { forEach({ value, key, _ -> put(key, value) }, map) }

@PublishedApi
internal fun <K, V> createMutableMapFrom(map: JsReadonlyMap<K, V>): MutableMap<K, V> =
    LinkedHashMap<K, V>().apply { forEach({ value, key, _ -> put(key, value) }, map) }

internal fun <E> createJsReadonlyArrayViewFrom(list: List<E>): JsReadonlyArray<E> =
    createJsArrayViewWith(
        listSize = { list.size },
        listGet = { i -> list[i] },
        listSet = ::UNSUPPORTED_OPERATION.asDynamic(),
        listDecreaseSize = ::UNSUPPORTED_OPERATION.asDynamic(),
        listIncreaseSize = ::UNSUPPORTED_OPERATION.asDynamic()
    )

internal fun <E> createJsArrayViewFrom(list: MutableList<E>): JsArray<E> =
    createJsArrayViewWith(
        listSize = { list.size },
        listGet = { i -> list[i] },
        listSet = { i, v -> list[i] = v },
        listDecreaseSize = { size -> list.subList(list.size - size, list.size).clear() },
        listIncreaseSize = ::UNSUPPORTED_OPERATION.asDynamic()
    )

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
private fun <E> createJsArrayViewWith(
    listSize: () -> Int,
    listGet: (Int) -> E,
    listSet: (Int, E) -> Unit,
    listDecreaseSize: (Int) -> Unit,
    listIncreaseSize: (Int) -> Unit,
): dynamic {
    val arrayView = JsArray<E>()
    val _ = JsObject.setPrototypeOf(arrayView, JsArrayView::class.js.asDynamic().prototype)

    return js("""
       new Proxy(arrayView, {
           get: function(target, prop, receiver) {
               if (prop === "length") return listSize();
               var type = typeof prop
               var index = type === "string" || type === "number" ? +prop : undefined
               if (!isNaN(index)) return listGet(index);
               return target[prop]
           },
           has: function(target, key) { return !isNaN(key) && key < listSize() },
           set: function(obj, prop, value) {
                if (prop === "length") {
                    var size = listSize();
                    var newSize = type === "string" || type === "number" ? +prop : undefined
                    if (isNaN(newSize)) throw new RangeError("invalid array length")
                    if (newSize < size) listDecreaseSize(size - newSize)
                    else listIncreaseSize(newSize - size)
                    return true
                } 
                
                var type = typeof prop;
                var index = type === "string" || type === "number" ? +prop : undefined;
                
                if (isNaN(index)) return false;
                
                listSet(index, value)
                
                return true
           },
       }) 
    """)
}

private class JsSetView<E> : JsSet<E>()

internal fun <E> createJsReadonlySetViewFrom(set: Set<E>): JsReadonlySet<E> =
    createJsSetViewWith<E>(
        setSize = { set.size },
        setAdd = ::UNSUPPORTED_OPERATION.asDynamic(),
        setRemove = ::UNSUPPORTED_OPERATION.asDynamic(),
        setClear = ::UNSUPPORTED_OPERATION.asDynamic(),
        setContains = { v -> set.contains(v) },
        valuesIterator = { createJsIteratorFrom(set.iterator()) },
        entriesIterator = { createJsIteratorFrom(set.iterator()) { arrayOf(it, it) } },
        forEach = { callback, set, thisArg -> forEach(callback, set, thisArg) }
    )

internal fun <E> createJsSetViewFrom(set: MutableSet<E>): JsSet<E> =
    createJsSetViewWith<E>(
        setSize = { set.size },
        setAdd = { v -> set.add(v) },
        setRemove = { v -> set.remove(v) },
        setClear = { set.clear() },
        setContains = { v -> set.contains(v) },
        valuesIterator = { createJsIteratorFrom(set.iterator()) },
        entriesIterator = { createJsIteratorFrom(set.iterator()) { arrayOf(it, it) } },
        forEach = { callback, set, thisArg -> forEach(callback, set, thisArg) }
    )

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
private fun <E> createJsSetViewWith(
    setSize: () -> Int,
    setAdd: (E) -> Unit,
    setRemove: (E) -> Boolean,
    setClear: () -> Unit,
    setContains: (E) -> Boolean,
    valuesIterator: () -> dynamic,
    entriesIterator: () -> dynamic,
    forEach: (callback: dynamic, set: dynamic, thisArg: dynamic) -> Unit,
): dynamic {
    val setView = objectCreate<JsSetView<E>>().also {
        js("it[Symbol.iterator] = valuesIterator")
        defineProp(it, "size", setSize, VOID, true)
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
            forEach: function (cb, thisArg) { forEach(cb, setView, thisArg) }
       })
    """)
}


private class JsMapView<K, V> : JsMap<K, V>()

internal fun <K, V> createJsReadonlyMapViewFrom(map: Map<K, V>): JsReadonlyMap<K, V> =
    createJsMapViewWith<K, V>(
        mapSize = { map.size },
        mapGet = { k -> map[k] },
        mapContains = { k -> map.containsKey(k) },
        mapPut = ::UNSUPPORTED_OPERATION.asDynamic(),
        mapRemove = ::UNSUPPORTED_OPERATION.asDynamic(),
        mapClear = ::UNSUPPORTED_OPERATION.asDynamic(),
        keysIterator = { createJsIteratorFrom(map.keys.iterator()) },
        valuesIterator = { createJsIteratorFrom(map.values.iterator()) },
        entriesIterator = { createJsIteratorFrom(map.entries.iterator()) { arrayOf(it.key, it.value) } },
        forEach = { callback, map, thisArg -> forEach(callback, map, thisArg) }
    )

internal fun <K, V> createJsMapViewFrom(map: MutableMap<K, V>): JsMap<K, V> =
    createJsMapViewWith<K, V>(
        mapSize = { map.size },
        mapGet = { k -> map[k] },
        mapContains = { k -> map.containsKey(k) },
        mapPut = { k, v -> map.put(k, v) },
        mapRemove = { k -> map.remove(k) },
        mapClear = { map.clear() },
        keysIterator = { createJsIteratorFrom(map.keys.iterator()) },
        valuesIterator = { createJsIteratorFrom(map.values.iterator()) },
        entriesIterator = { createJsIteratorFrom(map.entries.iterator()) { arrayOf(it.key, it.value) } },
        forEach = { callback, map, thisArg -> forEach(callback, map, thisArg) }
    )

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
private fun <K, V> createJsMapViewWith(
    mapSize: () -> Int,
    mapGet: (K) -> V?,
    mapContains: (K) -> Boolean,
    mapPut: (K, V) -> Unit,
    mapRemove: (K) -> Unit,
    mapClear: () -> Unit,
    keysIterator: () -> dynamic,
    valuesIterator: () -> dynamic,
    entriesIterator: () -> dynamic,
    forEach: (callback: dynamic, map: dynamic, thisArg: dynamic) -> Unit,
): dynamic {
    val mapView = objectCreate<JsMapView<K, V>>().also {
        js("it[Symbol.iterator] = entriesIterator")
        defineProp(it, "size", mapSize, VOID, true)
    }

    return js("""
       Object.assign(mapView, {
            get: mapGet,
            set: function(key, value) { mapPut(key, value); return this },
            'delete': mapRemove,
            clear: mapClear,
            has: mapContains,
            keys: keysIterator,
            values: valuesIterator,
            entries: entriesIterator,
            forEach: function (cb, thisArg) { forEach(cb, mapView, thisArg) }
       })
    """)
}

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
private fun <T> createJsIteratorFrom(iterator: Iterator<T>, transform: (T) -> dynamic = { it }): dynamic {
    val iteratorNext = { iterator.next() }
    val iteratorHasNext = { iterator.hasNext() }
    val jsIterator = js(
        """
        {
            next: function() {
                var result = { done: !iteratorHasNext() };
                if (!result.done) result.value = transform(iteratorNext());
                return result;
            }
        }
        """
    )
    js("jsIterator[Symbol.iterator] = function() { return this; }")
    return jsIterator
}

private fun forEach(cb: (dynamic, dynamic, dynamic) -> Unit, collection: dynamic, thisArg: dynamic = js("undefined")) {
    val iterator = collection.entries()
    var result = iterator.next()

    while (!result.done) {
        val value = result.value
        cb.asDynamic().call(thisArg, value[1], value[0], collection)
        result = iterator.next()
    }
}
