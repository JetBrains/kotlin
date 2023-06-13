// SPLIT_PER_MODULE
// MODULE: lib
// MODULE_KIND: UMD
// FILE: lib.kt
package lib

fun bar() = "OK"


// MODULE: main(lib)
// MODULE_KIND: UMD
// FILE: main.kt
package foo

import lib.bar

fun box() = bar()