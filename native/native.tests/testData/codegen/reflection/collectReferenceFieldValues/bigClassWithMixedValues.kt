/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.native.internal.*
import kotlin.test.*

value class BoxInt(val x: Int)
value class BoxBoxInt(val x: BoxInt)
data class A(val x: Int)
value class BoxA(val x: A)
value class BoxBoxA(val x: BoxA)


class X(
        val a: Int,
        val b: List<Int>,
        val c: IntArray,
        val d: Array<Int>,
        val e: Array<Any>,
        val f: BoxInt,
        val g: BoxBoxInt,
        val h: A,
        val i: BoxA,
        val j: BoxBoxA,
        val k: Any?,
        val l: Any?,
        val m: Any?,
        val n: Any?,
        val o: IntArray?
) {
    val p by lazy { 1 }
    lateinit var q: IntArray
    lateinit var r: IntArray
}

fun box(): String {
    val lst = listOf(1, 2, 3)
    val ia = intArrayOf(1, 2, 3)
    val ia2 = intArrayOf(3, 4, 5)
    val ai = arrayOf(1, 2, 3)
    val astr: Array<Any> = arrayOf("123", "456", 1, 2, 3)
    val bi = BoxInt(2)
    val bbi = BoxBoxInt(BoxInt(3))
    val a1 = A(1)
    val a2 = A(2)
    val a3 = A(3)
    val a6 = A(3)
    val x = X(
            1, lst, ia, ai, astr, bi, bbi,
            a1, BoxA(a2), BoxBoxA(BoxA(a3)),
            4, BoxInt(5), BoxA(a6), null, null
    )
    x.r = ia2
    val fields = x.collectReferenceFieldValues()
    assertEquals(12, fields.size)
    // not using assertContains because of ===
    assertTrue(fields.any { it === lst }, "Should contain list $lst")
    assertTrue(fields.any { it === ia }, "Should contain int array $ia")
    assertTrue(fields.any { it === ia2 }, "Should contain int array $ia2")
    assertTrue(fields.any { it === ai }, "Should contain array of int $ai")
    assertTrue(fields.any { it === astr }, "Should contain array of string $astr")
    assertTrue(fields.any { it === a1 }, "Should contain A(1)")
    assertTrue(fields.any { it === a2 }, "Should contain A(2)")
    assertTrue(fields.any { it === a3 }, "Should contain A(3)")
    assertTrue(fields.any { it.toString().startsWith("Lazy value not initialized yet") }, "Should contain lazy delegate")
    assertContains(fields, 4)
    assertContains(fields, BoxInt(5))
    assertContains(fields, BoxA(a6))

    return "OK"
}
