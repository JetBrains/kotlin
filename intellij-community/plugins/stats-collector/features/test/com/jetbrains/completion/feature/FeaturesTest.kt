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

package com.jetbrains.completion.feature

import com.jetbrains.completion.feature.impl.BinaryFeatureImpl
import com.jetbrains.completion.feature.impl.CategoricalFeatureImpl
import com.jetbrains.completion.feature.impl.DoubleFeatureImpl
import org.junit.Assert
import org.junit.Test

/**
 * @author Vitaliy.Bibaev
 */
class FeaturesTest {
    private companion object {
        val BINARY_FEATURE = BinaryFeatureImpl("testBinaryFeature", 0, 2, -1.0,
                BinaryFeature.BinaryValueDescriptor("no_true", 0.0),
                BinaryFeature.BinaryValueDescriptor("true", 1.0))

        val DOUBLE_FEATURE = DoubleFeatureImpl("testDoubleFeature", 2, 1, -100.0)

        val CATEGORICAL_FEATURE = CategoricalFeatureImpl("testCategoryFeature", 0, 1,
                                                         mapOf("BASIC" to 2, "SMART" to 3, "CLASS" to 5))
    }

    @Test
    fun `test binary feature processing if value is present`() {
        val featureArray = DoubleArray(3, { Double.MIN_VALUE })
        BINARY_FEATURE.process("no_true", featureArray)
        Assert.assertArrayEquals(doubleArrayOf(0.0, Double.MIN_VALUE, 0.0), featureArray, 1e-10)
        BINARY_FEATURE.process("true", featureArray)
        Assert.assertArrayEquals(doubleArrayOf(1.0, Double.MIN_VALUE, 0.0), featureArray, 1e-10)
    }

    @Test
    fun `test binary feature processing if value is absent`() {
        val featureArray = DoubleArray(3, { Double.MIN_VALUE })
        BINARY_FEATURE.setDefaults(featureArray)
        Assert.assertArrayEquals(doubleArrayOf(-1.0, Double.MIN_VALUE, 1.0), featureArray, 1e-10)
    }

    @Test
    fun `test double feature processing if value is present`() {
        val featureArray = DoubleArray(3, { Double.MIN_VALUE })
        DOUBLE_FEATURE.process(10, featureArray)
        Assert.assertArrayEquals(doubleArrayOf(Double.MIN_VALUE, 0.0, 10.0), featureArray, 1e-10)
        DOUBLE_FEATURE.process(30L, featureArray)
        Assert.assertArrayEquals(doubleArrayOf(Double.MIN_VALUE, 0.0, 30.0), featureArray, 1e-10)
    }

    @Test
    fun `test double feature processing if value is absent`() {
        val featureArray = DoubleArray(3, { Double.MIN_VALUE })
        DOUBLE_FEATURE.setDefaults(featureArray)
        Assert.assertArrayEquals(doubleArrayOf(Double.MIN_VALUE, 1.0, -100.0), featureArray, 1e-10)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `test double feature should throw exception if value cannot be interpreted as double`() {
        val featureArray = DoubleArray(3, { Double.MIN_VALUE })
        DOUBLE_FEATURE.process("NOT A NUMBER", featureArray)
    }

    @Test
    fun `test double feature should accept value as a string`() {
        val featureArray = DoubleArray(3, { Double.MIN_VALUE })
        DOUBLE_FEATURE.process("100", featureArray)
        Assert.assertArrayEquals(doubleArrayOf(Double.MIN_VALUE, 0.0, 100.0), featureArray, 1e-10)
    }

    @Test
    fun `test categorical feature`() {
        val featureArray = DoubleArray(6, { Double.MIN_VALUE })
        CATEGORICAL_FEATURE.setDefaults(featureArray)
        Assert.assertArrayEquals(doubleArrayOf(1.0, 0.0, 0.0, 0.0, Double.MIN_VALUE, 0.0), featureArray, 1e-10)

        CATEGORICAL_FEATURE.process("OTHER", featureArray)
        Assert.assertArrayEquals(doubleArrayOf(0.0, 1.0, 0.0, 0.0, Double.MIN_VALUE, 0.0), featureArray, 1e-10)

        CATEGORICAL_FEATURE.process("BASIC", featureArray)
        Assert.assertArrayEquals(doubleArrayOf(0.0, 0.0, 1.0, 0.0, Double.MIN_VALUE, 0.0), featureArray, 1e-10)

        CATEGORICAL_FEATURE.process("SMART", featureArray)
        Assert.assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE, 0.0), featureArray, 1e-10)

        CATEGORICAL_FEATURE.process("CLASS", featureArray)
        Assert.assertArrayEquals(doubleArrayOf(0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE, 1.0), featureArray, 1e-10)
    }
}