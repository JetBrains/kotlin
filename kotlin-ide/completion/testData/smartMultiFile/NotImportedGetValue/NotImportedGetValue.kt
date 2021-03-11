package main

import dependency.X1
import dependency.X2
import dependency.X3
import dependency.Y1
import dependency.Y2
import dependency.Y3

fun createX1() = X1()
fun createX2() = X2()
fun createX3() = X3()
fun createY1() = Y1()
fun createY2() = Y2()
fun createY3() = Y3()

class C {
    val property by <caret>
}

// EXIST: createX1
// ABSENT: createX2
// EXIST: createX3
// EXIST: createY1
// ABSENT: createY2
// EXIST: createY3
