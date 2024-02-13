import kotlin.native.internal.ExportedBridge

@ExportedBridge("a_b_bar_bridge__TypesOfArguments__int32_t_int64_t__")
public fun a_b_bar_bridge(param1: Int, param2: Long): Int {
    val result = a.b.bar(param1, param2)
    return result
}

@ExportedBridge("a_b_foo_bridge__TypesOfArguments__int32_t_int64_t__")
public fun a_b_foo_bridge(param1: Int, param2: Long): Boolean {
    val result = a.b.foo(param1, param2)
    return result
}
