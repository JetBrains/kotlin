@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlinx.cinterop.*
import kotlin.native.internal.ImportedBridge
import kotlin.native.internal.ExportedBridge
import platform.darwin.NSObject

/**
 * Universal Kotlin wrapper for a Swift error that crosses into Kotlin through a reverse bridge.
 * This is the symmetric counterpart of the Swift `KotlinError` struct.
 */
public class SwiftError internal constructor(
    internal val box: NSObject,
) : RuntimeException() {
    override val message: String? get() = box.description()
}

public fun throwableFromReverseBridge(ref: kotlin.native.internal.NativePtr): kotlin.Throwable {
    val throwable = kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as kotlin.Throwable
    kotlin.native.internal.ref.releaseExternalRCRef(ref)
    kotlin.native.internal.ref.disposeExternalRCRef(ref)
    return throwable
}

public fun throwErrorFromReverseBridge(errorRef: kotlinx.cinterop.COpaquePointer?) {
    if (errorRef == null) return
    throw throwableFromReverseBridge(errorRef.rawValue)
}

public fun dereferenceAndDisposeBridgeableRef(ref: kotlin.native.internal.NativePtr): kotlin.Any? {
    if (ref == kotlin.native.internal.NativePtr.NULL) return null
    val obj = kotlin.native.internal.ref.dereferenceExternalRCRef(ref)
    kotlin.native.internal.ref.releaseExternalRCRef(ref)
    kotlin.native.internal.ref.disposeExternalRCRef(ref)
    return obj
}

@ExportedBridge("SwiftError_create")
public fun SwiftError_create(box: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val boxObject = interpretObjCPointer<NSObject>(box)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(SwiftError(boxObject))
}

@ExportedBridge("SwiftError_retainedThrowableRef")
public fun SwiftError_retainedThrowableRef(ref: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val throwable = kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as? kotlin.Throwable ?: return kotlin.native.internal.NativePtr.NULL
    return kotlin.native.internal.ref.createRetainedExternalRCRef(throwable)
}

@ExportedBridge("SwiftError_unwrapBoxOrNull")
public fun SwiftError_unwrapBoxOrNull(ref: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val throwable = kotlin.native.internal.ref.dereferenceExternalRCRef(ref)
    return (throwable as? SwiftError)?.box?.objcPtr() ?: kotlin.native.internal.NativePtr.NULL
}

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
    return when (val v = kotlin.native.internal.ref.dereferenceExternalRCRef(ref)) {
        is UShort -> v
        is Char -> v.code.toUShort()
        else -> v as UShort
    }
}

@ExportedBridge("KotlinBridgeable_Char_unbox")
public fun KotlinBridgeable_Char_unbox(ref: kotlin.native.internal.NativePtr): UShort {
    return (kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Char).code.toUShort()
}

@ExportedBridge("KotlinBridgeable_Unit_box")
public fun KotlinBridgeable_Unit_box(): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(Unit)
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

@ExportedBridge("KotlinBridgeable_List_create")
public fun KotlinBridgeable_List_create(capacity: Int): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(ArrayList<kotlin.Any?>(capacity))
}

@ExportedBridge("KotlinBridgeable_List_add")
@Suppress("UNCHECKED_CAST")
public fun KotlinBridgeable_List_add(listRef: kotlin.native.internal.NativePtr, elemRef: kotlin.native.internal.NativePtr) {
    val list = kotlin.native.internal.ref.dereferenceExternalRCRef(listRef) as MutableList<kotlin.Any?>
    list.add(dereferenceAndDisposeBridgeableRef(elemRef))
}

@ExportedBridge("KotlinBridgeable_List_size")
public fun KotlinBridgeable_List_size(ref: kotlin.native.internal.NativePtr): Int {
    return (kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as List<*>).size
}

@ExportedBridge("KotlinBridgeable_List_get")
public fun KotlinBridgeable_List_get(ref: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val elem = (kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as List<*>)[index]
    return if (elem == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(elem)
}

@ExportedBridge("KotlinBridgeable_Set_create")
public fun KotlinBridgeable_Set_create(capacity: Int): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(LinkedHashSet<kotlin.Any?>(capacity))
}

@ExportedBridge("KotlinBridgeable_Set_add")
@Suppress("UNCHECKED_CAST")
public fun KotlinBridgeable_Set_add(setRef: kotlin.native.internal.NativePtr, elemRef: kotlin.native.internal.NativePtr) {
    val set = kotlin.native.internal.ref.dereferenceExternalRCRef(setRef) as MutableSet<kotlin.Any?>
    set.add(dereferenceAndDisposeBridgeableRef(elemRef))
}

@ExportedBridge("KotlinBridgeable_Set_toList")
public fun KotlinBridgeable_Set_toList(ref: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val set = kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Set<*>
    return kotlin.native.internal.ref.createRetainedExternalRCRef(set.toList())
}

@ExportedBridge("KotlinBridgeable_Map_create")
public fun KotlinBridgeable_Map_create(capacity: Int): kotlin.native.internal.NativePtr {
    return kotlin.native.internal.ref.createRetainedExternalRCRef(LinkedHashMap<kotlin.Any?, kotlin.Any?>(capacity))
}

@ExportedBridge("KotlinBridgeable_Map_put")
@Suppress("UNCHECKED_CAST")
public fun KotlinBridgeable_Map_put(
    mapRef: kotlin.native.internal.NativePtr,
    keyRef: kotlin.native.internal.NativePtr,
    valRef: kotlin.native.internal.NativePtr,
) {
    val map = kotlin.native.internal.ref.dereferenceExternalRCRef(mapRef) as MutableMap<kotlin.Any?, kotlin.Any?>
    map[dereferenceAndDisposeBridgeableRef(keyRef)] = dereferenceAndDisposeBridgeableRef(valRef)
}

@ExportedBridge("KotlinBridgeable_Map_entriesList")
public fun KotlinBridgeable_Map_entriesList(ref: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val map = kotlin.native.internal.ref.dereferenceExternalRCRef(ref) as Map<*, *>
    val flat = ArrayList<kotlin.Any?>(map.size * 2)
    for (entry in map.entries) {
        flat.add(entry.key)
        flat.add(entry.value)
    }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(flat)
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
        is Char -> 16
        is Unit -> 17
        else -> 0
    }
}

@ExportedBridge("KotlinBridgeable_cloneRef")
public fun KotlinBridgeable_cloneRef(ref: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    if (ref == kotlin.native.internal.NativePtr.NULL) return kotlin.native.internal.NativePtr.NULL
    val obj = kotlin.native.internal.ref.dereferenceExternalRCRef(ref)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(obj)
}

@ExportedBridge("KotlinBridgeable_disposeRef")
public fun KotlinBridgeable_disposeRef(ref: kotlin.native.internal.NativePtr) {
    kotlin.native.internal.ref.releaseExternalRCRef(ref)
    kotlin.native.internal.ref.disposeExternalRCRef(ref)
}

@ImportedBridge("KotlinRuntimeSupport_linkAnchor")
internal external fun KotlinRuntimeSupport_linkAnchor()

@ExportedBridge("KotlinRuntimeSupport_ensureLinked")
public fun KotlinRuntimeSupport_ensureLinked() {
    KotlinRuntimeSupport_linkAnchor()
}
