import kotlin.native.internal.ExportedBridge

@ExportedBridge("getter_bridge")
public fun getter_bridge(): Boolean {
    return variable
}

@ExportedBridge("setter_bridge__TypesOfArguments___Bool__")
public fun setter_bridge(newValue: Boolean): Unit {
    variable = newValue
}
