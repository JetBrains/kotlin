@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Array::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE5ArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.ByteArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE9ByteArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.ByteIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE12ByteIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_Iterator")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlin_Array_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_Array_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val __index = index
    val _result = __self.`get`(__index)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Array_iterator")
public fun kotlin_Array_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val __index = index
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    __self.`set`(__index, __value)
}

@ExportedBridge("kotlin_Array_size_get")
public fun kotlin_Array_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Byte {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("kotlin_ByteArray_iterator")
public fun kotlin_ByteArray_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__")
public fun kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__(self: kotlin.native.internal.NativePtr, index: Int, value: Byte): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val __index = index
    val __value = value
    __self.`set`(__index, __value)
}

@ExportedBridge("kotlin_ByteArray_size_get")
public fun kotlin_ByteArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_collections_ByteIterator_next")
public fun kotlin_collections_ByteIterator_next(self: kotlin.native.internal.NativePtr): Byte {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ByteIterator
    val _result = __self.next()
    return _result
}

@ExportedBridge("kotlin_collections_ByteIterator_nextByte")
public fun kotlin_collections_ByteIterator_nextByte(self: kotlin.native.internal.NativePtr): Byte {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ByteIterator
    val _result = __self.nextByte()
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
