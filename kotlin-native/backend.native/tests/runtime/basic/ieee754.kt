/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.ieee754

import kotlin.test.*

@Test fun runTest() {
    val v = Float.POSITIVE_INFINITY
    val i = v.toInt()
    println("$v $i ${i.toShort()}")

    val a = 42
    val b = Float.MAX_VALUE
    println("${a + b}")

    val s = Float.NaN.toInt().toShort()
    println("NAN2SHORT:: $s")

    val d: Float = Float.MAX_VALUE
    val d2i = d.toInt()
    println("$d2i ${d2i.toShort()}")

    for (f in arrayOf(Float.POSITIVE_INFINITY, Float.MAX_VALUE / 2, Float.MAX_VALUE,
            3.14f, Float.NaN, -33333.12312f, Float.MIN_VALUE, Float.NEGATIVE_INFINITY,
            -1.2f, -12.6f, 2.3f)) {
        println("FLOAT:: $f   INT:: ${f.toInt()}")
    }

    println("OK")
}