@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("org_nonisolated_kotlin_internal_foo")
public fun org_nonisolated_kotlin_internal_foo(): Unit {
    org.nonisolated.kotlin.`internal`.foo()
}
