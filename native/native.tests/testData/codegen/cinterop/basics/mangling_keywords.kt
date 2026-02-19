// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: mangling_keywords.def
---
#define as "as"
#define class "class"
#define dynamic "dynamic"
#define false "false"
#define fun "fun"
#define in "in"
#define interface "interface"
#define is "is"
#define null "null"
#define object "object"
#define package "package"
#define super "super"
#define this "this"
#define throw "throw"
#define true "true"
#define try "try"
#define typealias "typealias"
#define val "val"
#define var "var"
#define when "when"
// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlin.test.*
import mangling_keywords.*

fun box(): String {
    // Check that all Kotlin keywords are imported and mangled.
    assertEquals("as", `as`)
    assertEquals("class", `class`)
    assertEquals("dynamic", `dynamic`)
    assertEquals("false", `false`)
    assertEquals("fun", `fun`)
    assertEquals("in", `in`)
    assertEquals("interface", `interface`)
    assertEquals("is", `is`)
    assertEquals("null", `null`)
    assertEquals("object", `object`)
    assertEquals("package", `package`)
    assertEquals("super", `super`)
    assertEquals("this", `this`)
    assertEquals("throw", `throw`)
    assertEquals("true", `true`)
    assertEquals("try", `try`)
    assertEquals("typealias", `typealias`)
    assertEquals("val", `val`)
    assertEquals("var", `var`)
    assertEquals("when", `when`)
    return "OK"
}

