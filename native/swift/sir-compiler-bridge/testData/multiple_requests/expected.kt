import kotlin.native.internal.ExportForCppRuntime

@ExportForCppRuntime("a_b_bar_bridge")
public fun a_b_bar_bridge(param1: Int, param2: Long): Int {
    val result = a.b.bar(param1, param2)
    return result
}

@ExportForCppRuntime("a_b_foo_bridge")
public fun a_b_foo_bridge(param1: Int, param2: Long): Boolean {
    val result = a.b.foo(param1, param2)
    return result
}
