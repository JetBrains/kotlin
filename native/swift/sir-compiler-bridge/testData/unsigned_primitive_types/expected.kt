import kotlin.native.internal.ExportForCppRuntime

@ExportForCppRuntime("b_bridge")
public fun b_bridge(): UShort {
    val result = b()
    return result
}

@ExportForCppRuntime("c_bridge")
public fun c_bridge(): UInt {
    val result = c()
    return result
}

@ExportForCppRuntime("d_bridge")
public fun d_bridge(): ULong {
    val result = d()
    return result
}

@ExportForCppRuntime("e_bridge")
public fun e_bridge(): UByte {
    val result = e()
    return result
}