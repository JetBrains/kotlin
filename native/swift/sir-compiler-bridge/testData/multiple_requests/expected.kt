@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("a_b_bar_bridge__TypesOfArguments__Int32_Int64__")
public fun a_b_bar_bridge__TypesOfArguments__Int32_Int64__(param1: Int, param2: Long): Int {
    val __param1 = param1
    val __param2 = param2
    val _result = a.b.bar(__param1, __param2)
    return _result
}

@ExportedBridge("a_b_foo_bridge__TypesOfArguments__Int32_Int64__")
public fun a_b_foo_bridge__TypesOfArguments__Int32_Int64__(param1: Int, param2: Long): Boolean {
    val __param1 = param1
    val __param2 = param2
    val _result = a.b.foo(__param1, __param2)
    return _result
}
