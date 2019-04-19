/*
 * Copyright 2000-2018 JetBrains s.r.o.
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

package com.intellij.stats.personalization

import com.intellij.stats.personalization.impl.FactorsUtil
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase

/**
 * @author Vitaliy.Bibaev
 */
class StatisticsTest : UsefulTestCase() {
    fun testMergeAverageCommutative() {
        val val1 = FactorsUtil.mergeAverage(10, 50.0, 5, 25.0)
        val val2 = FactorsUtil.mergeAverage(5, 25.0, 10, 50.0)

        TestCase.assertEquals(val1, val2, 1e-7)
    }

    fun testMergeAverageWithZero() {
        val value = FactorsUtil.mergeAverage(0, 0.0, 10, 10.0)
        TestCase.assertEquals(10.0, value, 1e-7)
    }

    fun testMergeAverageWithSingle() {
        val value = FactorsUtil.mergeAverage(1, 60.0, 3, 0.0)
        TestCase.assertEquals(15.0, value, 1e-7)
    }

    fun testMergeAverageWithMany() {
        val value = FactorsUtil.mergeAverage(2, 10.0, 8, 5.0)
        TestCase.assertEquals(6.0, value, 1e-7)
    }

    fun testMergeVarianceCommutative() {
        val val1 = FactorsUtil.mergeVariance(10, 20.0, 10.0, 100, 10.0, 5.0)
        val val2 = FactorsUtil.mergeVariance(100, 10.0, 5.0, 10, 20.0, 10.0)

        TestCase.assertEquals(val1, val2, 1e-7)
    }

    fun testMergeVarianceWithZero() {
        val value = FactorsUtil.mergeVariance(10, 10.0, 100.0, 0, 0.0, 0.0)

        TestCase.assertEquals(10.0, value, 1e-7)
    }

    fun testMergeVarianceWithSingle() {
        // sample: [1, 3] merge with [2]
        val value = FactorsUtil.mergeVariance(2, 1.0, 2.0, 1, 0.0, 2.0)
        TestCase.assertEquals(2.0 / 3.0, value, 1e-7)
    }

    fun testMergeVarianceWithMany() {
        // sample: [1, 3] merge with [2, 4]
        val value = FactorsUtil.mergeVariance(2, 1.0, 2.0, 2, 1.0, 3.0)
        TestCase.assertEquals(1.25, value, 1e-7)
    }
}