// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.intellij.openapi.util.text.StringUtil
import java.util.concurrent.TimeUnit

@JsonSerialize(using = JsonDuration.Companion::class)
data class JsonDuration(val nano: Long) {
  companion object : JsonSerializer<JsonDuration>() {
    override fun serialize(value: JsonDuration, gen: JsonGenerator, serializers: SerializerProvider?) {
      gen.writeString(value.presentableDuration())
    }
  }

  fun presentableDuration(): String =
    if (nano < TimeUnit.MILLISECONDS.toNanos(1)) {
      "< 1 ms"
    }
    else {
      StringUtil.formatDuration(nano.toMillis())
    }
}