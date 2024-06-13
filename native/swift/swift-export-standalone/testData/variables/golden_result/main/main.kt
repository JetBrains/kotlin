import kotlin.native.internal.ExportedBridge

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

@ExportedBridge("__root___Foo_init_allocate")
public fun __root___Foo_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<Foo>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___Foo_init_initialize__TypesOfArguments__uintptr_t__")
public fun __root___Foo_init_initialize(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)
    kotlin.native.internal.initInstance(____kt, Foo())
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

@ExportedBridge("__root___foo_set__TypesOfArguments__uintptr_t__")
public fun __root___foo_set(newValue: kotlin.native.internal.NativePtr): Unit {
    val __newValue = kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as Foo
    foo = __newValue
}

@ExportedBridge("namespace_main_bar_get")
public fun namespace_main_bar_get(): Int {
    val _result = namespace.main.bar
    return _result
}

@ExportedBridge("namespace_main_bar_set__TypesOfArguments__int32_t__")
public fun namespace_main_bar_set(newValue: Int): Unit {
    val __newValue = newValue
    namespace.main.bar = __newValue
}

@ExportedBridge("namespace_main_foo_get")
public fun namespace_main_foo_get(): Int {
    val _result = namespace.main.foo
    return _result
}

@ExportedBridge("namespace_main_foobar__TypesOfArguments__int32_t__")
public fun namespace_main_foobar(param: Int): Int {
    val __param = param
    val _result = namespace.main.foobar(__param)
    return _result
}
