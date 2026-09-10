@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("org_nonisolated_kotlin_internal_foo")
public fun org_nonisolated_kotlin_internal_foo(): Boolean {
    val _result = run { org.nonisolated.kotlin.`internal`.foo() }
    return run { _result; true }
}
