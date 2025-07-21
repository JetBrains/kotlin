@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("EnumSimple_entries_get")
public fun EnumSimple_entries_get(): kotlin.native.internal.NativePtr {
    val _result = EnumSimple.entries
    return _result.objcPtr()
}

@ExportedBridge("EnumSimple_valueOf__TypesOfArguments__Swift_String__")
public fun EnumSimple_valueOf__TypesOfArguments__Swift_String__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = EnumSimple.valueOf(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumWithAbstractMembers_blue")
public fun EnumWithAbstractMembers_blue(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as EnumWithAbstractMembers
    val _result = __self.blue()
    return _result
}

@ExportedBridge("EnumWithAbstractMembers_entries_get")
public fun EnumWithAbstractMembers_entries_get(): kotlin.native.internal.NativePtr {
    val _result = EnumWithAbstractMembers.entries
    return _result.objcPtr()
}

@ExportedBridge("EnumWithAbstractMembers_green")
public fun EnumWithAbstractMembers_green(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as EnumWithAbstractMembers
    val _result = __self.green()
    return _result
}

@ExportedBridge("EnumWithAbstractMembers_ordinalSquare")
public fun EnumWithAbstractMembers_ordinalSquare(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as EnumWithAbstractMembers
    val _result = __self.ordinalSquare()
    return _result
}

@ExportedBridge("EnumWithAbstractMembers_red_get")
public fun EnumWithAbstractMembers_red_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as EnumWithAbstractMembers
    val _result = __self.red
    return _result
}

@ExportedBridge("EnumWithAbstractMembers_valueOf__TypesOfArguments__Swift_String__")
public fun EnumWithAbstractMembers_valueOf__TypesOfArguments__Swift_String__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = EnumWithAbstractMembers.valueOf(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumWithMembers_entries_get")
public fun EnumWithMembers_entries_get(): kotlin.native.internal.NativePtr {
    val _result = EnumWithMembers.entries
    return _result.objcPtr()
}

@ExportedBridge("EnumWithMembers_foo")
public fun EnumWithMembers_foo(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as EnumWithMembers
    val _result = __self.foo()
    return _result.objcPtr()
}

@ExportedBridge("EnumWithMembers_isNorth_get")
public fun EnumWithMembers_isNorth_get(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as EnumWithMembers
    val _result = __self.isNorth
    return _result
}

@ExportedBridge("EnumWithMembers_valueOf__TypesOfArguments__Swift_String__")
public fun EnumWithMembers_valueOf__TypesOfArguments__Swift_String__(value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = EnumWithMembers.valueOf(__value)
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

@ExportedBridge("__root___enumId__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__")
public fun __root___enumId__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__(e: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __e = kotlin.native.internal.ref.dereferenceExternalRCRef(e) as kotlin.Enum<kotlin.Enum<*>>
    val _result = enumId(__e)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ewamValues")
public fun __root___ewamValues(): kotlin.native.internal.NativePtr {
    val _result = ewamValues()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___yellow")
public fun __root___yellow(): kotlin.native.internal.NativePtr {
    val _result = yellow()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
