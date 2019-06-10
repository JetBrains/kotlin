// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.feature.impl

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jetbrains.completion.feature.*

class FeaturesInfo(private val knownFeatures: Set<String>,
                   override val binary: List<BinaryFeature>,
                   override val float: List<DoubleFeature>,
                   override val categorical: List<CategoricalFeature>,
                   override val featuresOrder: Map<String, Int>,
                   override val version: String?) : ModelMetadataEx {

  companion object {
    private val gson = Gson()

    fun readFromResources(resourceDirectory: String) = buildInfo(MetadataReader(resourceDirectory), FeatureInterpreterImpl())

    private fun buildInfo(reader: MetadataReader, interpreter: FeatureInterpreter): FeaturesInfo {
      val order = reader.featuresOrder()

      val knownFeatures = reader.allKnown().fromJson<List<String>>().toSet()

      val binaryFactors = reader.binaryFeatures().fromJson<Map<String, Map<String, Any>>>()
        .map { (name, description) -> interpreter.binary(name, description, order) }
      val doubleFactors = reader.floatFeatures().fromJson<Map<String, Map<String, Any>>>()
        .map { (name, defaultValue) -> interpreter.double(name, defaultValue, order) }
      val categoricalFactors = reader.categoricalFeatures().fromJson<Map<String, List<String>>>()
        .map { (name, categories) -> interpreter.categorical(name, categories, order) }

      return FeaturesInfo(knownFeatures, binaryFactors, doubleFactors, categoricalFactors, order, reader.extractVersion())
    }

    private fun <T> String.fromJson(): T {
      val typeToken = object : TypeToken<T>() {}
      return gson.fromJson<T>(this, typeToken.type)
    }
  }

  private val allFeatures: Map<String, Feature> = mutableListOf<Feature>()
    .apply {
      addAll(binary)
      addAll(float)
      addAll(categorical)
    }.associate { it.name to it }

  override fun createTransformer(): FeatureTransformer = FeatureTransformer(allFeatures, featuresOrder.size)

  override fun unknownFeatures(features: Set<String>): List<String> {
    var result: MutableList<String>? = null
    for (featureName in features) {
      if (featureName !in knownFeatures) {
        result = (result ?: mutableListOf()).apply { add(featureName) }
      }
    }

    return result ?: emptyList()
  }
}