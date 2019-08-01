// TARGET_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS


// FILE: file1.kt

package foo.bar.baz

// import a.*
import a.b.*
import C3

fun box(): String = "OK"

@JsExport
class C1

@JsExport
fun f(x1: C1, x2: C2, x3: C3) {}

// FILE: file2.kt

@file:JsExport

package a.b

// import a.*
import foo.bar.baz.*
import C3

class C2
fun f(x1: C1, x2: C2, x3: C3) {}

// FILE: file3.kt

@file:JsExport

import a.b.*
import foo.bar.baz.*

@JsExport
external interface C3

@JsExport
fun f(x1: C1, x2: C2, x3: C3) {}