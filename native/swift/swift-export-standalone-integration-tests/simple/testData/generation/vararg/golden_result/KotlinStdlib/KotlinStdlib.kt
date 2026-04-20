@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.BooleanArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE12BooleanArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.IntArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE8IntArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Number::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE6NumberC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.BooleanIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE15BooleanIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.IntIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE11IntIteratorC")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("kotlin_Number_toByte__reverse_swift")
internal external fun kotlin_Number_toByte__reverse_swift(self: kotlin.native.internal.NativePtr): Byte

@BindReverseBridgeToMethod(kotlin.Number::class, "toByte")
public fun kotlin_Number_toByte__reverse(self: kotlin.Number): Byte {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_Number_toByte__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_Number_toDouble__reverse_swift")
internal external fun kotlin_Number_toDouble__reverse_swift(self: kotlin.native.internal.NativePtr): Double

@BindReverseBridgeToMethod(kotlin.Number::class, "toDouble")
public fun kotlin_Number_toDouble__reverse(self: kotlin.Number): Double {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_Number_toDouble__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_Number_toFloat__reverse_swift")
internal external fun kotlin_Number_toFloat__reverse_swift(self: kotlin.native.internal.NativePtr): Float

@BindReverseBridgeToMethod(kotlin.Number::class, "toFloat")
public fun kotlin_Number_toFloat__reverse(self: kotlin.Number): Float {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_Number_toFloat__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_Number_toInt__reverse_swift")
internal external fun kotlin_Number_toInt__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.Number::class, "toInt")
public fun kotlin_Number_toInt__reverse(self: kotlin.Number): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_Number_toInt__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_Number_toLong__reverse_swift")
internal external fun kotlin_Number_toLong__reverse_swift(self: kotlin.native.internal.NativePtr): Long

@BindReverseBridgeToMethod(kotlin.Number::class, "toLong")
public fun kotlin_Number_toLong__reverse(self: kotlin.Number): Long {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_Number_toLong__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_Number_toShort__reverse_swift")
internal external fun kotlin_Number_toShort__reverse_swift(self: kotlin.native.internal.NativePtr): Short

@BindReverseBridgeToMethod(kotlin.Number::class, "toShort")
public fun kotlin_Number_toShort__reverse(self: kotlin.Number): Short {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_Number_toShort__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_collections_BooleanIterator_nextBoolean__reverse_swift")
internal external fun kotlin_collections_BooleanIterator_nextBoolean__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.BooleanIterator::class, "nextBoolean")
public fun kotlin_collections_BooleanIterator_nextBoolean__reverse(self: kotlin.collections.BooleanIterator): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_BooleanIterator_nextBoolean__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_collections_IntIterator_nextInt__reverse_swift")
internal external fun kotlin_collections_IntIterator_nextInt__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.IntIterator::class, "nextInt")
public fun kotlin_collections_IntIterator_nextInt__reverse(self: kotlin.collections.IntIterator): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_IntIterator_nextInt__reverse_swift(__self)
    return __result
}

@ExportedBridge("kotlin_BooleanArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_BooleanArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.BooleanArray
    val __index = index
    val _result = run { __self.`get`(__index) }
    return _result
}

@ExportedBridge("kotlin_BooleanArray_iterator")
public fun kotlin_BooleanArray_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.BooleanArray
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_BooleanArray_set__TypesOfArguments__Swift_Int32_Swift_Bool__")
public fun kotlin_BooleanArray_set__TypesOfArguments__Swift_Int32_Swift_Bool__(self: kotlin.native.internal.NativePtr, index: Int, value: Boolean): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.BooleanArray
    val __index = index
    val __value = value
    val _result = run { __self.`set`(__index, __value) }
    return run { _result; true }
}

@ExportedBridge("kotlin_BooleanArray_size_get")
public fun kotlin_BooleanArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.BooleanArray
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("kotlin_IntArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_IntArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val __index = index
    val _result = run { __self.`get`(__index) }
    return _result
}

@ExportedBridge("kotlin_IntArray_iterator")
public fun kotlin_IntArray_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int, value: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val __index = index
    val __value = value
    val _result = run { __self.`set`(__index, __value) }
    return run { _result; true }
}

@ExportedBridge("kotlin_IntArray_size_get")
public fun kotlin_IntArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("kotlin_Number_toByte")
public fun kotlin_Number_toByte(self: kotlin.native.internal.NativePtr): Byte {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = run { __self.toByte() }
    return _result
}

@ExportedBridge("kotlin_Number_toChar")
public fun kotlin_Number_toChar(self: kotlin.native.internal.NativePtr): Char {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = run { __self.toChar() }
    return _result
}

@ExportedBridge("kotlin_Number_toDouble")
public fun kotlin_Number_toDouble(self: kotlin.native.internal.NativePtr): Double {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = run { __self.toDouble() }
    return _result
}

@ExportedBridge("kotlin_Number_toFloat")
public fun kotlin_Number_toFloat(self: kotlin.native.internal.NativePtr): Float {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = run { __self.toFloat() }
    return _result
}

@ExportedBridge("kotlin_Number_toInt")
public fun kotlin_Number_toInt(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = run { __self.toInt() }
    return _result
}

@ExportedBridge("kotlin_Number_toLong")
public fun kotlin_Number_toLong(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = run { __self.toLong() }
    return _result
}

@ExportedBridge("kotlin_Number_toShort")
public fun kotlin_Number_toShort(self: kotlin.native.internal.NativePtr): Short {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Number
    val _result = run { __self.toShort() }
    return _result
}

@ExportedBridge("kotlin_collections_BooleanIterator_next")
public fun kotlin_collections_BooleanIterator_next(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.BooleanIterator
    val _result = run { __self.next() }
    return _result
}

@ExportedBridge("kotlin_collections_BooleanIterator_nextBoolean")
public fun kotlin_collections_BooleanIterator_nextBoolean(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.BooleanIterator
    val _result = run { __self.nextBoolean() }
    return _result
}

@ExportedBridge("kotlin_collections_IntIterator_next")
public fun kotlin_collections_IntIterator_next(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IntIterator
    val _result = run { __self.next() }
    return _result
}

@ExportedBridge("kotlin_collections_IntIterator_nextInt")
public fun kotlin_collections_IntIterator_nextInt(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IntIterator
    val _result = run { __self.nextInt() }
    return _result
}
