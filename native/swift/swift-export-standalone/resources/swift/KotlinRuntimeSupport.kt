@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlin.native.internal.ExportedBridge

@ExportedBridge("__root____getExceptionMessage__TypesOfArguments__ExportedKotlinPackages_kotlin_Exception__")
public fun __root____getExceptionMessage__TypesOfArguments__ExportedKotlinPackages_kotlin_Exception__(exception: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __exception = kotlin.native.internal.ref.dereferenceExternalRCRef(exception) as kotlin.Throwable
    val _result = __exception.message
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}

// _KotlinBridgeable bridge functions for primitive types

@ExportedBridge("KotlinBridgeable_Int8_box")
public fun KotlinBridgeable_Int8_box(value: Byte): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_Int8_unbox")
public fun KotlinBridgeable_Int8_unbox(ref: kotlin.native.internal.NativePtr): Byte {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Byte
}

@ExportedBridge("KotlinBridgeable_Int16_box")
public fun KotlinBridgeable_Int16_box(value: Short): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_Int16_unbox")
public fun KotlinBridgeable_Int16_unbox(ref: kotlin.native.internal.NativePtr): Short {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Short
}

@ExportedBridge("KotlinBridgeable_Int32_box")
public fun KotlinBridgeable_Int32_box(value: Int): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_Int32_unbox")
public fun KotlinBridgeable_Int32_unbox(ref: kotlin.native.internal.NativePtr): Int {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Int
}

@ExportedBridge("KotlinBridgeable_Int64_box")
public fun KotlinBridgeable_Int64_box(value: Long): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_Int64_unbox")
public fun KotlinBridgeable_Int64_unbox(ref: kotlin.native.internal.NativePtr): Long {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Long
}

@ExportedBridge("KotlinBridgeable_UInt8_box")
public fun KotlinBridgeable_UInt8_box(value: UByte): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_UInt8_unbox")
public fun KotlinBridgeable_UInt8_unbox(ref: kotlin.native.internal.NativePtr): UByte {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as UByte
}

@ExportedBridge("KotlinBridgeable_UInt16_box")
public fun KotlinBridgeable_UInt16_box(value: UShort): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_UInt16_unbox")
public fun KotlinBridgeable_UInt16_unbox(ref: kotlin.native.internal.NativePtr): UShort {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as UShort
}

@ExportedBridge("KotlinBridgeable_UInt32_box")
public fun KotlinBridgeable_UInt32_box(value: UInt): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_UInt32_unbox")
public fun KotlinBridgeable_UInt32_unbox(ref: kotlin.native.internal.NativePtr): UInt {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as UInt
}

@ExportedBridge("KotlinBridgeable_UInt64_box")
public fun KotlinBridgeable_UInt64_box(value: ULong): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_UInt64_unbox")
public fun KotlinBridgeable_UInt64_unbox(ref: kotlin.native.internal.NativePtr): ULong {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as ULong
}

@ExportedBridge("KotlinBridgeable_Bool_box")
public fun KotlinBridgeable_Bool_box(value: Boolean): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_Bool_unbox")
public fun KotlinBridgeable_Bool_unbox(ref: kotlin.native.internal.NativePtr): Boolean {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Boolean
}

@ExportedBridge("KotlinBridgeable_Float_box")
public fun KotlinBridgeable_Float_box(value: Float): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_Float_unbox")
public fun KotlinBridgeable_Float_unbox(ref: kotlin.native.internal.NativePtr): Float {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Float
}

@ExportedBridge("KotlinBridgeable_Double_box")
public fun KotlinBridgeable_Double_box(value: Double): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(value as kotlin.Any)
}

@ExportedBridge("KotlinBridgeable_Double_unbox")
public fun KotlinBridgeable_Double_unbox(ref: kotlin.native.internal.NativePtr): Double {
    return kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Double
}

// _KotlinBridgeable bridge functions for String

@ExportedBridge("KotlinBridgeable_String_box")
public fun KotlinBridgeable_String_box(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val str = interpretObjCPointer<String>(value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(str)
}

@ExportedBridge("KotlinBridgeable_String_unbox")
public fun KotlinBridgeable_String_unbox(ref: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val str = kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as String
    return str.objcPtr()
}

// _KotlinBridgeable bridge functions for collection types

@ExportedBridge("KotlinBridgeable_Array_box")
public fun KotlinBridgeable_Array_box(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val list = interpretObjCPointer<List<*>>(value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(list)
}

@ExportedBridge("KotlinBridgeable_Array_unbox")
public fun KotlinBridgeable_Array_unbox(ref: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val list = kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as List<*>
    return list.objcPtr()
}

@ExportedBridge("KotlinBridgeable_Set_box")
public fun KotlinBridgeable_Set_box(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val set = interpretObjCPointer<Set<*>>(value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(set)
}

@ExportedBridge("KotlinBridgeable_Set_unbox")
public fun KotlinBridgeable_Set_unbox(ref: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val set = kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Set<*>
    return set.objcPtr()
}

@ExportedBridge("KotlinBridgeable_Dictionary_box")
public fun KotlinBridgeable_Dictionary_box(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val map = interpretObjCPointer<Map<*, *>>(value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(map)
}

@ExportedBridge("KotlinBridgeable_Dictionary_unbox")
public fun KotlinBridgeable_Dictionary_unbox(ref: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val map = kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Map<*, *>
    return map.objcPtr()
}

// _KotlinBridgeable type-tag dispatch for __createBridgeable

@ExportedBridge("KotlinBridgeable_getTypeTag")
public fun KotlinBridgeable_getTypeTag(ref: kotlin.native.internal.NativePtr): Int {
    val obj = kotlin.native.internal.ref.dereferenceExternalRCRef(ref)
    return when (obj) {
        is String -> 1
        is Byte -> 2
        is Short -> 3
        is Int -> 4
        is Long -> 5
        is UByte -> 6
        is UShort -> 7
        is UInt -> 8
        is ULong -> 9
        is Boolean -> 10
        is Float -> 11
        is Double -> 12
        is List<*> -> 13
        is Set<*> -> 14
        is Map<*, *> -> 15
        else -> 0
    }
}

@ExportedBridge("KotlinBridgeable_disposeRef")
public fun KotlinBridgeable_disposeRef(ref: kotlin.native.internal.NativePtr) {
    kotlin.native.internal.ref.releaseExternalRCRef(ref)
    kotlin.native.internal.ref.disposeExternalRCRef(ref)
}