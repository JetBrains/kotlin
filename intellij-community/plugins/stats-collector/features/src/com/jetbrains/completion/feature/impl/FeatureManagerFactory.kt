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

import com.jetbrains.completion.feature.*

class FeatureManagerFactory: FeatureManager.Factory {
    override fun createFeatureManager(reader: FeatureReader, interpreter: FeatureInterpreter): FeatureManager {
        val order = FeatureReader.featuresOrder()

        val binaryFactors = FeatureReader.binaryFactors()
                .map { (name, description) -> interpreter.binary(name, description, order) }
        val doubleFactors = FeatureReader.doubleFactors()
                .map { (name, defaultValue) -> interpreter.double(name, defaultValue, order) }
        val categoricalFactors = FeatureReader.categoricalFactors()
                .map { (name, categories) -> interpreter.categorical(name, categories, order) }

        val completionFactors = FeatureReader.completionFactors()

        return MyFeatureManager(binaryFactors, doubleFactors, categoricalFactors, completionFactors, order)
    }

    private class MyFeatureManager(override val binaryFactors: List<BinaryFeature>,
                                   override val doubleFactors: List<DoubleFeature>,
                                   override val categoricalFactors: List<CategoricalFeature>,
                                   override val completionFactors: CompletionFactors,
                                   override val featureOrder: Map<String, Int>) : FeatureManager {
        override fun isUserFeature(name: String): Boolean = false

        override fun allFeatures(): List<Feature> = ArrayList<Feature>().apply {
            addAll(binaryFactors)
            addAll(doubleFactors)
            addAll(categoricalFactors)
        }

        override fun createTransformer(): Transformer {
            val features = allFeatures().associate { it.name to it }
            return FeatureTransformer(features, featureOrder.size)
        }
    }
}