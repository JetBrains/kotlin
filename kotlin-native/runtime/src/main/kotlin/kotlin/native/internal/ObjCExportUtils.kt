/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.native.internal.ExportForCppRuntime
import kotlin.native.internal.KonanSet
import kotlin.native.internal.ReportUnhandledException

/**
 * This interface denotes the object to be a wrapper for the Objective-C object,
 * so the latter should be used to observe object lifetime.
 */
@ExportTypeInfo("theObjCObjectWrapperTypeInfo")
internal interface ObjCObjectWrapper

internal class NSArrayAsKList : AbstractList<Any?>(), RandomAccess, ObjCObjectWrapper {

    override val size: Int get() = getSize()

    @GCUnsafeCall("Kotlin_NSArrayAsKList_getSize")
    private external fun getSize(): Int

    @GCUnsafeCall("Kotlin_NSArrayAsKList_get")
    external override fun get(index: Int): Any?
}

internal class NSMutableArrayAsKMutableList : AbstractMutableList<Any?>(), RandomAccess, ObjCObjectWrapper {

    override val size: Int get() = getSize()

    @GCUnsafeCall("Kotlin_NSArrayAsKList_getSize")
    private external fun getSize(): Int

    @GCUnsafeCall("Kotlin_NSArrayAsKList_get")
    external override fun get(index: Int): Any?

    @GCUnsafeCall("Kotlin_NSMutableArrayAsKMutableList_add")
    external override fun add(index: Int, element: Any?): Unit

    @GCUnsafeCall("Kotlin_NSMutableArrayAsKMutableList_removeAt")
    external override fun removeAt(index: Int): Any?

    @GCUnsafeCall("Kotlin_NSMutableArrayAsKMutableList_set")
    external override fun set(index: Int, element: Any?): Any?
}

internal class NSSetAsKSet : AbstractSet<Any?>(), KonanSet<Any?>, ObjCObjectWrapper {

    override val size: Int get() = getSize()

    @GCUnsafeCall("Kotlin_NSSetAsKSet_getSize")
    private external fun getSize(): Int

    @GCUnsafeCall("Kotlin_NSSetAsKSet_contains")
    external override fun contains(element: Any?): Boolean

    @GCUnsafeCall("Kotlin_NSSetAsKSet_getElement")
    external override fun getElement(element: Any?): Any?

    @GCUnsafeCall("Kotlin_NSSetAsKSet_iterator")
    external override fun iterator(): Iterator<Any?>
}

internal class NSDictionaryAsKMap : Map<Any?, Any?>, ObjCObjectWrapper {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Map<*, *>) return false
        if (this.size != other.size) return false

        return other.entries.all { this.containsEntry(it.key, it.value) }
    }

    override fun hashCode(): Int {
        var result = 0
        keyIterator().forEach { key ->
            result += key.hashCode() xor this.getOrThrowConcurrentModification(key).hashCode()
        }
        return result
    }

    override fun toString(): String = entries.joinToString(", ", "{", "}") { toString(it.key) + "=" + toString(it.value) }

    private fun toString(o: Any?): String = if (o === this) "(this Map)" else o.toString()

    override val size: Int get() = getSize()

    @GCUnsafeCall("Kotlin_NSDictionaryAsKMap_getSize")
    private external fun getSize(): Int

    override fun isEmpty(): Boolean = (size == 0)

    @GCUnsafeCall("Kotlin_NSDictionaryAsKMap_containsKey")
    override external fun containsKey(key: Any?): Boolean

    @GCUnsafeCall("Kotlin_NSDictionaryAsKMap_containsValue")
    override external fun containsValue(value: Any?): Boolean

    @GCUnsafeCall("Kotlin_NSDictionaryAsKMap_get")
    external override operator fun get(key: Any?): Any?

    @GCUnsafeCall("Kotlin_NSDictionaryAsKMap_getOrThrowConcurrentModification")
    private external fun getOrThrowConcurrentModification(key: Any?): Any?

    @GCUnsafeCall("Kotlin_NSDictionaryAsKMap_containsEntry")
    private external fun containsEntry(key: Any?, value: Any?): Boolean

    // Views
    override val keys: Set<Any?> get() = this.Keys()

    override val values: Collection<Any?> get() = this.Values()

    override val entries: Set<Map.Entry<Any?, Any?>> get() = this.Entries()

    @GCUnsafeCall("Kotlin_NSDictionaryAsKMap_keyIterator")
    private external fun keyIterator(): Iterator<Any?>

    private inner class Keys : AbstractSet<Any?>() {

        override val size: Int get() = this@NSDictionaryAsKMap.size

        override fun iterator(): Iterator<Any?> = this@NSDictionaryAsKMap.keyIterator()

        override fun contains(element: Any?): Boolean = this@NSDictionaryAsKMap.containsKey(element)
    }

    @GCUnsafeCall("Kotlin_NSDictionaryAsKMap_valueIterator")
    private external fun valueIterator(): Iterator<Any?>

    private inner class Values : AbstractCollection<Any?>() {
        // TODO: what about equals and hashCode?

        override val size: Int get() = this@NSDictionaryAsKMap.size

        override fun iterator(): Iterator<Any?> = this@NSDictionaryAsKMap.valueIterator()

        override fun contains(element: Any?): Boolean = this@NSDictionaryAsKMap.containsValue(element)
    }

    private inner class Entries : AbstractSet<Map.Entry<Any?, Any?>>() {

        override val size: Int get() = this@NSDictionaryAsKMap.size

        override fun iterator(): Iterator<Map.Entry<Any?, Any?>> = this@NSDictionaryAsKMap.EntryIterator()

        override fun contains(element: Map.Entry<Any?, Any?>): Boolean {
            return this@NSDictionaryAsKMap.containsEntry(element.key, element.value)
        }
    }

    private class Entry(override val key: Any?, override val value: Any?) : Map.Entry<Any?, Any?> {
        override fun equals(other: Any?): Boolean =
                other is Map.Entry<*, *> &&
                        other.key == key &&
                        other.value == value

        override fun hashCode(): Int = key.hashCode() xor value.hashCode()

        override fun toString(): String = "$key=$value"
    }

    private inner class EntryIterator : Iterator<Map.Entry<Any?, Any?>> {
        val keyIterator = this@NSDictionaryAsKMap.keyIterator()

        override fun hasNext(): Boolean = keyIterator.hasNext()

        override fun next(): Map.Entry<Any?, Any?> {
            val nextKey = keyIterator.next()
            val nextValue = this@NSDictionaryAsKMap.getOrThrowConcurrentModification(nextKey)

            return Entry(nextKey, nextValue)
        }
    }

}

internal class NSEnumeratorAsKIterator : AbstractIterator<Any?>() {

    @GCUnsafeCall("Kotlin_NSEnumeratorAsKIterator_computeNext")
    override external fun computeNext()

    @ExportForCppRuntime
    private fun Kotlin_NSEnumeratorAsKIterator_done() = this.done()

    @ExportForCppRuntime
    private fun Kotlin_NSEnumeratorAsKIterator_setNext(value: Any?) = this.setNext(value)
}

@ExportForCppRuntime private fun Kotlin_Collection_getSize(collection: Collection<*>): Int = collection.size

@ExportForCppRuntime private fun Kotlin_List_get(list: List<*>, index: Int): Any? = list.get(index)

@ExportForCppRuntime private fun Kotlin_MutableList_addObjectAtIndex(list: MutableList<Any?>, index: Int, obj: Any?) {
    list.add(index, obj)
}

@ExportForCppRuntime private fun Kotlin_MutableList_removeObjectAtIndex(list: MutableList<Any?>, index: Int) {
    list.removeAt(index)
}

@ExportForCppRuntime private fun Kotlin_MutableCollection_addObject(list: MutableCollection<Any?>, obj: Any?) {
    list.add(obj)
}

@ExportForCppRuntime private fun Kotlin_MutableList_removeLastObject(list: MutableList<Any?>) {
    list.removeAt(list.lastIndex)
}

@ExportForCppRuntime private fun Kotlin_MutableList_setObject(list: MutableList<Any?>, index: Int, obj: Any?) {
    list.set(index, obj)
}

@ExportForCppRuntime private fun Kotlin_MutableCollection_removeObject(
        collection: MutableCollection<Any?>, element: Any?
) {
    collection.remove(element)
}

@ExportForCppRuntime private fun Kotlin_Iterator_hasNext(iterator: Iterator<Any?>): Boolean = iterator.hasNext()
@ExportForCppRuntime private fun Kotlin_Iterator_next(iterator: Iterator<Any?>): Any? = iterator.next()

@ExportForCppRuntime private fun Kotlin_Set_contains(set: Set<Any?>, element: Any?): Boolean = set.contains(element)

@ExportForCppRuntime private fun Kotlin_Set_getElement(set: Set<Any?>, element: Any?): Any? =
        if (set is KonanSet<Any?>) {
            set.getElement(element)
        } else if (set.contains(element)) {
            set.first { it == element }
        } else {
            null
        }

@ExportForCppRuntime private fun Kotlin_Set_iterator(set: Set<Any?>): Iterator<Any?> = set.iterator()
@ExportForCppRuntime private fun Kotlin_MutableSet_createWithCapacity(capacity: Int): MutableSet<Any?> =
        HashSet<Any?>(capacity)

@ExportForCppRuntime private fun Kotlin_Map_getSize(map: Map<Any?, Any?>): Int = map.size
@ExportForCppRuntime private fun Kotlin_Map_containsKey(map: Map<Any?, Any?>, key: Any?): Boolean = map.containsKey(key)
@ExportForCppRuntime private fun Kotlin_Map_get(map: Map<Any?, Any?>, key: Any?): Any? = map.get(key)
@ExportForCppRuntime private fun Kotlin_Map_keyIterator(map: Map<Any?, Any?>): Iterator<Any?> = map.keys.iterator()

@ExportForCppRuntime private fun Kotlin_MutableMap_createWithCapacity(capacity: Int): MutableMap<Any?, Any?> =
        HashMap<Any?, Any?>(capacity)

@ExportForCppRuntime private fun Kotlin_MutableMap_set(map: MutableMap<Any?, Any?>, key: Any?, value: Any?) {
    map.set(key, value)
}
@ExportForCppRuntime private fun Kotlin_MutableMap_remove(map: MutableMap<Any?, Any?>, key: Any?) {
    map.remove(key)
}

@ExportForCppRuntime private fun Kotlin_ObjCExport_ThrowCollectionTooLarge() {
    throw Error("an Objective-C collection is too large")
}

@ExportForCppRuntime private fun Kotlin_ObjCExport_ThrowCollectionConcurrentModification() {
    throw Error("an Objective-C collection was modified while iterating")
}

@ExportForCppRuntime private fun Kotlin_NSArrayAsKList_create() = NSArrayAsKList()
@ExportForCppRuntime private fun Kotlin_NSMutableArrayAsKMutableList_create() = NSMutableArrayAsKMutableList()
@ExportForCppRuntime private fun Kotlin_NSEnumeratorAsKIterator_create() = NSEnumeratorAsKIterator()
@ExportForCppRuntime private fun Kotlin_NSSetAsKSet_create() = NSSetAsKSet()
@ExportForCppRuntime private fun Kotlin_NSDictionaryAsKMap_create() = NSDictionaryAsKMap()

@ExportForCppRuntime private fun Kotlin_ObjCExport_NSErrorAsExceptionImpl(
        message: String?,
        error: Any
) = ObjCErrorException(message, error)

public class ObjCErrorException(
        message: String?,
        internal val error: Any
) : Exception(message) {
    override fun toString(): String = "NSError-based exception: $message"
}

@PublishedApi
@GCUnsafeCall("Kotlin_ObjCExport_trapOnUndeclaredException")
@ExportForCppRuntime
internal external fun trapOnUndeclaredException(exception: Throwable)

@ExportForCppRuntime
private fun Kotlin_Throwable_getMessage(throwable: Throwable): String? = throwable.message

@ExportForCppRuntime
private fun Kotlin_ObjCExport_getWrappedError(throwable: Throwable): Any? =
        (throwable as? ObjCErrorException)?.error

@ExportTypeInfo("theOpaqueFunctionTypeInfo")
@PublishedApi
internal class OpaqueFunction : Function<Any?>
