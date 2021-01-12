// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize

@JsonSerialize(using = JsonPercentages.Companion::class)
data class JsonPercentages(val percentages: Double) {
  companion object : JsonSerializer<JsonPercentages>() {
    override fun serialize(value: JsonPercentages, gen: JsonGenerator, serializers: SerializerProvider?) {
      gen.writeString(value.presentablePercentages())
    }
  }

  fun presentablePercentages(): String =
    if (percentages < 0.01) {
      "< 1%"
    }
    else {
      "${String.format("%.1f", percentages * 100)}%"
    }
}