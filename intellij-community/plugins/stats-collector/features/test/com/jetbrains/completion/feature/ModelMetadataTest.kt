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

import com.jetbrains.completion.feature.impl.FeatureUtils
import org.junit.Assert.*
import org.junit.Test

/**
 * @author Vitaliy.Bibaev
 */
abstract class ModelMetadataTest {
    @Test
    fun `validate feature order`() {
        val metadata = modelMetadata()
        val featureIndex: Map<String, Feature> = mutableMapOf<String, Feature>().apply {
            metadata.binary.forEach { put(it.name, it) }
            metadata.float.forEach { put(it.name, it) }
            metadata.categorical.forEach { put(it.name, it) }
        }

        for ((name, order) in metadata.featuresOrder) {
            val split = name.split('=')
            if (split.size > 3) fail("line '$name' in feature order contains more than 1 symbol '='")
            if (split.size == 2) {
                val (featureName, value) = split
                val feature = featureIndex[featureName]
                        ?: throw AssertionError("feature declared in the featureOrder not found: $featureName")
                when (feature) {
                    is BinaryFeature -> {
                        assertEquals(FeatureUtils.UNDEFINED, value)
                        assertEquals(order, feature.undefinedIndex)
                    }
                    is DoubleFeature -> {
                        assertEquals(FeatureUtils.UNDEFINED, value)
                        assertEquals(order, feature.undefinedIndex)
                    }
                    is CategoricalFeature -> {
                        when (value) {
                            FeatureUtils.UNDEFINED -> assertEquals(order, feature.undefinedIndex)
                            FeatureUtils.OTHER -> assertEquals(order, feature.otherCategoryIndex)
                            else -> assertEquals(order, feature.indexByCategory(value))
                        }
                    }
                    else -> fail("unknown feature type found: ${feature.javaClass.canonicalName}")
                }
            } else {
                val featureName = split.single()
                val feature = featureIndex[featureName]
                        ?: throw AssertionError("feature declared in the featureOrder not found: $featureName")
                when (feature) {
                    is BinaryFeature -> assertEquals(order, feature.index)
                    is DoubleFeature -> assertEquals(order, feature.index)
                    is CategoricalFeature -> fail("categorical feature claims 'name=category' in feature order list")
                    else -> fail("unknown feature type found: ${feature.javaClass.canonicalName}")
                }
            }
        }
    }

    @Test
    fun `test all features have distinct names`() {
        val metadata = modelMetadata()
        val featuresByName = mutableListOf<Feature>().apply {
            addAll(metadata.binary)
            addAll(metadata.float)
            addAll(metadata.categorical)
        }.groupBy { it.name }

        var failed = false
        for ((name, features) in featuresByName) {
            if (features.size > 1) {
                println("${features.size} features with name '$name found'")
                failed = true
            }
        }

        assertFalse("features with the same name found (see output above)", failed)

    }

    @Test
    fun `test features fill features array properly`() {
        val metadata = modelMetadata()
        val revertedFeatureOrder = mutableMapOf<Int, Feature>()
        fun MutableMap<Int, Feature>.checkAndPut(index: Int?, feature: Feature) {
            if (index != null) {
              assertFeaturesNotStoreValueBySameIndex(index, put(index, feature), feature)
            }
        }
        for (feature in metadata.binary) {
            revertedFeatureOrder.checkAndPut(feature.index, feature)
            revertedFeatureOrder.checkAndPut(feature.undefinedIndex, feature)
        }

        for (feature in metadata.float) {
            revertedFeatureOrder.checkAndPut(feature.index, feature)
            revertedFeatureOrder.checkAndPut(feature.undefinedIndex, feature)
        }

        for (feature in metadata.categorical) {
            feature.categories.forEach {
                revertedFeatureOrder.checkAndPut(feature.indexByCategory(it), feature)
            }
        }

        assertEquals("features should totally cover the features array", metadata.featuresOrder.size, revertedFeatureOrder.size)
        assertEquals(0, revertedFeatureOrder.keys.min())
        assertEquals(metadata.featuresOrder.size - 1, revertedFeatureOrder.keys.max())
    }

    @Test
    fun `extract model version`() {
        val meta = modelMetadata()
        assertNotNull(meta.version)
    }

    private fun assertFeaturesNotStoreValueBySameIndex(index: Int, old: Feature?, new: Feature) {
        if (old != null) {
            fail("features store values to the same array element [index = $index, names = {${old.name}, ${new.name}}]")
        }
    }

    protected abstract fun modelMetadata(): ModelMetadataEx
}