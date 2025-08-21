@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Array::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE5ArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Enum::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE4EnumC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Enum.Companion::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE4EnumC9CompanionC")
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

@ExportedBridge("kotlin_Enum_Companion_get")
public fun kotlin_Enum_Companion_get(): kotlin.native.internal.NativePtr {
    val _result = kotlin.Enum.Companion
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Enum_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__")
public fun kotlin_Enum_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.compareTo(__other)
    return _result
}

@ExportedBridge("kotlin_Enum_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_Enum_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = __self.equals(__other)
    return _result
}

@ExportedBridge("kotlin_Enum_hashCode")
public fun kotlin_Enum_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.hashCode()
    return _result
}

@ExportedBridge("kotlin_Enum_name_get")
public fun kotlin_Enum_name_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.name
    return _result.objcPtr()
}

@ExportedBridge("kotlin_Enum_ordinal_get")
public fun kotlin_Enum_ordinal_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.ordinal
    return _result
}

@ExportedBridge("kotlin_Enum_toString")
public fun kotlin_Enum_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.toString()
    return _result.objcPtr()
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
