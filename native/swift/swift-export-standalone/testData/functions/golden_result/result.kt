import kotlin.native.internal.ExportedBridge

@ExportedBridge("namespace1_local_functions_foo")
public fun namespace1_local_functions_foo(): Unit {
    val result = namespace1.local_functions.foo()
    return result
}

@ExportedBridge("namespace1_main_foobar__TypesOfArguments__int32_t__")
public fun namespace1_main_foobar(param: Int): Int {
    val result = namespace1.main.foobar(param)
    return result
}

@ExportedBridge("namespace1_main_all_args__TypesOfArguments___Bool_int8_t_int16_t_int32_t_int64_t_uint8_t_uint16_t_uint32_t_uint64_t_float_double__")
public fun namespace1_main_all_args(arg1: Boolean, arg2: Byte, arg3: Short, arg4: Int, arg5: Long, arg6: UByte, arg7: UShort, arg8: UInt, arg9: ULong, arg10: Float, arg11: Double): Unit {
    val result = namespace1.main.all_args(arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8, arg9, arg10, arg11)
    return result
}

@ExportedBridge("namespace1_bar")
public fun namespace1_bar(): Int {
    val result = namespace1.bar()
    return result
}

@ExportedBridge("namespace2_foo__TypesOfArguments__int32_t__")
public fun namespace2_foo(arg1: Int): Int {
    val result = namespace2.foo(arg1)
    return result
}

@ExportedBridge("overload_foo__TypesOfArguments__int32_t__")
public fun overload_foo(arg1: Int): Int {
    val result = overload.foo(arg1)
    return result
}

@ExportedBridge("overload_foo__TypesOfArguments__double__")
public fun overload_foo(arg1: Double): Int {
    val result = overload.foo(arg1)
    return result
}

@ExportedBridge("__root___foo")
public fun __root___foo(): Int {
    val result = foo()
    return result
}

