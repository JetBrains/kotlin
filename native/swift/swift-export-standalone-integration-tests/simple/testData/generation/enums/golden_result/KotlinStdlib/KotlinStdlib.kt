@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Enum::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE4EnumC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Enum.Companion::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE4EnumC9CompanionC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

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

@ExportedBridge("kotlin_Enum_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun kotlin_Enum_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
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
