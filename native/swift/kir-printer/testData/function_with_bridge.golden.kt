@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*

public fun foo() {
    Lib_foo()
}

@ImportedBridge("Lib_foo")
internal external fun Lib_foo(): Unit
