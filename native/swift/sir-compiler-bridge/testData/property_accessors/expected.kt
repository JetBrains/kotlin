@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("getter_bridge")
public fun getter_bridge(): Boolean {
    val _result = variable
    return _result
}

@ExportedBridge("setter_bridge__TypesOfArguments__Bool__")
public fun setter_bridge__TypesOfArguments__Bool__(newValue: Boolean): Unit {
    val __newValue = newValue
    variable = __newValue
}
