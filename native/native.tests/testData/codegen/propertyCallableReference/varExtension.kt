// OUTPUT_DATA_FILE: varExtension.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

class A(y: Int) {
    var x = y
}

var A.z: Int
    get() = this.x
    set(value: Int) {
        this.x = value
    }

fun box(): String {
    val p1 = A::z
    val a = A(42)
    p1.set(a, 117)
    println(a.x)
    println(p1.get(a))
    val p2 = a::z
    p2.set(42)
    println(a.x)
    println(p2.get())

    return "OK"
}
