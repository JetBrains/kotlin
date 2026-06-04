/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("JUnitTestCaseWithNoTests")

package org.jetbrains.kotlin.konan.test.blackbox

import org.jetbrains.kotlin.konan.test.blackbox.support.TestCase
import org.jetbrains.kotlin.konan.test.blackbox.support.TestKind
import org.jetbrains.kotlin.konan.test.blackbox.support.compilation.TestCompilationResult.Companion.assertSuccess
import org.jetbrains.kotlin.konan.test.blackbox.support.TestCompilerArgs
import org.jetbrains.kotlin.konan.test.blackbox.support.runner.TestExecutable
import org.junit.jupiter.api.Test

/**
 * Blackbox tests for the HaIR backend (`IrToHair`), enabled via `-Xbinary=enableHair=true`.
 *
 * Each test exercises one operation that `IrToHair` currently supports. HaIR is incomplete and
 * silently falls back to the regular backend for functions it cannot translate, so these tests only
 * assert that the produced program behaves correctly — they do not yet assert which path was taken.
 *
 * Each program is a single file compiled standalone with no test runner, keeping the compilation set
 * minimal so HaIR is exercised on the operation under test rather than on test-harness code.
 */
class HairCodegenTest : AbstractNativeSimpleTest() {
    private fun runHairTest(name: String, source: String) {
        val file = buildDir.resolve("$name.kt")
        file.parentFile.mkdirs()
        file.writeText(source.trimIndent())

        val testCase = generateTestCaseWithSingleFile(
            sourceFile = file,
            freeCompilerArgs = TestCompilerArgs("-Xbinary=enableHair=true"),
            testKind = TestKind.STANDALONE_NO_TR,
            extras = TestCase.NoTestRunnerExtras(),
        )

        val compilationResult = compileToExecutableInOneStage(testCase).assertSuccess()
        runExecutableAndVerify(testCase, TestExecutable.fromCompilationResult(testCase, compilationResult))
    }

    @Test
    fun intConstant() = runHairTest(
        "intConstant", """
            fun answer(): Int = 42
            fun main() { if (answer() != 42) error("intConstant") }
        """
    )

    @Test
    fun longConstant() = runHairTest(
        "longConstant", """
            fun answer(): Long = 9000000000L
            fun main() { if (answer() != 9000000000L) error("longConstant") }
        """
    )

    @Test
    fun floatConstant() = runHairTest(
        "floatConstant", """
            fun answer(): Float = 1.5f
            fun main() { if (answer() != 1.5f) error("floatConstant") }
        """
    )

    @Test
    fun doubleConstant() = runHairTest(
        "doubleConstant", """
            fun answer(): Double = 2.5
            fun main() { if (answer() != 2.5) error("doubleConstant") }
        """
    )

    @Test
    fun booleanConstant() = runHairTest(
        "booleanConstant", """
            fun yes(): Boolean = true
            fun no(): Boolean = false
            fun main() { if (!yes() || no()) error("booleanConstant") }
        """
    )

    @Test
    fun charConstant() = runHairTest(
        "charConstant", """
            fun letter(): Char = 'K'
            fun main() { if (letter() != 'K') error("charConstant") }
        """
    )

    @Test
    fun nullConstant() = runHairTest(
        "nullConstant", """
            fun nothing(): Any? = null
            fun main() { if (nothing() != null) error("nullConstant") }
        """
    )

    @Test
    fun localVariables() = runHairTest(
        "localVariables", """
            fun compute(x: Int): Int {
                var acc = x
                acc = acc + 1
                val doubled = acc + acc
                return doubled
            }
            fun main() { if (compute(10) != 22) error("localVariables") }
        """
    )

    @Test
    fun addition() = runHairTest(
        "addition", """
            fun add(a: Int, b: Int): Int = a + b
            fun main() { if (add(2, 3) != 5) error("addition") }
        """
    )

    @Test
    fun subtraction() = runHairTest(
        "subtraction", """
            fun sub(a: Int, b: Int): Int = a - b
            fun main() { if (sub(10, 3) != 7) error("subtraction") }
        """
    )

    @Test
    fun multiplication() = runHairTest(
        "multiplication", """
            fun mul(a: Int, b: Int): Int = a * b
            fun main() { if (mul(6, 7) != 42) error("multiplication") }
        """
    )

    @Test
    fun bitwiseAnd() = runHairTest(
        "bitwiseAnd", """
            fun and(a: Int, b: Int): Int = a and b
            fun main() { if (and(0b1100, 0b1010) != 0b1000) error("bitwiseAnd") }
        """
    )

    @Test
    fun bitwiseOr() = runHairTest(
        "bitwiseOr", """
            fun or(a: Int, b: Int): Int = a or b
            fun main() { if (or(0b1100, 0b1010) != 0b1110) error("bitwiseOr") }
        """
    )

    @Test
    fun bitwiseXor() = runHairTest(
        "bitwiseXor", """
            fun xor(a: Int, b: Int): Int = a xor b
            fun main() { if (xor(0b1100, 0b1010) != 0b0110) error("bitwiseXor") }
        """
    )

    @Test
    fun incrementDecrement() = runHairTest(
        "incrementDecrement", """
            fun roundTrip(x: Int): Int {
                var y = x
                y++
                y++
                y--
                return y
            }
            fun main() { if (roundTrip(5) != 6) error("incrementDecrement") }
        """
    )

    @Test
    fun integerComparisons() = runHairTest(
        "integerComparisons", """
            fun lt(a: Int, b: Int): Boolean = a < b
            fun le(a: Int, b: Int): Boolean = a <= b
            fun gt(a: Int, b: Int): Boolean = a > b
            fun ge(a: Int, b: Int): Boolean = a >= b
            fun main() {
                if (!lt(1, 2) || lt(2, 1)) error("lt")
                if (!le(2, 2) || le(3, 2)) error("le")
                if (!gt(2, 1) || gt(1, 2)) error("gt")
                if (!ge(2, 2) || ge(1, 2)) error("ge")
            }
        """
    )

    @Test
    fun charComparison() = runHairTest(
        "charComparison", """
            fun before(a: Char, b: Char): Boolean = a < b
            fun main() { if (!before('a', 'b') || before('b', 'a')) error("charComparison") }
        """
    )

    @Test
    fun referenceEquality() = runHairTest(
        "referenceEquality", """
            class Marker
            fun same(a: Any, b: Any): Boolean = a === b
            fun main() {
                val m = Marker()
                if (!same(m, m) || same(m, Marker())) error("referenceEquality")
            }
        """
    )

    @Test
    fun booleanNot() = runHairTest(
        "booleanNot", """
            fun negate(b: Boolean): Boolean = !b
            fun main() { if (negate(true) || !negate(false)) error("booleanNot") }
        """
    )

    @Test
    fun integerConversions() = runHairTest(
        "integerConversions", """
            fun widen(x: Int): Long = x.toLong()
            fun narrow(x: Long): Int = x.toInt()
            fun truncateToByte(x: Int): Byte = x.toByte()
            fun charToInt(c: Char): Int = c.code
            fun main() {
                if (widen(123) != 123L) error("widen")
                if (narrow(9000000000L) != 410065408) error("narrow")
                if (truncateToByte(258).toInt() != 2) error("truncateToByte")
                if (charToInt('A') != 65) error("charToInt")
            }
        """
    )

    @Test
    fun ifElse() = runHairTest(
        "ifElse", """
            fun pick(cond: Boolean): Int = if (cond) 1 else 2
            fun main() {
                if (pick(true) != 1) error("ifThen")
                if (pick(false) != 2) error("ifElse")
            }
        """
    )

    @Test
    fun whenMultipleBranches() = runHairTest(
        "whenMultipleBranches", """
            fun classify(x: Int): Int = when {
                x < 0 -> -1
                x > 0 -> 1
                else -> 0
            }
            fun main() {
                if (classify(-5) != -1) error("negative")
                if (classify(0) != 0) error("zero")
                if (classify(7) != 1) error("positive")
            }
        """
    )

    @Test
    fun whileLoop() = runHairTest(
        "whileLoop", """
            fun sumTo(n: Int): Int {
                var i = 1
                var acc = 0
                while (i <= n) {
                    acc = acc + i
                    i = i + 1
                }
                return acc
            }
            fun main() { if (sumTo(5) != 15) error("whileLoop") }
        """
    )

    @Test
    fun doWhileLoop() = runHairTest(
        "doWhileLoop", """
            fun countDown(n: Int): Int {
                var i = n
                var steps = 0
                do {
                    steps = steps + 1
                    i = i - 1
                } while (i > 0)
                return steps
            }
            fun main() { if (countDown(4) != 4) error("doWhileLoop") }
        """
    )

    @Test
    fun breakStatement() = runHairTest(
        "breakStatement", """
            fun countUntil(limit: Int): Int {
                var i = 0
                while (true) {
                    if (i >= limit) break
                    i = i + 1
                }
                return i
            }
            fun main() { if (countUntil(3) != 3) error("breakStatement") }
        """
    )

    @Test
    fun continueStatement() = runHairTest(
        "continueStatement", """
            fun sumFrom(n: Int, threshold: Int): Int {
                var i = 0
                var acc = 0
                while (i < n) {
                    i = i + 1
                    if (i < threshold) continue
                    acc = acc + i
                }
                return acc
            }
            fun main() { if (sumFrom(5, 3) != 12) error("continueStatement") }
        """
    )

    @Test
    fun earlyReturn() = runHairTest(
        "earlyReturn", """
            fun clamp(x: Int): Int {
                if (x < 0) return 0
                if (x > 10) return 10
                return x
            }
            fun main() {
                if (clamp(-3) != 0) error("low")
                if (clamp(5) != 5) error("mid")
                if (clamp(99) != 10) error("high")
            }
        """
    )

    @Test
    fun isInstance() = runHairTest(
        "isInstance", """
            open class Base
            class Derived : Base()
            fun check(x: Base): Boolean = x is Derived
            fun main() {
                if (!check(Derived())) error("positive")
                if (check(Base())) error("negative")
            }
        """
    )

    @Test
    fun notIsInstance() = runHairTest(
        "notIsInstance", """
            open class Base
            class Derived : Base()
            fun check(x: Base): Boolean = x !is Derived
            fun main() {
                if (check(Derived())) error("positive")
                if (!check(Base())) error("negative")
            }
        """
    )

    @Test
    fun asCast() = runHairTest(
        "asCast", """
            open class Base
            class Derived : Base()
            fun downcast(x: Base): Derived = x as Derived
            fun main() {
                val d = Derived()
                if (downcast(d) !== d) error("asCast")
            }
        """
    )

    @Test
    fun instanceFields() = runHairTest(
        "instanceFields", """
            class Box(var value: Int)
            fun roundTrip(): Int {
                val b = Box(10)
                b.value = 42
                return b.value
            }
            fun main() { if (roundTrip() != 42) error("instanceFields") }
        """
    )

    @Test
    fun topLevelGlobal() = runHairTest(
        "topLevelGlobal", """
            var counter: Int = 0
            fun bump() { counter = counter + 1 }
            fun read(): Int = counter
            fun main() {
                bump()
                bump()
                bump()
                if (read() != 3) error("topLevelGlobal")
            }
        """
    )

    @Test
    fun staticCall() = runHairTest(
        "staticCall", """
            fun helper(x: Int): Int = x * 2
            fun caller(x: Int): Int = helper(x) + 1
            fun main() { if (caller(5) != 11) error("staticCall") }
        """
    )
}
