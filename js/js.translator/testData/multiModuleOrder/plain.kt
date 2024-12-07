// SPLIT_PER_MODULE
// MODULE: lib
// FILE: lib.kt
package lib

fun bar() = "OK"


// MODULE: main(lib)
// FILE: main.kt
package foo

import lib.bar

fun box() = bar()