@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("__root___BOOLEAN_CONST_get")
public fun __root___BOOLEAN_CONST_get(): Boolean {
    val _result = BOOLEAN_CONST
    return _result
}

@ExportedBridge("__root___BYTE_CONST_get")
public fun __root___BYTE_CONST_get(): Byte {
    val _result = BYTE_CONST
    return _result
}

@ExportedBridge("__root___CHAR_CONST_get")
public fun __root___CHAR_CONST_get(): Char {
    val _result = CHAR_CONST
    return _result
}

@ExportedBridge("__root___DOUBLE_CONST_get")
public fun __root___DOUBLE_CONST_get(): Double {
    val _result = DOUBLE_CONST
    return _result
}

@ExportedBridge("__root___FLOAT_CONST_get")
public fun __root___FLOAT_CONST_get(): Float {
    val _result = FLOAT_CONST
    return _result
}

@ExportedBridge("__root___INT_CONST_get")
public fun __root___INT_CONST_get(): Int {
    val _result = INT_CONST
    return _result
}

@ExportedBridge("__root___LONG_CONST_get")
public fun __root___LONG_CONST_get(): Long {
    val _result = LONG_CONST
    return _result
}

@ExportedBridge("__root___SHORT_CONST_get")
public fun __root___SHORT_CONST_get(): Short {
    val _result = SHORT_CONST
    return _result
}

@ExportedBridge("__root___STRING_CONST_get")
public fun __root___STRING_CONST_get(): kotlin.native.internal.NativePtr {
    val _result = STRING_CONST
    return _result.objcPtr()
}

@ExportedBridge("__root___UBYTE_CONST_get")
public fun __root___UBYTE_CONST_get(): UByte {
    val _result = UBYTE_CONST
    return _result
}

@ExportedBridge("__root___UINT_CONST_get")
public fun __root___UINT_CONST_get(): UInt {
    val _result = UINT_CONST
    return _result
}

@ExportedBridge("__root___ULONG_CONST_get")
public fun __root___ULONG_CONST_get(): ULong {
    val _result = ULONG_CONST
    return _result
}

@ExportedBridge("__root___USHORT_CONST_get")
public fun __root___USHORT_CONST_get(): UShort {
    val _result = USHORT_CONST
    return _result
}

@ExportedBridge("__root___baz_get")
public fun __root___baz_get(): Int {
    val _result = baz
    return _result
}

@ExportedBridge("__root___foo_get")
public fun __root___foo_get(): kotlin.native.internal.NativePtr {
    val _result = foo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo_set__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun __root___foo_set__TypesOfArguments__KotlinRuntime_KotlinBase__(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as kotlin.Any
    foo = __newValue
}

@ExportedBridge("__root___lateinit_foo_get")
public fun __root___lateinit_foo_get(): kotlin.native.internal.NativePtr {
    val _result = lateinit_foo
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___lateinit_foo_set__TypesOfArguments__KotlinRuntime_KotlinBase__")
public fun __root___lateinit_foo_set__TypesOfArguments__KotlinRuntime_KotlinBase__(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as kotlin.Any
    lateinit_foo = __newValue
}

@ExportedBridge("namespace_main_bar_get")
public fun namespace_main_bar_get(): Int {
    val _result = namespace.main.bar
    return _result
}

@ExportedBridge("namespace_main_bar_set__TypesOfArguments__Swift_Int32__")
public fun namespace_main_bar_set__TypesOfArguments__Swift_Int32__(newValue: Int): Unit {
    val __newValue = newValue
    namespace.main.bar = __newValue
}

@ExportedBridge("namespace_main_foo_get")
public fun namespace_main_foo_get(): Int {
    val _result = namespace.main.foo
    return _result
}

@ExportedBridge("namespace_main_foobar__TypesOfArguments__Swift_Int32__")
public fun namespace_main_foobar__TypesOfArguments__Swift_Int32__(`param`: Int): Int {
    val __param = `param`
    val _result = namespace.main.foobar(__param)
    return _result
}

