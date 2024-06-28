import kotlin.native.internal.ExportedBridge

@ExportedBridge("getter_bridge")
public fun getter_bridge(): Boolean {
    val _result = variable
    return _result
}

@ExportedBridge("setter_bridge__TypesOfArguments___Bool__")
public fun setter_bridge(newValue: Boolean): Unit {
    val __newValue = newValue
    variable = __newValue
}
