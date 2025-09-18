// MODULE: lib
// FILE: lib.kt
package ab

@JsQualifier("a.b")
external fun c(): String

@JsQualifier("a.b")
external val d: String

// MODULE: main(lib)
// FILE: main.kt

package main

fun box() = ab.c() + ab.d
