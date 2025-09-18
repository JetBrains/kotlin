@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.IntArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE8IntArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.IntIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE11IntIteratorC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

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
public fun kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int, value: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val __index = index
    val __value = value
    __self.`set`(__index, __value)
}

@ExportedBridge("kotlin_IntArray_size_get")
public fun kotlin_IntArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val _result = __self.size
    return _result
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
