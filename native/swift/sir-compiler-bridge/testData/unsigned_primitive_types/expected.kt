import kotlin.native.internal.ExportedBridge

@ExportedBridge("b_bridge")
public fun b_bridge(): UShort {
    val result = b()
    return result
}

@ExportedBridge("c_bridge")
public fun c_bridge(): UInt {
    val result = c()
    return result
}

@ExportedBridge("d_bridge")
public fun d_bridge(): ULong {
    val result = d()
    return result
}

@ExportedBridge("e_bridge")
public fun e_bridge(): UByte {
    val result = e()
    return result
}