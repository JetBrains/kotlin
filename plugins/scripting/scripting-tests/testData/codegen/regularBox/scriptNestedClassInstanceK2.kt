// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: test.kt

fun box(): String =
    Nested().x

// FILE: script.kts

class Nested {
    val x = "OK"
}
