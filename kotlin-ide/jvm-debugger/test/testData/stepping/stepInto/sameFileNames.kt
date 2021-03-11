// FILE: sameFileNames.kt
package sameFileNames

fun main() {
    //Breakpoint!
    val result = simple.foo()
}

// STEP_INTO: 1

// FILE: simple.kt
package simple

// test more than one file for package in JetPositionManager:prepareTypeMapper. the second file in this package is singleBreakpoint/simple.kt
fun foo() {
}