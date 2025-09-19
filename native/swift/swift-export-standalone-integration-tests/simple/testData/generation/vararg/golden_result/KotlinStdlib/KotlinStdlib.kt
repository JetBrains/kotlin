@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.BooleanArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE12BooleanArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.IntArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE8IntArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Number::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE6NumberC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.BooleanIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE15BooleanIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.IntIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE11IntIteratorC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

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
public fun kotlin_BooleanArray_set__TypesOfArguments__Swift_Int32_Swift_Bool__(self: kotlin.native.internal.NativePtr, index: Int, value: Boolean): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.BooleanArray
    val __index = index
    val __value = value
    __self.`set`(__index, __value)
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

@ExportedBridge("kotlin_Number_toByte")
public fun kotlin_Number_toByte(self: kotlin.native.internal.NativePtr): Byte {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = __self.toByte()
    return _result
}

@ExportedBridge("kotlin_Number_toChar")
public fun kotlin_Number_toChar(self: kotlin.native.internal.NativePtr): Char {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = __self.toChar()
    return _result
}

@ExportedBridge("kotlin_Number_toDouble")
public fun kotlin_Number_toDouble(self: kotlin.native.internal.NativePtr): Double {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = __self.toDouble()
    return _result
}

@ExportedBridge("kotlin_Number_toFloat")
public fun kotlin_Number_toFloat(self: kotlin.native.internal.NativePtr): Float {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = __self.toFloat()
    return _result
}

@ExportedBridge("kotlin_Number_toInt")
public fun kotlin_Number_toInt(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = __self.toInt()
    return _result
}

@ExportedBridge("kotlin_Number_toLong")
public fun kotlin_Number_toLong(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = __self.toLong()
    return _result
}

@ExportedBridge("kotlin_Number_toShort")
public fun kotlin_Number_toShort(self: kotlin.native.internal.NativePtr): Short {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = __self.toShort()
    return _result
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
