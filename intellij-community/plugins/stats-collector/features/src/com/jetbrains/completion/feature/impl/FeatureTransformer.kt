/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

import com.jetbrains.completion.feature.Feature
import com.jetbrains.completion.feature.Transformer


/**
 * @author Vitaliy.Bibaev
 */
class FeatureTransformer(private val features: Map<String, Feature>,
                         arraySize: Int)
    : Transformer {
    private val array = DoubleArray(arraySize)
    override fun featureArray(relevanceMap: Map<String, Any>, userFactors: Map<String, Any?>): DoubleArray {
        for ((name, feature) in features) {
            val value = relevanceMap[name]
            if (value == null) {
                feature.setDefaults(array)
            } else {
                feature.process(value, array)
            }
        }

        return array
    }
}