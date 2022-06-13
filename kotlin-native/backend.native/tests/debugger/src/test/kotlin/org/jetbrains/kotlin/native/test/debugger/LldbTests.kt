/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import org.jetbrains.kotlin.native.test.debugger.lldbCommandRunOrContinue
import org.jetbrains.kotlin.native.test.debugger.lldbComplexTest
import org.jetbrains.kotlin.native.test.debugger.lldbCheckLineNumbers
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
    fun `can inspect catch parameter`() = lldbTest("""
        fun main() {
            try {
                throw Exception("message 1")
            } catch (e1: Throwable) {
                println(e1.message)
            }

            try {
                throwError()
            } catch (e2: Throwable) {
                println(e2.message)
            }
        }

        fun throwError() {
            throw Error("message 2")
        }
    """, """
            > b main.kt:5
            > ${lldbCommandRunOrContinue()}
            > fr var
            (ObjHeader *) e1 = [..]
            > b main.kt:11
            > c
            > fr var
            (ObjHeader *) e2 = [..]
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
            Breakpoint 1: where = [..]`kfun:#b(){}kotlin.String [..] at b.kt:1:1, [..]

            > b kfun:#a(){}kotlin.String
            Breakpoint 2: where = [..]`kfun:#a(){}kotlin.String [..] at a.kt:1:1, [..]
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
              frame #1: [..] kt42208.kexe`kfun:main${'$'}lambda${'$'}0#internal at kt42208-2.kt:3:18
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #3: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-UNN>invoke(_this=[..]){}kotlin.Nothing#internal at kt42208-1.kt:2:5
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
            * frame #0: [..] kt42208.kexe`kfun:main${'$'}lambda${'$'}0#internal at kt42208-2.kt:5:5
              frame #1: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-BNN>invoke(_this=[..]){}kotlin.Boolean#internal at kt42208-1.kt:2:5
              frame #3: [..] kt42208.kexe`kfun:#main(){} at kt42208-1.kt:3:5
              frame #4: [..] kt42208.kexe`Konan_start(args=[..]) at kt42208-1.kt:1:1
              frame #5: [..]
            > c
            > bt
            * thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
            * frame #0: [..] kt42208.kexe`kfun:main${'$'}lambda${'$'}0#internal at kt42208-2.kt:5:5
              frame #1: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-BNN>invoke(_this=[..]){}kotlin.Boolean#internal at kt42208-1.kt:2:5
              frame #3: [..] kt42208.kexe`kfun:#main(){} at kt42208-1.kt:4:5
              frame #4: [..] kt42208.kexe`Konan_start(args=[..]) at kt42208-1.kt:1:1
            > c
            > bt
            * thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
            * frame #0: [..] kt42208.kexe`kfun:main${'$'}lambda${'$'}0#internal at kt42208-2.kt:5:5
              frame #1: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-BNN>invoke(_this=[..]){}kotlin.Boolean#internal at kt42208-1.kt:2:5
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
        val binary = arrayOf(kt42208One, kt42208Two, kt42208Three).binary("kt42208", "-g", "-XXLanguage:+UnitConversionsOnArbitraryExpressions")
        """
            > b kt42208-2.kt:5
            > ${lldbCommandRunOrContinue()}
            > bt
            * thread #1, queue = 'com.apple.main-thread', stop reason = breakpoint 1.1
            * frame #0: [..] kt42208.kexe`kfun:main${'$'}lambda${'$'}0#internal at kt42208-2.kt:5:5
              frame #1: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.invoke#internal(_this=[..]) at kt42208-1.kt:2:5
              frame #2: [..] kt42208.kexe`kfun:${'$'}main${'$'}lambda${'$'}0${'$'}FUNCTION_REFERENCE${'$'}0.${'$'}<bridge-BNN>invoke(_this=[..]){}kotlin.Boolean#internal at kt42208-1.kt:2:5
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
            Breakpoint 1: where = kt47198.kexe`kfun:#foo(kotlin.Int){} [..] at kt47198.kt:1:1, [..]
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
            Breakpoint 1: where = kt47198.kexe`kfun:#foo(kotlin.Int){} [..] at kt47198.kt:1:[..]
            > ${lldbCommandRunOrContinue()}
            > fr v
            (int) a = 33
            > q
        """.trimIndent().lldb(binary)
    }

    @Test
    fun `lldb line numbers are valid in source`() {
        // Whitespace is important, since the character offsets are what defines source lines
        lldbCheckLineNumbers(mapOf(
                "main.kt" to """
    
    fun main() {
    
        inliner {
        
            println("1")
            
        }
        
        inliner {
            
            println("2")
            
        }
    
    }
    
""", "inliner.kt" to """
    ${" ".repeat(1000)}
    inline fun inliner(block: ()->Unit) {
    
        block()
    
    }
    
"""), "main.kt:2", 15)
    }

    @Test
    fun `works in native thread state`() = lldbComplexTest {
        // This test checks that K/N runtime debug interface properly handles the cases when a calling thread is in native state.

        // So we need to stop on a breakpoint in native code, but inspect Kotlin code.
        // To achieve that, we set breakpoint on `write` function, and call `println` to trigger it.
        // The `test` function is recursive -- this little trick helps us to switch to frame 10 in the debugger,
        // and be sure that it is in `test` function regardless of how the inline works for println and its callees.

        val program = """
            fun main() {
                test(10)
            }

            fun test(n: Int) {
                val myData = MyData(1, longArrayOf(2, 3), "four", arrayOf("five", "six"))
                val i = 7
                val la = longArrayOf(8, 9)
                val s = "ten"
                val a = arrayOf("eleven")

                if (n > 0) test(n - 1)

                println("Hello")
            }

            class MyData(val i: Int, val la: LongArray, val s: String, val a: Array<String>)

        """.trimIndent().binary("nativestate", "-g", "-Xbinary=runtimeAssertionsMode=panic")

        // Now we can just stop on `write`, switch frame to Kotlin code and try using different debug interface functions, both directly
        // and indirectly (through konan_lldb.py, which integrates the built-in lldb capabilities like `frame variable` command with
        // K/N debug interface).
        """
            > b write
            Breakpoint 1: [..]
            > ${lldbCommandRunOrContinue()}
            [..] stop reason = breakpoint [..]
            > frame select 10
            -> 12  	    if (n > 0) test(n - 1)
            > frame variable
            (int) i = 7
            (ObjHeader *) myData = [la: ..., s: ..., a: ..., i: ...]
            (ObjHeader *) la = [..., ...]
            (ObjHeader *) s = [..]
            (ObjHeader *) a = [...]
            > expression -- (int32_t)Konan_DebugPrint(s)
            ten(int32_t) [..] = 0
            > expression -- (int32_t)Konan_DebugPrint(la)
            [8, 9](int32_t) [..] = 0
            > expression -- (int32_t)Konan_DebugPrint(myData)
            MyData@[..](int32_t) [..] = 0
            > script lldb.frame.FindVariable("myData").GetChildMemberWithName("i").Dereference().GetValue()
            '1'
            > q
        """.trimIndent().lldb(program)
    }

}