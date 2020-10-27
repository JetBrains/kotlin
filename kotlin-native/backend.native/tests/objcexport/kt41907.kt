/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kt41907

class Ckt41907

interface Ikt41907 {
    fun foo(c: Ckt41907)
}

private class Bkt41907 {
    var c: Ckt41907? = null
}

private val b = Bkt41907()

fun escapeC(c: Ckt41907) {
    b.c = c
}

fun testKt41907(o: Ikt41907) {
    val c = Ckt41907()
    o.foo(c)
}
