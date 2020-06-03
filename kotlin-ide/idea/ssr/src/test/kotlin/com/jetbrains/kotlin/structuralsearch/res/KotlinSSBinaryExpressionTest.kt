package com.jetbrains.kotlin.structuralsearch.res

import com.jetbrains.kotlin.structuralsearch.KotlinSSResourceInspectionTest

class KotlinSSBinaryExpressionTest : KotlinSSResourceInspectionTest() {
    override fun getBasePath(): String = "binaryExpression"

    fun testBinaryExpression() { doTest("1 + 2 - 3") }

    fun testBinaryParExpression() { doTest("3 * (2 - 3)") }

    fun testTwoBinaryExpressions() { doTest("a = 1 \n b = 2") }

    fun testBinaryPlus() { doTest("1 + 2") }

    fun testBinaryMinus() { doTest("1 - 2") }

    fun testBinaryTimes() { doTest("1 * 2") }

    fun testBinaryDiv() { doTest("1 / 2") }

    fun testBinaryRem() { doTest("1 % 2") }

    fun testBinaryRangeTo() { doTest("1..2") }

    fun testBinaryIn() { doTest("1 in 0..2") }

    fun testBinaryNotIn() { doTest("1 !in 0..2") }

    fun testBinaryBigThan() { doTest("1 > 2") }

    fun testBinaryLessThan() { doTest("1 < 2") }

    fun testBinaryBigEqThan() { doTest("1 >= 2") }

    fun testBinaryLessEqThan() { doTest("1 <= 2") }

    fun testBinaryEquality() { doTest("a == b") }

    fun testBinaryInEquality() { doTest("a != b") }

    fun testElvis() { doTest("'_ ?: '_") }

    fun testBinaryPlusAssign() { doTest("1 += 2") }

    fun testBinaryMinusAssign() { doTest("1 -= 2") }

    fun testBinaryTimesAssign() { doTest("1 *= 2") }

    fun testBinaryDivAssign() { doTest("1 /= 2") }

    fun testBinaryRemAssign() { doTest("1 %= 2") }

    fun testBinarySet() { doTest("a[0] = 1 + 2") }
}