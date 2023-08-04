/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections


@JsName("Array")
private external abstract class JsMutableArray<E> : JsImmutableArray<E>

@JsName("ReadonlyArray")
internal external interface JsImmutableArray<out E>

private class JsArrayView<E> : JsMutableArray<E>()

@Suppress("UNUSED_VARIABLE")
internal fun <E> JsArrayView(list: List<E>): JsImmutableArray<E> {
    val arrayView = objectCreate<JsArrayView<*>>()
    val listSize = { list.size }
    val listGet = { index: Int -> list[index] }
    val UNSUPPORTED_OPERATION = { UnsupportedOperationException() }

    return js("""
       new Proxy(arrayView, {
           get: function(target, prop, receiver) {
               if (prop == "length") return listSize();
               if (typeof prop !== "symbol" && !isNaN(prop)) return listGet(prop);
               return target[prop]
           },
           has: function(target, key) { return !isNaN(key) && key < listSize() },
           set: function(obj, prop, value) { UNSUPPORTED_OPERATION() },
       }) 
    """)
}


@JsName("ReadonlySet")
internal external interface JsImmutableSet<out E>

@JsName("Set")
private external abstract class JsMutableSet<E> : JsImmutableArray<E>

private class JsSetView<E> : JsMutableSet<E>()

@Suppress("UNUSED_VARIABLE")
internal fun <E> JsSetView(set: Set<E>): JsImmutableSet<E> {
    val setSize = { set.size }
    val setContains = { value: E -> set.contains(value) }

    val capturedForEach = { cb: dynamic, self: dynamic -> forEach(cb, self) }
    val valuesIterator = { JsIterator(set.iterator()) }
    val entriesIterator = { JsIterator(set.iterator()) { arrayOf(it, it) } }
    val UNSUPPORTED_OPERATION = { UnsupportedOperationException() }

    val setView = objectCreate<JsSetView<E>>().also {
        js("it[Symbol.iterator] = valuesIterator")
        defineProp(it, "size", setSize, VOID)
    }
    
    return js("""
       Object.assign(setView, {
            add: function(value) { UNSUPPORTED_OPERATION() },
            'delete': function(value) { UNSUPPORTED_OPERATION()},
            clear: function() { UNSUPPORTED_OPERATION() },
            has: setContains,
            keys: valuesIterator,
            values: valuesIterator,
            entries: entriesIterator,
            forEach: function(cb, thisArg) { capturedForEach(cb, thisArg || setView) }
       })
    """)
}


@JsName("ReadonlyMap")
internal external interface JsImmutableMap<K, out V>

@JsName("Map")
private external abstract class JsMutableMap<K, V> : JsImmutableMap<K, V>

private class JsMapView<K, V> : JsMutableMap<K, V>()

@Suppress("UNUSED_VARIABLE")
internal fun <K, V> JsMapView(map: Map<K, V>): JsImmutableMap<K, V> {
    val mapSize = { map.size }
    val mapContains = { key: K -> map.containsKey(key) }

    val capturedForEach = { cb: dynamic, self: dynamic -> forEach(cb, self) }
    val keysIterator = { JsIterator(map.keys.iterator()) }
    val valuesIterator = { JsIterator(map.values.iterator()) }
    val entriesIterator = { JsIterator(map.entries.iterator(), { arrayOf(it.key, it.value) }) }
    val UNSUPPORTED_OPERATION = { UnsupportedOperationException() }
    
    val mapView = objectCreate<JsMapView<K, V>>().also {
        js("it[Symbol.iterator] = entriesIterator")
        defineProp(it, "size", mapSize, VOID)
    }
    
    return js("""
       Object.assign(mapView, {
            set: function(key, value) { UNSUPPORTED_OPERATION() },
            'delete': function(key) { UNSUPPORTED_OPERATION() },
            clear: function() { UNSUPPORTED_OPERATION() },
            has: mapContains,
            keys: valuesIterator,
            values: valuesIterator,
            entries: entriesIterator,
            forEach: function(cb, thisArg) { capturedForEach(cb, thisArg || mapView) }
       })
    """)
}

@Suppress("UNUSED_VARIABLE", "UNUSED_PARAMETER")
private fun <T> JsIterator(iterator: Iterator<T>, transform: (T) -> dynamic = { it }): dynamic {
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