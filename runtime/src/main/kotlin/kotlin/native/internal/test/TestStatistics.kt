/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal.test

internal interface TestStatistics {
    val total: Int
    val passed: Int
    val failed: Int
    val ignored: Int

    val totalSuites: Int

    val failedTests: Collection<TestCase>
    val hasFailedTests: Boolean
}

internal class MutableTestStatistics: TestStatistics {

    override var total:   Int = 0; private set
    override var passed:  Int = 0; private set
    override var ignored: Int = 0; private set

    override var totalSuites: Int = 0; private set

    override val failed: Int
        get() = failedTests_.size

    override val hasFailedTests: Boolean
        get() = failedTests_.isNotEmpty()

    private val failedTests_ = mutableListOf<TestCase>()
    override val failedTests: Collection<TestCase>
        get() = failedTests_

    fun registerSuite() { totalSuites++ }

    fun registerPass() { total++; passed++ }
    fun registerFail(testCase: TestCase) { total++; failedTests_.add(testCase) }
    fun registerIgnore() { total++; ignored++ }

    fun reset() {
        total = 0
        passed = 0
        ignored = 0
        totalSuites = 0
        failedTests_.clear()
    }
}
