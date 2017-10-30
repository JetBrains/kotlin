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

interface TestStatistics {
    val total: Int
    val passed: Int
    val failed: Int
    val ignored: Int

    val totalSuites: Int

    val failedTests: Collection<TestCase>
    val hasFailedTests: Boolean
}

class MutableTestStatistics: TestStatistics {

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
