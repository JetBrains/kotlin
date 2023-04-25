/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal.test

import kotlin.experimental.ExperimentalNativeApi

@ExperimentalNativeApi
internal interface TestListener {
    fun startTesting(runner: TestRunner)
    fun finishTesting(runner: TestRunner, timeMillis: Long)

    fun startIteration(runner: TestRunner, iteration: Int, suites: Collection<TestSuite>)
    fun finishIteration(runner: TestRunner, iteration: Int, timeMillis: Long)

    fun startSuite(suite: TestSuite)
    fun finishSuite(suite: TestSuite, timeMillis: Long)
    fun ignoreSuite(suite: TestSuite)

    fun start(testCase: TestCase)
    fun pass(testCase: TestCase, timeMillis: Long)
    fun fail(testCase: TestCase, e: Throwable, timeMillis: Long)
    fun ignore(testCase: TestCase)
}

@ExperimentalNativeApi
internal open class BaseTestListener: TestListener {
    override fun startTesting(runner: TestRunner) {}
    override fun finishTesting(runner: TestRunner, timeMillis: Long) {}
    override fun startIteration(runner: TestRunner, iteration: Int, suites: Collection<TestSuite>) {}
    override fun finishIteration(runner: TestRunner, iteration: Int, timeMillis: Long) {}
    override fun startSuite(suite: TestSuite) {}
    override fun finishSuite(suite: TestSuite, timeMillis: Long) {}
    override fun ignoreSuite(suite: TestSuite) {}
    override fun start(testCase: TestCase) {}
    override fun pass(testCase: TestCase, timeMillis: Long) {}
    override fun fail(testCase: TestCase, e: Throwable, timeMillis: Long) {}
    override fun ignore(testCase: TestCase) {}
}
