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
        > q
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
            > q
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
        > q
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
        > q
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
        > q
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
            Breakpoint 1: where = [..]`kfun:#b(){}kotlin.String [..] at b.kt:1:12, [..]

            > b kfun:#a(){}kotlin.String
            Breakpoint 2: where = [..]`kfun:#a(){}kotlin.String [..] at a.kt:1:12, [..]
            > q
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
        """.trimMargin().binary("kt33055", "-g", "-Xg-generate-debug-trampoline=enable")
        """
            > b 2
            Breakpoint 1: where = [..]`kfun:#question(kotlin.String;kotlin.Array<kotlin.String>){}kotlin.String [..] at kt33055.kt:2:12, [..]
            > q
        """.trimIndent().lldb(kt33055)
    }

    @Test
    fun `kt33364`() = lldbComplexTest {
        val kt33364 = """
            |fun main() {
            |    val param = 3
            |
            |    //breakpoint here (line: 4, breakpoint is set to 5th line)
            |    when(param) {
            |        1 -> print("A")
            |        2 -> print("B")
            |        else -> print("C")
            |    }
            |
            |    // breakpoint here (line: 11, breakpoint is set to 12th line)
            |    when {
            |        param == 1 -> print("A")
            |        param == 2 -> print("B")
            |        else -> print("C")
            |    }
            |}
        """.trimMargin().binary("kt33364", "-g", "-Xg-generate-debug-trampoline=enable")
        """
            > b 5
            Breakpoint 1: where = [..]kfun:#main(){} [..] at kt33364.kt:5:[..]
            > b 11
            Breakpoint 2: where = [..]kfun:#main(){} [..] at kt33364.kt:12:[..]
            > q
        """.trimIndent().lldb(kt33364)
    }

    @Test
    fun `kt42208`() = lldbComplexTest {
        val kt42208One = """
            fun main() {
                foo()()
            }
        """.feedOutput("kt42208-1.kt")
        val kt42208Two = """
             // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
             inline fun foo() = {
                 throw Error()
             }
        """.feedOutput("kt42208-2.kt")
        val binary = arrayOf(kt42208One, kt42208Two).binary("kt42208", "-g")
        """
            > b ThrowException
            > ${lldbCommandRunOrContinue()}
            > bt
            * thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
            * frame #0: [..] kt42208.kexe`ThrowException
              frame #1: [..] kt42208.kexe`kfun:main${'$'}<anonymous>_1#internal at kt42208-2.kt:3:18
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #3: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-UNN>invoke(_this=[..]){}kotlin.Nothing#internal at kt42208-1.kt:2:5
              frame #4: [..] kt42208.kexe`kfun:#main(){} at kt42208-1.kt:2:5
              frame #5: [..] kt42208.kexe`Konan_start(args=[..]) at kt42208-1.kt:1:1
              frame #6: [..]
              frame #7: [..]
            > q
        """.trimIndent().lldb(binary)
    }

    @Test
    fun `kt42208 with variable`() = lldbComplexTest {
        val kt42208One = """
            fun main() {
                val a = foo()
                a()
                a()
                a()
            }
        """.feedOutput("kt42208-1.kt")
        val kt42208Two = """
             // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
             class A
             val list = mutableListOf<A>()
             inline fun foo() = { ->
                 list.add(A())
             }
        """.feedOutput("kt42208-2.kt")
        val binary = arrayOf(kt42208One, kt42208Two).binary("kt42208", "-g")
        """
            > b kt42208-2.kt:5
            > ${lldbCommandRunOrContinue()}
            > bt
            * thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
            * frame #0: [..] kt42208.kexe`kfun:main${'$'}<anonymous>_1#internal at kt42208-2.kt:5:5
              frame #1: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-BNN>invoke(_this=[..]){}kotlin.Boolean#internal at kt42208-1.kt:2:5
              frame #3: [..] kt42208.kexe`kfun:#main(){} at kt42208-1.kt:3:5
              frame #4: [..] kt42208.kexe`Konan_start(args=[..]) at kt42208-1.kt:1:1
              frame #5: [..]
            > c
            > bt
            * thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
            * frame #0: [..] kt42208.kexe`kfun:main${'$'}<anonymous>_1#internal at kt42208-2.kt:5:5
              frame #1: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-BNN>invoke(_this=[..]){}kotlin.Boolean#internal at kt42208-1.kt:2:5
              frame #3: [..] kt42208.kexe`kfun:#main(){} at kt42208-1.kt:4:5
              frame #4: [..] kt42208.kexe`Konan_start(args=[..]) at kt42208-1.kt:1:1
            > c
            > bt
            * thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
            * frame #0: [..] kt42208.kexe`kfun:main${'$'}<anonymous>_1#internal at kt42208-2.kt:5:5
              frame #1: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-BNN>invoke(_this=[..]){}kotlin.Boolean#internal at kt42208-1.kt:2:5
              frame #3: [..] kt42208.kexe`kfun:#main(){} at kt42208-1.kt:5:5
              frame #4: [..] kt42208.kexe`Konan_start(args=[..]) at kt42208-1.kt:1:1
            > q
        """.trimIndent().lldb(binary)
    }


    @Test
    fun `kt42208 with passing lambda to another function`() = lldbComplexTest {
        val kt42208One = """
            fun main() {
                val a = foo()
                bar(a)
            }
        """.feedOutput("kt42208-1.kt")
        val kt42208Two = """
             // aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa
             class A
             val list = mutableListOf<A>()
             inline fun foo() = { ->
                 list.add(A())
             }
        """.feedOutput("kt42208-2.kt")

        val kt42208Three = """
            fun bar(v:(()->Unit)) {
               v()
            }
        """.feedOutput("kt42208-3.kt")
        val binary = arrayOf(kt42208One, kt42208Two, kt42208Three).binary("kt42208", "-g", "-XXLanguage:+UnitConversion")
        """
            > b kt42208-2.kt:5
            > ${lldbCommandRunOrContinue()}
            > bt
            * thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
            * frame #0: [..] kt42208.kexe`kfun:main${'$'}<anonymous>_1#internal at kt42208-2.kt:5:5
              frame #1: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}<anonymous>_1${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-BNN>invoke(_this=[..]){}kotlin.Boolean#internal at kt42208-1.kt:2:5
              frame #3: [..] kt42208.kexe`kfun:#bar(v=[]){} at kt42208-3.kt:2:4
              frame #4: [..] kt42208.kexe`kfun:#main(){} at kt42208-1.kt:3:5
              frame #5: [..] kt42208.kexe`Konan_start(args=[]) at kt42208-1.kt:1:1
            > c
            > q
        """.trimIndent().lldb(binary)
    }

    @Test
    fun `kt47198`() = lldbComplexTest {
        val kt47198 = """
            fun foo(a:Int) = print("a: ${'$'}a")

            fun main() {
                foo(33)
            }
        """.feedOutput("kt47198.kt")

        val binary = arrayOf(kt47198).binary("kt47198", "-g")
        """
            > b 1
            Breakpoint 1: where = kt47198.kexe`kfun:#foo(kotlin.Int){} [..] at kt47198.kt:1:29, [..]
            > ${lldbCommandRunOrContinue()}
            > fr v
            (int) a = 33
            > q
        """.trimIndent().lldb(binary)
    }

    @Test
    fun `kt47198 with body`() = lldbComplexTest {
        val kt47198 = """
            fun foo(a:Int){
              print("a: ${'$'}a")
            }

            fun main() {
                foo(33)
            }
        """.feedOutput("kt47198.kt")

        val binary = arrayOf(kt47198).binary("kt47198", "-g")
        """
            > b 1
            Breakpoint 1: where = kt47198.kexe`kfun:#foo(kotlin.Int){} [..] at kt47198.kt:2:[..]
            > ${lldbCommandRunOrContinue()}
            > fr v
            (int) a = 33
            > q
        """.trimIndent().lldb(binary)
    }
}