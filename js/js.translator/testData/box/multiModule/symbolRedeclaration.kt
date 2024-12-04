// IGNORE_BACKEND: JS_IR, JS_IR_ES6

// MODULE: AT
// FILE: at.kt
package foo

fun redeclaredFunction(): String = "OK"

// MODULE: A(AT)
// FILE: a.kt
package foo

fun userFunction(): String = redeclaredFunction()

// MODULE: main(A)
// FILE: main.kt
package foo

fun redeclaredFunction(): String = "Fail"

fun box(): String {
    assertEquals(redeclaredFunction(), "Fail")
    return userFunction()
}
