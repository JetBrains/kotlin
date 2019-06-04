// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.jetbrains.completion.feature.impl

class MetadataReader(private val featuresDirectory: String) {

  fun binaryFeatures(): String = resourceContent("binary.json")
  fun floatFeatures(): String = resourceContent("float.json")
  fun categoricalFeatures(): String = resourceContent("categorical.json")
  fun allKnown(): String = resourceContent("all_features.json")
  fun featuresOrder(): Map<String, Int> = resourceContent("features_order.txt").lineToNumber()

  fun extractVersion(): String? {
    val resource = MetadataReader::class.java.classLoader.getResource("$featuresDirectory/binary.json")
    if (resource == null) return null
    val result = resource.file.substringBeforeLast(".jar!", "").substringAfterLast("-", "")
    return if (result.isBlank()) null else result
  }

  private fun resourceContent(fileName: String): String {
    val fileStream = MetadataReader::class.java.classLoader.getResourceAsStream("$featuresDirectory/$fileName")
    return fileStream.bufferedReader().readText()
  }

  private fun String.lineToNumber(): Map<String, Int> {
    var index = 0
    val map = mutableMapOf<String, Int>()
    split("\n").forEach {
      val featureName = it.trim()
      map[featureName] = index++
    }

    return map
  }
}
