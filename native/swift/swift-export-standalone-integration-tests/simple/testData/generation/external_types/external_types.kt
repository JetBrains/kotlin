// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlinx.cinterop.ExperimentalForeignApi
// MODULE: main
// FILE: main.kt

fun produce_nsdate(): platform.Foundation.NSDate = TODO()

fun consume_nsdate(date: platform.Foundation.NSDate): Unit = TODO()

var store_nsdate: platform.Foundation.NSDate
    get() = TODO()
    set(newValue) = TODO()

fun produce_typealias(): platform.Foundation.NSTimeInterval = TODO()

var store_cgReck: platform.CoreGraphics.CGRect
    get() = TODO()
    set(newValue) = TODO()
