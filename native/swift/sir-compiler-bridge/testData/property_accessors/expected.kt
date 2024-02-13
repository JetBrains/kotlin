import kotlin.native.internal.ExportedBridge

@ExportedBridge("getter_bridge")
public fun getter_bridge(): Boolean {
    val result = variable()
    return result
}

@ExportedBridge("setter_bridge__TypesOfArguments___Bool__")
public fun setter_bridge(newValue: Boolean): Unit {
    val result = variable(newValue)
    return result
}
