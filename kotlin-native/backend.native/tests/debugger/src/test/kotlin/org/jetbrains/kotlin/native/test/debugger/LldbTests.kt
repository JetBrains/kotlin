/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.native.test.debugger.lldbCommandRunOrContinue
import org.jetbrains.kotlin.native.test.debugger.lldbComplexTest
import org.jetbrains.kotlin.native.test.debugger.lldbTest
import org.junit.Test

class LldbTests {
    @Test
    fun `can step through code`() = lldbTest("""
        fun main(args: Array<String>) {
            var x = 1
            var y = 2
            var z = x + y
            println(z)
        }
    """, """
        > b main.kt:2
        Breakpoint 1: [..]

        > ${lldbCommandRunOrContinue()}
        Process [..] stopped
        [..] stop reason = breakpoint 1.1
        [..] at main.kt:2[..]

        > n
        Process [..] stopped
        [..] stop reason = step over
        [..] at main.kt:3[..]

        > n
        Process [..] stopped
        [..] stop reason = step over
        [..] at main.kt:4[..]

        > n
        Process [..] stopped
        [..] stop reason = step over
        [..] at main.kt:5[..]
    """)

    @Test
    fun `can inspect values of primitive types`() = lldbTest("""
        fun main(args: Array<String>) {
            var a: Byte =  1
            var b: Int  =  2
            var c: Long = -3
            var d: Char = 'c'
            var e: Boolean = true
            return
        }
    """, """
            > b main.kt:7
            > ${lldbCommandRunOrContinue()}
            > fr var
            (char) a = '\x01'
            (int) b = 2
            (long) c = -3
            (short) d = 99
            (bool) e = true
    """)

    @Test
    fun `can inspect classes`() = lldbTest("""
        fun main(args: Array<String>) {
            val point = Point(1, 2)
            val person = Person()
            return
        }

        data class Point(val x: Int, val y: Int)
        class Person {
            override fun toString() = "John Doe"
        }
    """, """
        > b main.kt:4
        > ${lldbCommandRunOrContinue()}
        > fr var
        (ObjHeader *) args = []
        (ObjHeader *) point = [x: ..., y: ...]
        (ObjHeader *) person = []
    """)

    @Test
    fun `can inspect arrays`() = lldbTest("""
        fun main(args: Array<String>) {
            val xs = IntArray(3)
            xs[0] = 1
            xs[1] = 2
            xs[2] = 3
            val ys: Array<Any?> = arrayOfNulls(2)
            ys[0] = Point(1, 2)
            return
        }

        data class Point(val x: Int, val y: Int)
    """, """
        > b main.kt:8
        > ${lldbCommandRunOrContinue()}
        > fr var
        (ObjHeader *) args = []
        (ObjHeader *) xs = [..., ..., ...]
        (ObjHeader *) ys = [..., ...]
    """)

    @Test
    fun `can inspect array children`() = lldbTest("""
        fun main(args: Array<String>) {
            val xs = intArrayOf(3, 5, 8)
            return
        }

        data class Point(val x: Int, val y: Int)
    """, """
        > b main.kt:3
        > ${lldbCommandRunOrContinue()}
        > fr var xs
        (ObjHeader *) xs = [..., ..., ...]
    """)

    @Test
    fun `swift with kotlin static framework`() = lldbComplexTest {
        val aKtSrc = """
            fun a() = "a"
        """.feedOutput("a.kt")
        val bKtSrc = """
            fun b() = "b"
        """.feedOutput("b.kt")

        arrayOf(aKtSrc, bKtSrc).framework("AandBFramework", "-g", "-Xstatic-framework")

        val swiftSrc = """
            import AandBFramework

            print(AKt.a())
            print(BKt.b())

        """.feedOutput("test.swift")
        val application = swiftc("application", swiftSrc, "-F", root.toString())
        """
            > b kfun:#b(){}kotlin.String
            Breakpoint 1: where = [..]`kfun:#b(){}kotlin.String [..] at b.kt:1:1, [..]

            > b kfun:#a(){}kotlin.String
            Breakpoint 2: where = [..]`kfun:#a(){}kotlin.String [..] at a.kt:1:1, [..]
        """.trimIndent().lldb(application)
    }

    @Test
    fun `kt33055`() = lldbComplexTest {
        val kt33055 = """
            |fun question(subject: String, names: Array<String> = emptyArray()): String {
            |    return buildString { // breakpoint here
            |        append("${"$"}subject?") // actual stop
            |        for (name in names)
            |            append(" ${"$"}name?")
            |    }
            |}
            |
            |fun main(args: Array<String>) {
            |    println(question("Subject", args))
            |}
        """.trimMargin().binary("kt33055", "-g", "-Xg-generate-inline-function-body-marker=enable")
        """
            > b 2
            Breakpoint 1: where = [..]`kfun:#question(kotlin.String;kotlin.Array<kotlin.String>){}kotlin.String [..] at kt33055.kt:2:12, [..]
        """.trimIndent().lldb(kt33055)
    }
}