/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import a.*

open class C: B() {
    override val x: Int = 156

    fun foo() {
        println(x)

        println(super<B>.x)
        bar()
    }
}

fun main(args: Array<String>) {
    val c = C()
    c.foo()
}
