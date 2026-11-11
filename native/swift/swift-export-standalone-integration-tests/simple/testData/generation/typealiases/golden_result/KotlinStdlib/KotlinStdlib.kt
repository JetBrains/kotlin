@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_ExportedKotlinPackages_kotlin_collections_Iterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.ListIterator::class, "_ExportedKotlinPackages_kotlin_collections_ListIterator")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("kotlin_collections_Iterator_hasNext__reverse_swift")
internal external fun kotlin_collections_Iterator_hasNext__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Iterator::class, "hasNext")
public fun kotlin_collections_Iterator_hasNext__reverse(self: kotlin.collections.Iterator<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Iterator_hasNext__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_Iterator_next__reverse_swift")
internal external fun kotlin_collections_Iterator_next__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Iterator::class, "next")
public fun kotlin_collections_Iterator_next__reverse(self: kotlin.collections.Iterator<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Iterator_next__reverse_swift(__self)
    return (dereferenceAndDisposeBridgeableRef(_result) as kotlin.Any?)
}

@ImportedBridge("kotlin_collections_ListIterator_hasNext__reverse_swift")
internal external fun kotlin_collections_ListIterator_hasNext__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "hasNext")
public fun kotlin_collections_ListIterator_hasNext__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_ListIterator_hasNext__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_ListIterator_hasPrevious__reverse_swift")
internal external fun kotlin_collections_ListIterator_hasPrevious__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "hasPrevious")
public fun kotlin_collections_ListIterator_hasPrevious__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_ListIterator_hasPrevious__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_ListIterator_nextIndex__reverse_swift")
internal external fun kotlin_collections_ListIterator_nextIndex__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "nextIndex")
public fun kotlin_collections_ListIterator_nextIndex__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_ListIterator_nextIndex__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_ListIterator_next__reverse_swift")
internal external fun kotlin_collections_ListIterator_next__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "next")
public fun kotlin_collections_ListIterator_next__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_ListIterator_next__reverse_swift(__self)
    return (dereferenceAndDisposeBridgeableRef(_result) as kotlin.Any?)
}

@ImportedBridge("kotlin_collections_ListIterator_previousIndex__reverse_swift")
internal external fun kotlin_collections_ListIterator_previousIndex__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "previousIndex")
public fun kotlin_collections_ListIterator_previousIndex__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_ListIterator_previousIndex__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_ListIterator_previous__reverse_swift")
internal external fun kotlin_collections_ListIterator_previous__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "previous")
public fun kotlin_collections_ListIterator_previous__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_ListIterator_previous__reverse_swift(__self)
    return (dereferenceAndDisposeBridgeableRef(_result) as kotlin.Any?)
}

@ExportedBridge("kotlin_collections_Iterator_hasNext")
public fun kotlin_collections_Iterator_hasNext(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterator<kotlin.Any?>
    val _result = __self.hasNext()
    return _result
}

@ExportedBridge("kotlin_collections_Iterator_next")
public fun kotlin_collections_Iterator_next(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterator<kotlin.Any?>
    val _result = __self.next()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_ListIterator_hasNext")
public fun kotlin_collections_ListIterator_hasNext(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.hasNext()
    return _result
}

@ExportedBridge("kotlin_collections_ListIterator_hasPrevious")
public fun kotlin_collections_ListIterator_hasPrevious(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.hasPrevious()
    return _result
}

@ExportedBridge("kotlin_collections_ListIterator_next")
public fun kotlin_collections_ListIterator_next(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.next()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_ListIterator_nextIndex")
public fun kotlin_collections_ListIterator_nextIndex(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.nextIndex()
    return _result
}

@ExportedBridge("kotlin_collections_ListIterator_previous")
public fun kotlin_collections_ListIterator_previous(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.previous()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_ListIterator_previousIndex")
public fun kotlin_collections_ListIterator_previousIndex(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.previousIndex()
    return _result
}
