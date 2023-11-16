import kotlin.native.internal.ExportForCppRuntime

@ExportForCppRuntime("a_bridge")
public fun a_bridge(): Boolean {
    val result = a()
    return result
}

@ExportForCppRuntime("b_bridge")
public fun b_bridge(): Short {
    val result = b()
    return result
}

@ExportForCppRuntime("c_bridge")
public fun c_bridge(): Int {
    val result = c()
    return result
}

@ExportForCppRuntime("d_bridge")
public fun d_bridge(): Long {
    val result = d()
    return result
}

@ExportForCppRuntime("e_bridge")
public fun e_bridge(): Byte {
    val result = e()
    return result
}