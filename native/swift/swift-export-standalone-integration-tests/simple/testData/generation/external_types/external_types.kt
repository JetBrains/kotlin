// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlinx.cinterop.ExperimentalForeignApi
// MODULE: main
// FILE: main.kt

fun produce_nsdate(): platform.Foundation.NSDate = TODO()

fun consume_nsdate(date: platform.Foundation.NSDate): Unit = TODO()

var store_nsdate: platform.Foundation.NSDate
    get() = TODO()
    set(newValue) = TODO()

// FILE: unsupported.kt
import platform.CoreGraphics.CGRectEdge

var store_cvalue = platform.CoreGraphics.CGRectMake(1.0, 1.0, 1.0, 1.0)

public fun produce_cenum(): CGRectEdge = platform.CoreGraphics.CGRectEdge.CGRectMaxXEdge
