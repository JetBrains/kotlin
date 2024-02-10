import kotlin.native.internal.ExportedBridge

@ExportedBridge("namespace1_local_functions_foo")
public fun namespace1_local_functions_foo(): Unit {
    val result = namespace1.local_functions.foo()
    return result
}

@ExportedBridge("namespace1_main_foobar")
public fun namespace1_main_foobar(param: Int): Int {
    val result = namespace1.main.foobar(param)
    return result
}

@ExportedBridge("namespace1_main_all_args")
public fun namespace1_main_all_args(arg1: Boolean, arg2: Byte, arg3: Short, arg4: Int, arg5: Long, arg10: Float, arg11: Double): Unit {
    val result = namespace1.main.all_args(arg1, arg2, arg3, arg4, arg5, arg10, arg11)
    return result
}

@ExportedBridge("namespace1_bar")
public fun namespace1_bar(): Int {
    val result = namespace1.bar()
    return result
}

@ExportedBridge("namespace2_foo")
public fun namespace2_foo(arg1: Int): Int {
    val result = namespace2.foo(arg1)
    return result
}

@ExportedBridge("overload_foo")
public fun overload_foo(arg1: Int): Int {
    val result = overload.foo(arg1)
    return result
}

@ExportedBridge("overload_foo")
public fun overload_foo(arg1: Double): Int {
    val result = overload.foo(arg1)
    return result
}

@ExportedBridge("__root___foo")
public fun __root___foo(): Int {
    val result = foo()
    return result
}

