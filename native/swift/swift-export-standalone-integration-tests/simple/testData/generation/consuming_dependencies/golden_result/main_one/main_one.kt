@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("org_main_first_main_first")
public fun org_main_first_main_first(): Int {
    val _result = org.main.first.main_first()
    return _result
}

