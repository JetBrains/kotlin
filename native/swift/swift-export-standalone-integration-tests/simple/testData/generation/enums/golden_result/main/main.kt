@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("EnumSimple_FIRST")
public fun EnumSimple_FIRST(): kotlin.native.internal.NativePtr {
    val _result = EnumSimple.FIRST
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumSimple_LAST")
public fun EnumSimple_LAST(): kotlin.native.internal.NativePtr {
    val _result = EnumSimple.LAST
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumSimple_SECOND")
public fun EnumSimple_SECOND(): kotlin.native.internal.NativePtr {
    val _result = EnumSimple.SECOND
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumWithAbstractMembers_MAGENTA")
public fun EnumWithAbstractMembers_MAGENTA(): kotlin.native.internal.NativePtr {
    val _result = EnumWithAbstractMembers.MAGENTA
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumWithAbstractMembers_SKY")
public fun EnumWithAbstractMembers_SKY(): kotlin.native.internal.NativePtr {
    val _result = EnumWithAbstractMembers.SKY
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumWithAbstractMembers_YELLOW")
public fun EnumWithAbstractMembers_YELLOW(): kotlin.native.internal.NativePtr {
    val _result = EnumWithAbstractMembers.YELLOW
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumWithAbstractMembers_blue")
public fun EnumWithAbstractMembers_blue(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as EnumWithAbstractMembers
    val _result = __self.blue()
    return _result
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

@ExportedBridge("EnumWithMembers_NORTH")
public fun EnumWithMembers_NORTH(): kotlin.native.internal.NativePtr {
    val _result = EnumWithMembers.NORTH
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("EnumWithMembers_SOUTH")
public fun EnumWithMembers_SOUTH(): kotlin.native.internal.NativePtr {
    val _result = EnumWithMembers.SOUTH
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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

@ExportedBridge("Enum_a")
public fun Enum_a(): kotlin.native.internal.NativePtr {
    val _result = Enum.a
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("Enum_b")
public fun Enum_b(): kotlin.native.internal.NativePtr {
    val _result = Enum.b
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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

@ExportedBridge("__root___enumId__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__")
public fun __root___enumId__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__(e: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __e = kotlin.native.internal.ref.dereferenceExternalRCRef(e) as kotlin.Enum<*>
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
