/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.ring

private const val RUNS = 1_000_000

open class CallsBenchmark {

    interface I {
        fun foo(): Int

        fun foo1(): Int = 1
        fun foo2(): Int = 2
        fun foo3(): Int = 3
        fun foo4(): Int = 4
        fun foo5(): Int = 5
        fun foo6(): Int = 6
        fun foo7(): Int = 7
        fun foo8(): Int = 8
        fun foo9(): Int = 9
        fun foo10(): Int = 10
        fun foo11(): Int = 11
        fun foo12(): Int = 12
        fun foo13(): Int = 13
        fun foo14(): Int = 14
        fun foo15(): Int = 15
        fun foo16(): Int = 16
        fun foo17(): Int = 17
        fun foo18(): Int = 18
        fun foo19(): Int = 19
        fun foo20(): Int = 20
        fun foo21(): Int = 21
        fun foo22(): Int = 22
        fun foo23(): Int = 23
        fun foo24(): Int = 24
        fun foo25(): Int = 25
        fun foo26(): Int = 26
        fun foo27(): Int = 27
        fun foo28(): Int = 28
        fun foo29(): Int = 29
        fun foo30(): Int = 30
        fun foo31(): Int = 31
        fun foo32(): Int = 32
        fun foo33(): Int = 33
        fun foo34(): Int = 34
        fun foo35(): Int = 35
        fun foo36(): Int = 36
        fun foo37(): Int = 37
        fun foo38(): Int = 38
        fun foo39(): Int = 39
        fun foo40(): Int = 40
        fun foo41(): Int = 41
        fun foo42(): Int = 42
        fun foo43(): Int = 43
        fun foo44(): Int = 44
        fun foo45(): Int = 45
        fun foo46(): Int = 46
        fun foo47(): Int = 47
        fun foo48(): Int = 48
        fun foo49(): Int = 49
        fun foo50(): Int = 50
        fun foo51(): Int = 51
        fun foo52(): Int = 52
        fun foo53(): Int = 53
        fun foo54(): Int = 54
        fun foo55(): Int = 55
        fun foo56(): Int = 56
        fun foo57(): Int = 57
        fun foo58(): Int = 58
        fun foo59(): Int = 59
        fun foo60(): Int = 60
        fun foo61(): Int = 61
        fun foo62(): Int = 62
        fun foo63(): Int = 63
        fun foo64(): Int = 64
        fun foo65(): Int = 65
        fun foo66(): Int = 66
        fun foo67(): Int = 67
        fun foo68(): Int = 68
        fun foo69(): Int = 69
        fun foo70(): Int = 70
        fun foo71(): Int = 71
        fun foo72(): Int = 72
        fun foo73(): Int = 73
        fun foo74(): Int = 74
        fun foo75(): Int = 75
        fun foo76(): Int = 76
        fun foo77(): Int = 77
        fun foo78(): Int = 78
        fun foo79(): Int = 79
        fun foo80(): Int = 80
        fun foo81(): Int = 81
        fun foo82(): Int = 82
        fun foo83(): Int = 83
        fun foo84(): Int = 84
        fun foo85(): Int = 85
        fun foo86(): Int = 86
        fun foo87(): Int = 87
        fun foo88(): Int = 88
        fun foo89(): Int = 89
        fun foo90(): Int = 90
        fun foo91(): Int = 91
        fun foo92(): Int = 92
        fun foo93(): Int = 93
        fun foo94(): Int = 94
        fun foo95(): Int = 95
        fun foo96(): Int = 96
        fun foo97(): Int = 97
        fun foo98(): Int = 98
        fun foo99(): Int = 99
    }

    abstract class A : I

    open class B : A() {
        override fun foo() = 42
    }

    open class C : A() {
        override fun foo() = 117
    }

    class D: A() {
        override fun foo() = 314
    }

    open class X : A() {
        override fun foo() = 456456
    }

    open class Y : A() {
        override fun foo() = -398473
    }

    open class Z : A() {
        override fun foo() = 78298734
    }

    val d = D()
    val a1: A = B()
    val a2: A = C()
    val a3: A = d
    val i1: I = a1
    val i2: I = a2
    val i3: I = d
    val i4: I = X()
    val i5: I = Y()
    val i6: I = Z()

    fun finalMethodCall(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val d = d
        for (i in 0 until RUNS)
            x += d.foo()
        return x
    }

    fun classOpenMethodCall_MonomorphicCallsite(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val a1 = a1
        for (i in 0 until RUNS)
            x += a1.foo()
        return x
    }

    fun classOpenMethodCall_BimorphicCallsite(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val a1 = a1
        val a2 = a2
        for (i in 0 until RUNS)
            x += (if (i and 1 == 0) a1 else a2).foo()
        return x
    }

    fun classOpenMethodCall_TrimorphicCallsite(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val a1 = a1
        val a2 = a2
        val a3 = a3
        for (i in 0 until RUNS)
            x += (when (i % 3) {
                1 -> a1
                2 -> a2
                else -> a3
            }).foo()
        return x
    }

    fun interfaceMethodCall_MonomorphicCallsite(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val i1 = i1
        for (i in 0 until RUNS)
            x += i1.foo()
        return x
    }

    fun interfaceMethodCall_BimorphicCallsite(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val i1 = i1
        val i2 = i2
        for (i in 0 until RUNS)
            x += (if (i and 1 == 0) i1 else i2).foo()
        return x
    }

    fun interfaceMethodCall_TrimorphicCallsite(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val i1 = i1
        val i2 = i2
        val i3 = i3
        for (i in 0 until RUNS)
            x += (when (i % 3) {
                1 -> i1
                2 -> i2
                else -> i3
            }).foo()
        return x
    }

    fun interfaceMethodCall_HexamorphicCallsite(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val i1 = i1
        val i2 = i2
        val i3 = i3
        val i4 = i4
        val i5 = i5
        val i6 = i6
        for (i in 0 until RUNS)
            x += (when (i % 6) {
                1 -> i1
                2 -> i2
                3 -> i3
                4 -> i4
                5 -> i5
                else -> i6
            }).foo()
        return x
    }

    abstract class E {
        abstract fun foo(): Any
    }

    open class F : E() {
        override fun foo(): Int = 42
    }

    val e: E = F()

    fun returnBoxUnboxFolding(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val e = e
        for (i in 0 until RUNS)
            x += e.foo() as Int
        return x
    }

    abstract class G<in T> {
        abstract fun foo(x: T): Int
    }

    open class H : G<Int>() {
        override fun foo(x: Int): Int {
            return x
        }
    }

    val g: G<Any> = H() as G<Any>

    fun parameterBoxUnboxFolding(): Int {
        var x = 0
        // TODO: optimize fields accesses
        val g = g
        for (i in 0 until RUNS)
            x += g.foo(i)
        return x
    }
}