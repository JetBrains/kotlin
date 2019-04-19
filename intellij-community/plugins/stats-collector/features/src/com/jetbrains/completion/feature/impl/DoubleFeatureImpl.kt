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

package com.jetbrains.completion.feature.impl

import com.jetbrains.completion.feature.DoubleFeature

class DoubleFeatureImpl(override val name: String,
                        override val index: Int,
                        override val undefinedIndex: Int,
                        override val defaultValue: Double) : DoubleFeature {
    private companion object {
        private val MAX_VALUE = Math.pow(10.0, 10.0)
    }

    override fun process(value: Any, featureArray: DoubleArray) {
        featureArray[undefinedIndex] = 0.0
        featureArray[index] = Math.min(value.asDouble(), MAX_VALUE)
    }

    private fun Any.asDouble(): Double {
        if (this is Number) return this.toDouble()
        return this.toString().toDouble()
    }

    override fun setDefaults(featureArray: DoubleArray) {
        featureArray[undefinedIndex] = 1.0
        featureArray[index] = defaultValue
    }
}