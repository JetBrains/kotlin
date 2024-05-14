import kotlin.native.internal.ExportedBridge

@ExportedBridge("__root___foo")
public fun __root___foo(): Int {
    val _result = foo()
    return _result
}

@ExportedBridge("namespace1_bar")
public fun namespace1_bar(): Int {
    val _result = namespace1.bar()
    return _result
}

@ExportedBridge("namespace1_local_functions_foo")
public fun namespace1_local_functions_foo(): Unit {
    namespace1.local_functions.foo()
}

@ExportedBridge("namespace1_main_all_args__TypesOfArguments___Bool_int8_t_int16_t_int32_t_int64_t_uint8_t_uint16_t_uint32_t_uint64_t_float_double__")
public fun namespace1_main_all_args(arg1: Boolean, arg2: Byte, arg3: Short, arg4: Int, arg5: Long, arg6: UByte, arg7: UShort, arg8: UInt, arg9: ULong, arg10: Float, arg11: Double): Unit {
    val __arg1 = arg1
    val __arg2 = arg2
    val __arg3 = arg3
    val __arg4 = arg4
    val __arg5 = arg5
    val __arg6 = arg6
    val __arg7 = arg7
    val __arg8 = arg8
    val __arg9 = arg9
    val __arg10 = arg10
    val __arg11 = arg11
    namespace1.main.all_args(__arg1, __arg2, __arg3, __arg4, __arg5, __arg6, __arg7, __arg8, __arg9, __arg10, __arg11)
}

@ExportedBridge("namespace1_main_foobar__TypesOfArguments__int32_t__")
public fun namespace1_main_foobar(param: Int): Int {
    val __param = param
    val _result = namespace1.main.foobar(__param)
    return _result
}

@ExportedBridge("namespace2_foo__TypesOfArguments__int32_t__")
public fun namespace2_foo(arg1: Int): Int {
    val __arg1 = arg1
    val _result = namespace2.foo(__arg1)
    return _result
}

@ExportedBridge("overload_foo__TypesOfArguments__int32_t__")
public fun overload_foo(arg1: Int): Int {
    val __arg1 = arg1
    val _result = overload.foo(__arg1)
    return _result
}

@ExportedBridge("overload_foo__TypesOfArguments__double__")
public fun overload_foo(arg1: Double): Int {
    val __arg1 = arg1
    val _result = overload.foo(__arg1)
    return _result
}

