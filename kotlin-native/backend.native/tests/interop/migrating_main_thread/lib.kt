/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

class A {
    var i = 0
}

private var globalA = A()

fun writeToA(i: Int) {
    globalA.i = i
}

fun tryReadFromA(default: Int): Int {
    return try {
        globalA.i
    } catch (e: IncorrectDereferenceException) {
        default
    }
}
