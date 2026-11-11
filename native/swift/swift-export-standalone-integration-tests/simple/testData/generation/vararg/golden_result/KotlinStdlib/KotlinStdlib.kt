@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.BooleanArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE12BooleanArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.IntArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE8IntArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.BooleanIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE15BooleanIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.IntIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE11IntIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_ExportedKotlinPackages_kotlin_collections_Iterator")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("kotlin_collections_BooleanIterator_nextBoolean__reverse_swift")
internal external fun kotlin_collections_BooleanIterator_nextBoolean__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.BooleanIterator::class, "nextBoolean")
public fun kotlin_collections_BooleanIterator_nextBoolean__reverse(self: kotlin.collections.BooleanIterator): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_BooleanIterator_nextBoolean__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_IntIterator_nextInt__reverse_swift")
internal external fun kotlin_collections_IntIterator_nextInt__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.IntIterator::class, "nextInt")
public fun kotlin_collections_IntIterator_nextInt__reverse(self: kotlin.collections.IntIterator): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_IntIterator_nextInt__reverse_swift(__self)
    return _result
}

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

@ExportedBridge("kotlin_BooleanArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_BooleanArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.BooleanArray
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("kotlin_BooleanArray_iterator")
public fun kotlin_BooleanArray_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.BooleanArray
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_BooleanArray_set__TypesOfArguments__Swift_Int32_Swift_Bool__")
public fun kotlin_BooleanArray_set__TypesOfArguments__Swift_Int32_Swift_Bool__(self: kotlin.native.internal.NativePtr, index: Int, value: Boolean): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.BooleanArray
    val __index = index
    val __value = value
    __self.`set`(__index, __value)
    return true
}

@ExportedBridge("kotlin_BooleanArray_size_get")
public fun kotlin_BooleanArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.BooleanArray
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_IntArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_IntArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("kotlin_IntArray_iterator")
public fun kotlin_IntArray_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int, value: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val __index = index
    val __value = value
    __self.`set`(__index, __value)
    return true
}

@ExportedBridge("kotlin_IntArray_size_get")
public fun kotlin_IntArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_collections_BooleanIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__", nonVirtualTargetMethod = "<init>")
public fun kotlin_collections_BooleanIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, kotlin.collections.BooleanIterator())
    return true
}

@ExportedBridge("kotlin_collections_BooleanIterator_next")
public fun kotlin_collections_BooleanIterator_next(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.BooleanIterator
    val _result = __self.next()
    return _result
}

@ExportedBridge("kotlin_collections_BooleanIterator_nextBoolean")
public fun kotlin_collections_BooleanIterator_nextBoolean(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.BooleanIterator
    val _result = __self.nextBoolean()
    return _result
}

@ExportedBridge("kotlin_collections_IntIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__", nonVirtualTargetMethod = "<init>")
public fun kotlin_collections_IntIterator_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, kotlin.collections.IntIterator())
    return true
}

@ExportedBridge("kotlin_collections_IntIterator_next")
public fun kotlin_collections_IntIterator_next(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IntIterator
    val _result = __self.next()
    return _result
}

@ExportedBridge("kotlin_collections_IntIterator_nextInt")
public fun kotlin_collections_IntIterator_nextInt(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IntIterator
    val _result = __self.nextInt()
    return _result
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
