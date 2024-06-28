/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal.test

import kotlin.experimental.ExperimentalNativeApi

@ExperimentalNativeApi
internal interface TestStatistics {
    val total: Int
    val passed: Int
    val failed: Int
    val ignored: Int

    val totalSuites: Int

    val failedTests: Collection<TestCase>
    val hasFailedTests: Boolean
}

@ExperimentalNativeApi
internal class MutableTestStatistics: TestStatistics {

    override var total:   Int = 0; private set
    override var passed:  Int = 0; private set

    override val ignored: Int
        get() = _ignoredTests.size

    override var totalSuites: Int = 0; private set

    override val failed: Int
        get() = _failedTests.size

    override val hasFailedTests: Boolean
        get() = _failedTests.isNotEmpty()

    private val _failedTests = mutableListOf<TestCase>()
    override val failedTests: Collection<TestCase>
        get() = _failedTests

    private val _ignoredTests = mutableListOf<TestCase>()
    val ignoredTests: Collection<TestCase>
        get() = _ignoredTests

    fun registerSuite(count: Int = 1) {
        require(count >= 0)
        totalSuites += count
    }

    fun registerPass(count: Int = 1) {
        require(count >= 0)
        total += count
        passed += count
    }

    fun registerFail(testCases: Collection<TestCase>) {
        total += testCases.size
        _failedTests.addAll(testCases)
    }

    fun registerFail(testCase: TestCase) = registerFail(listOf(testCase))

    fun registerIgnore(testCases: Collection<TestCase>) {
        total += testCases.size
        _ignoredTests.addAll(testCases)
    }

    fun registerIgnore(testCase: TestCase) = registerIgnore(listOf(testCase))

    fun registerIgnore(suite: TestSuite) {
        registerIgnore(suite.testCases.values)
    }

    fun reset() {
        total = 0
        passed = 0
        totalSuites = 0
        _failedTests.clear()
        _ignoredTests.clear()
    }
}
