@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___BOOLEAN_CONST_get")
public fun __root___BOOLEAN_CONST_get(): Boolean {
    val _result = run { BOOLEAN_CONST }
    return _result
}

@ExportedBridge("__root___BYTE_CONST_get")
public fun __root___BYTE_CONST_get(): Byte {
    val _result = run { BYTE_CONST }
    return _result
}

@ExportedBridge("__root___CHAR_CONST_get")
public fun __root___CHAR_CONST_get(): Char {
    val _result = run { CHAR_CONST }
    return _result
}

@ExportedBridge("__root___DOUBLE_CONST_get")
public fun __root___DOUBLE_CONST_get(): Double {
    val _result = run { DOUBLE_CONST }
    return _result
}

@ExportedBridge("__root___FLOAT_CONST_get")
public fun __root___FLOAT_CONST_get(): Float {
    val _result = run { FLOAT_CONST }
    return _result
}

@ExportedBridge("__root___INT_CONST_get")
public fun __root___INT_CONST_get(): Int {
    val _result = run { INT_CONST }
    return _result
}

@ExportedBridge("__root___LONG_CONST_get")
public fun __root___LONG_CONST_get(): Long {
    val _result = run { LONG_CONST }
    return _result
}

@ExportedBridge("__root___SHORT_CONST_get")
public fun __root___SHORT_CONST_get(): Short {
    val _result = run { SHORT_CONST }
    return _result
}

@ExportedBridge("__root___STRING_CONST_get")
public fun __root___STRING_CONST_get(): kotlin.native.internal.NativePtr {
    val _result = run { STRING_CONST }
    return _result.objcPtr()
}

@ExportedBridge("__root___UBYTE_CONST_get")
public fun __root___UBYTE_CONST_get(): UByte {
    val _result = run { UBYTE_CONST }
    return _result
}

@ExportedBridge("__root___UINT_CONST_get")
public fun __root___UINT_CONST_get(): UInt {
    val _result = run { UINT_CONST }
    return _result
}

@ExportedBridge("__root___ULONG_CONST_get")
public fun __root___ULONG_CONST_get(): ULong {
    val _result = run { ULONG_CONST }
    return _result
}

@ExportedBridge("__root___USHORT_CONST_get")
public fun __root___USHORT_CONST_get(): UShort {
    val _result = run { USHORT_CONST }
    return _result
}

@ExportedBridge("__root___baz_get")
public fun __root___baz_get(): Int {
    val _result = run { baz }
    return _result
}

@ExportedBridge("__root___foo_get")
public fun __root___foo_get(): kotlin.native.internal.NativePtr {
    val _result = run { foo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___foo_set__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun __root___foo_set__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as kotlin.Any
    val _result = run { foo = __newValue }
    return run { _result; true }
}

@ExportedBridge("__root___lateinit_foo_get")
public fun __root___lateinit_foo_get(): kotlin.native.internal.NativePtr {
    val _result = run { lateinit_foo }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___lateinit_foo_set__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun __root___lateinit_foo_set__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(newValue: kotlin.native.internal.NativePtr): Boolean {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as kotlin.Any
    val _result = run { lateinit_foo = __newValue }
    return run { _result; true }
}

@ExportedBridge("inline_barGet_get")
public fun inline_barGet_get(): Int {
    val _result = run { `inline`.barGet }
    return _result
}

@ExportedBridge("inline_barGet_set__TypesOfArguments__Swift_Int32__")
public fun inline_barGet_set__TypesOfArguments__Swift_Int32__(newValue: Int): Boolean {
    val __newValue = newValue
    val _result = run { `inline`.barGet = __newValue }
    return run { _result; true }
}

@ExportedBridge("inline_barSet_get")
public fun inline_barSet_get(): Int {
    val _result = run { `inline`.barSet }
    return _result
}

@ExportedBridge("inline_barSet_set__TypesOfArguments__Swift_Int32__")
public fun inline_barSet_set__TypesOfArguments__Swift_Int32__(newValue: Int): Boolean {
    val __newValue = newValue
    val _result = run { `inline`.barSet = __newValue }
    return run { _result; true }
}

@ExportedBridge("inline_bar_get")
public fun inline_bar_get(): Int {
    val _result = run { `inline`.bar }
    return _result
}

@ExportedBridge("inline_bar_set__TypesOfArguments__Swift_Int32__")
public fun inline_bar_set__TypesOfArguments__Swift_Int32__(newValue: Int): Boolean {
    val __newValue = newValue
    val _result = run { `inline`.bar = __newValue }
    return run { _result; true }
}

@ExportedBridge("inline_fooGet_get")
public fun inline_fooGet_get(): kotlin.native.internal.NativePtr {
    val _result = run { `inline`.fooGet }
    return _result.objcPtr()
}

@ExportedBridge("inline_foo_get")
public fun inline_foo_get(): kotlin.native.internal.NativePtr {
    val _result = run { `inline`.foo }
    return _result.objcPtr()
}

@ExportedBridge("namespace_main_bar_get")
public fun namespace_main_bar_get(): Int {
    val _result = run { namespace.main.bar }
    return _result
}

@ExportedBridge("namespace_main_bar_set__TypesOfArguments__Swift_Int32__")
public fun namespace_main_bar_set__TypesOfArguments__Swift_Int32__(newValue: Int): Boolean {
    val __newValue = newValue
    val _result = run { namespace.main.bar = __newValue }
    return run { _result; true }
}

@ExportedBridge("namespace_main_foo_get")
public fun namespace_main_foo_get(): Int {
    val _result = run { namespace.main.foo }
    return _result
}

@ExportedBridge("namespace_main_foobar__TypesOfArguments__Swift_Int32__")
public fun namespace_main_foobar__TypesOfArguments__Swift_Int32__(`param`: Int): Int {
    val __param = `param`
    val _result = run { namespace.main.foobar(__param) }
    return _result
}
