// ES_MODULES
// WITH_STDLIB
// TS_COMPILATION_STRATEGY: EACH_FILE

// MODULE: lib1
// FILE: lib1a.kt
@JsExport
fun foo() = listOf(1, 2, 3)

// FILE: lib1b.kt
@JsExport
fun bar() = listOf(4, 5, 6)

// MODULE: lib2(lib1)
// FILE: lib2.kt
@JsExport
fun baz() = listOf(7, 8, 9)

// MODULE: main(lib1, lib2)
// FILE: main.kt
@JsExport
fun box(): String = "OK"
