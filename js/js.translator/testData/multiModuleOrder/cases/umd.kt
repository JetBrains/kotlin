// MODULE: lib
// FILE: lib.kt
// MODULE_KIND: UMD
package lib

fun bar() = "OK"


// MODULE: main(lib)
// FILE: main.kt
// MODULE_KIND: UMD
package foo

import lib.bar

fun box() = bar()