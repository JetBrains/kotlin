// OUTPUT_DATA_FILE: interfaceCallNoEntryClass.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

interface A {
    fun foo(): String
}

enum class Zzz(val zzz: String, val x: Int) : A {
    Z1("z1", 1),
    Z2("z2", 2),
    Z3("z3", 3);

    override fun foo(): String{
        return "('$zzz', $x)"
    }
}

fun box(): String {
    println(Zzz.Z3.foo())
    val a: A = Zzz.Z3
    println(a.foo())

    return "OK"
}
