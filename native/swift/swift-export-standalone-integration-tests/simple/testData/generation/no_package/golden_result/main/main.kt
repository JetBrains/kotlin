@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("__root___meaningOfLife")
public fun __root___meaningOfLife(): Int {
    val _result = meaningOfLife()
    return _result
}

