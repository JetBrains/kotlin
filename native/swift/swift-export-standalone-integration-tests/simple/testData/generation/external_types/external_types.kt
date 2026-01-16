// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlinx.cinterop.ExperimentalForeignApi
// WITH_PLATFORM_LIBS
// APPLE_ONLY_VALIDATION
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

// NSURLCredential has SwiftName: URLCredential in API Notes
fun produce_nsurlcredential(): platform.Foundation.NSURLCredential = TODO()

fun consume_nsurlcredential(credential: platform.Foundation.NSURLCredential): Unit = TODO()
