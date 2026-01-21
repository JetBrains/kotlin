/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.ring

private const val RUNS = 2_000_000

class CastsBenchmark {
    interface I0
    open class C0: I0
    interface I1
    open class C1: C0(), I1
    interface I2: I0
    open class C2: C1(), I2
    interface I3: I1
    open class C3: C2(), I3
    interface I4: I0, I2
    open class C4: C3(), I4
    interface I5: I3
    open class C5: C4(), I5
    interface I6: I0, I2, I4
    open class C6: C5(), I6
    interface I9: I0, I2, I4
    open class C9: C5(), I9, I1
    interface I7: I1
    open class C7: C3(), I7
    interface I8: I0, I1
    open class C8: C3(), I8

    private fun foo_class(c: Any, x: Int, i: Int): Int {
        var x = x
        if (c is C0) x += i
        if (c is C1) x = x xor i
        if (c is C2) x += i
        if (c is C3) x = x xor i
        if (c is C4) x += i
        if (c is C5) x = x xor i
        if (c is C6) x += i
        if (c is C7) x = x xor i
        if (c is C8) x += i
        if (c is C9) x = x xor i
        return x
    }

    private fun foo_iface(c: Any, x: Int, i: Int): Int {
        var x = x
        if (c is I0) x += i
        if (c is I1) x = x xor i
        if (c is I2) x += i
        if (c is I3) x = x xor i
        if (c is I4) x += i
        if (c is I5) x = x xor i
        if (c is I6) x += i
        if (c is I7) x = x xor i
        if (c is I8) x += i
        if (c is I9) x = x xor i
        return x
    }

    fun classCast(): Int {
        val c0: Any = C0()
        val c1: Any = C1()
        val c2: Any = C2()
        val c3: Any = C3()
        val c4: Any = C4()
        val c5: Any = C5()
        val c6: Any = C6()
        val c7: Any = C7()
        val c8: Any = C8()
        val c9: Any = C9()

        var x = 0
        for (i in 0 until RUNS) {
            x += foo_class(c0, x, i)
            x += foo_class(c1, x, i)
            x += foo_class(c2, x, i)
            x += foo_class(c3, x, i)
            x += foo_class(c4, x, i)
            x += foo_class(c5, x, i)
            x += foo_class(c6, x, i)
            x += foo_class(c7, x, i)
            x += foo_class(c8, x, i)
            x += foo_class(c9, x, i)
        }
        return x
    }

    fun interfaceCast(): Int {
        val c0: Any = C0()
        val c1: Any = C1()
        val c2: Any = C2()
        val c3: Any = C3()
        val c4: Any = C4()
        val c5: Any = C5()
        val c6: Any = C6()
        val c7: Any = C7()
        val c8: Any = C8()
        val c9: Any = C9()

        var x = 0
        for (i in 0 until RUNS) {
            x += foo_iface(c0, x, i)
            x += foo_iface(c1, x, i)
            x += foo_iface(c2, x, i)
            x += foo_iface(c3, x, i)
            x += foo_iface(c4, x, i)
            x += foo_iface(c5, x, i)
            x += foo_iface(c6, x, i)
            x += foo_iface(c7, x, i)
            x += foo_iface(c8, x, i)
            x += foo_iface(c9, x, i)
        }
        return x
    }
}