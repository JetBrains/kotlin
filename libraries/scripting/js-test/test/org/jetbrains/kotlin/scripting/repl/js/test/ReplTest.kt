/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.repl.js.test

import junit.framework.TestCase
import org.jetbrains.kotlin.cli.common.repl.ReplCompileResult
import org.junit.Assert
import org.junit.Test
import org.jetbrains.kotlin.cli.common.repl.ReplEvalResult
import org.jetbrains.kotlin.scripting.repl.js.makeReplCodeLine

abstract class AbstractReplTestRunner : TestCase() {
    abstract fun getTester(): AbstractJsReplTest

    @Test
    fun testIndependentLines() {
        val lines = listOf(
            "var x = 38",
            "var y = 99",
            "4 + 1"
        )
        Assert.assertEquals(5, compileAndEval(lines))
    }

    @Test
    fun testDependentLines() {
        val lines = listOf(
            "var x = 7",
            "var y = 32",
            "x + y"
        )
        Assert.assertEquals(39, compileAndEval(lines))
    }

    @Test
    fun testFunctionCall() {
        val lines = listOf(
            "var x = 2",
            "var y = 3",
            "fun foo(x: Int, unused: Int) = x + y",
            "foo(x, x) * foo(y, y)"
        )
        Assert.assertEquals(30, compileAndEval(lines))
    }

    @Test
    fun testList() {
        val lines = listOf(
            "var a = 4",
            "var b = 6",
            "listOf(a, 5, b).last()"
        )
        Assert.assertEquals(6, compileAndEval(lines))
    }

    @Test
    fun testMatchingNames() {
        val lines = listOf(
            "fun foo(i: Int) = i + 2",
            "fun foo(s: String) = s",
            "class C {fun foo(s: String) = s + s}",
            "foo(\"x\") + foo(2) + C().foo(\"class\")"
        )
        Assert.assertEquals("x4classclass", compileAndEval(lines))
    }

    @Test
    fun testInline() {
        val lines = listOf(
            "inline fun foo(i : Int) = if (i % 2 == 0) {} else i",
            """
            fun box(): String {
                val a = foo(1)
                if (a != 1) return "fail1: ${'$'}a"

                val b = foo(2)
                if (b != Unit) return "fail2: ${'$'}b"

                return "OK"
            }
            """,
            "box()"
        )
        Assert.assertEquals("OK", compileAndEval(lines))
    }

    @Test
    fun testAnonymous() {
        val lines = listOf(
            """
            inline fun foo(f: () -> String): () -> String {
                val result = f()
                return { result }
            }
            """,
            "fun bar(f: () -> String) = foo(f)()",
            "fun box(): String = bar { \"OK\" }",
            "box()"
        )
        Assert.assertEquals("OK", compileAndEval(lines))
    }

    @Test
    fun testNoneLocalReturn() {
        val lines = listOf(
            """
            inline fun f(ignored: () -> Any): Any {
                return ignored()
            }
            """,
            """
            fun test(): String {
                f { return "OK" };
                return "error"
            }
            """,
            "test()"
        )
        Assert.assertEquals("OK", compileAndEval(lines))
    }

    @Test
    fun testInstanceOf() {
        val lines = listOf(
            """
            val list = listOf(1, 2, 3)
            val f: Boolean = list is List<Int>
            """,
            "val s = list is List<Int>",
            "f.toString() + s.toString()"
        )
        Assert.assertEquals("truetrue", compileAndEval(lines))
    }

    @Test
    fun testScopes() {
        val lines = listOf(
            """
            fun foo(): Int {
                var t = 2 * 2
                class A(val value: Int = 5) {
                    fun bar(): Int {
                        class B(val value: Int = 4) {
                            fun baz(): Int = value
                        }
                        var q = B().baz()
                        var w = 1
                        return q + w
                    }
                }

                return A().bar() * 2
            }
            foo()
            """
        )
        Assert.assertEquals(10, compileAndEval(lines))
    }

    @Test
    fun testEvaluateFunctionName() {
        val lines = listOf(
            "fun evaluateScript() = 5",
            "fun foo(i: Int) = i + evaluateScript()",
            "foo(5)"
        )
        Assert.assertEquals(10, compileAndEval(lines))
    }

    @Test
    fun testMemberDeclarations() {
        val lines = listOf(
            """
            val a = listOf(1, 2, 3, 4, 5)
            val str = "" + kotlin.math.PI
            str + a.subList(2, 3).toString() + a.lastIndexOf(4)
            """
        )
        Assert.assertEquals("3.141592653589793[3]3", compileAndEval(lines))
    }

    @Test
    fun testInitializeScriptFunction() {
        val lines = listOf(
            """
            var result = ""
            
            class Class(val x: Int = 10)
            result += Class().x
            
            fun function() = "#$@"
            result += function()
            
            val field = 123456
            result += field
            
            val sq: (x: Int) -> Int = { x -> x * x }
            result += "_" + sq(9)
            
            result += if (sq(5) % 2 == 0) {
                class I(val x: Int = 100)
                I().x 
            } else {
                class I(val x: String = "goo")
                I().x
            }
            
            result
            """
        )
        Assert.assertEquals("10#$@123456_81goo", compileAndEval(lines))
    }

    @Test
    fun testFunctionReference() {
        val lines = listOf(
            """
            fun foo(k: String) = "O" + k
            val f = ::foo
            f("K")
            """
        )

        Assert.assertEquals("OK", compileAndEval(lines))
    }

    @Test
    fun testPropertyReference() {
        val lines = listOf(
            """
            var r = ""
            val o = "O"
            val ro = ::o
            r += ro.get()
            r += ro()
            
            var k = "k"
            var rk = ::k
            r += rk.get()
            rk.set("y")
            r += rk()
            
            r
            """
        )

        Assert.assertEquals("OOky", compileAndEval(lines))
    }

    private fun compileAndEval(lines: List<String>): Any? {
        var result: Any? = null
        getTester().use { tester ->
            tester.reset()

            lines.forEach { line ->
                val compileResult = tester.compile(makeReplCodeLine(tester.newSnippetId(), line))
                if (compileResult !is ReplCompileResult.CompiledClasses) return compileResult.toString()

                val evalResult = tester.evaluate(compileResult)
                when (evalResult) {
                    is ReplEvalResult.Error.Runtime -> return evalResult.cause.toString()
                    !is ReplEvalResult.ValueResult -> return evalResult.toString()
                    else -> result = evalResult.value
                }
            }
        }
        return result
    }
}

class ReplTestRunnerAgainstKLib : AbstractReplTestRunner() {
    override fun getTester(): AbstractJsReplTest = JsReplTestAgainstKlib()
}

class ReplTestRunnerAgainstBinaries : AbstractReplTestRunner() {
    override fun getTester(): AbstractJsReplTest = tester

    companion object {
        private val tester = JsReplTestAgainstBinaries()
    }
}
