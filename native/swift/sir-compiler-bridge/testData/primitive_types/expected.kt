import kotlin.native.internal.ExportedBridge

@ExportedBridge("a_bridge")
public fun a_bridge(): Boolean {
    val result = a()
    return result
}

@ExportedBridge("b_bridge")
public fun b_bridge(): Short {
    val result = b()
    return result
}

@ExportedBridge("c_bridge")
public fun c_bridge(): Int {
    val result = c()
    return result
}

@ExportedBridge("d_bridge")
public fun d_bridge(): Long {
    val result = d()
    return result
}

@ExportedBridge("e_bridge")
public fun e_bridge(): Byte {
    val result = e()
    return result
}