import kotlin.native.internal.ExportedBridge

@ExportedBridge("a_bridge")
public fun a_bridge(): Boolean {
    val _result = a()
    return _result
}

@ExportedBridge("b_bridge")
public fun b_bridge(): Short {
    val _result = b()
    return _result
}

@ExportedBridge("c_bridge")
public fun c_bridge(): Int {
    val _result = c()
    return _result
}

@ExportedBridge("d_bridge")
public fun d_bridge(): Long {
    val _result = d()
    return _result
}

@ExportedBridge("e_bridge")
public fun e_bridge(): Byte {
    val _result = e()
    return _result
}
