// MODULE: lib
// FILE: lib.kt

package lib

import platform.Foundation.NSString

fun createNSString(): NSString {
    val x: NSString = NSString()
    return x
}

// MODULE: app(lib)
// FILE: app.kt

package app

import lib.createNSString
import platform.Foundation.NSString

fun main() {
    val x: NSString = createNSString()
    check(x.length.toInt() == 0)
}
