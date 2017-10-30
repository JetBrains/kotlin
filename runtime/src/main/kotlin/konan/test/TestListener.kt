/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package konan.test

interface TestListener {
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

open class BaseTestListener: TestListener {
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
