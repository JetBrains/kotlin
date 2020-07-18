/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.test

import kotlin.math.abs

internal fun messagePrefix(message: String?) = if (message == null) "" else "$message. "
internal expect fun lookupAsserter(): Asserter

@PublishedApi // required to get stable name as it's called from box tests
internal fun overrideAsserter(value: Asserter?): Asserter? = _asserter.also { _asserter = value }


private fun checkAbsoluteTolerance(absoluteTolerance: Double) {
    require(absoluteTolerance >= 0.0) { "Illegal negative absolute tolerance <$absoluteTolerance>." }
    require(!absoluteTolerance.isNaN()) { "Illegal NaN absolute tolerance <$absoluteTolerance>." }
}

internal fun checkDoublesAreEqual(
    expected: Double,
    actual: Double,
    absoluteTolerance: Double,
    message: String?,
    shouldFail: Boolean = false
) {
    checkAbsoluteTolerance(absoluteTolerance)
    val equal = expected.toBits() == actual.toBits() || abs(expected - actual) <= absoluteTolerance

    asserter.assertTrue(
        { messagePrefix(message) + "Expected <$expected> with absolute tolerance <$absoluteTolerance>, actual <$actual>." },
        equal != shouldFail
    )
}

internal fun checkFloatsAreEqual(
    expected: Float,
    actual: Float,
    absoluteTolerance: Float,
    message: String?,
    shouldFail: Boolean = false
) {
    checkAbsoluteTolerance(absoluteTolerance.toDouble())
    val equal = expected.toBits() == actual.toBits() || abs(expected - actual) <= absoluteTolerance

    asserter.assertTrue(
        { messagePrefix(message) + "Expected <$expected> with absolute tolerance <$absoluteTolerance>, actual <$actual>." },
        equal != shouldFail
    )
}