@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(Enum::class, "4main4EnumC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("Enum_a_get")
public fun Enum_a_get(): kotlin.native.internal.NativePtr {
    val _result = Enum.a
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Enum_b_get")
public fun Enum_b_get(): kotlin.native.internal.NativePtr {
    val _result = Enum.b
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Enum_entries_get")
public fun Enum_entries_get(): kotlin.native.internal.NativePtr {
    val _result = Enum.entries
    return _result.objcPtr()
}

@ExportedBridge("Enum_i_get")
public fun Enum_i_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Enum
    val _result = __self.i
    return _result
}

@ExportedBridge("Enum_i_set__TypesOfArguments__Swift_Int32__")
public fun Enum_i_set__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newValue: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Enum
    val __newValue = newValue
    __self.i = __newValue
}

@ExportedBridge("Enum_print")
public fun Enum_print(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as Enum
    val _result = __self.print()
    return _result.objcPtr()
}

@ExportedBridge("Enum_valueOf__TypesOfArguments__Swift_String__")
public fun Enum_valueOf__TypesOfArguments__Swift_String__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = Enum.valueOf(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

