// TARGET_BACKEND: JS_IR
// CHECK_TYPESCRIPT_DECLARATIONS
// RUN_PLAIN_BOX_FUNCTION

// FILE: file1.kt

package foo.bar.baz

import a.b.*
import C3

@JsExport
data class C1(val value: String)

@JsExport
fun f(x1: C1, x2: C2, x3: C3): String {
    return "foo.bar.baz.f($x1, $x2, $x3)"
}

// FILE: file2.kt

@file:JsExport

package a.b

import foo.bar.baz.*
import C3

data class C2(val value: String)
fun f(x1: C1, x2: C2, x3: C3): String {
    return "a.b.f($x1, $x2, $x3)"
}

// FILE: file3.kt

@file:JsExport

import a.b.*
import foo.bar.baz.*

@JsExport
data class C3(val value: String)

@JsExport
fun f(x1: C1, x2: C2, x3: C3): String {
    return "f($x1, $x2, $x3)"
}