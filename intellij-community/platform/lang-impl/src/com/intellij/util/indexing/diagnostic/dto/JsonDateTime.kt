// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@JsonSerialize(using = JsonDateTime.Companion::class)
data class JsonDateTime(val instant: Instant) {
  companion object : JsonSerializer<JsonDateTime>() {
    override fun serialize(value: JsonDateTime, gen: JsonGenerator, serializers: SerializerProvider?) {
      gen.writeString(value.presentableDateTime())
    }
  }

  fun presentableDateTime(): String =
    ZonedDateTime.ofInstant(instant, ZoneId.systemDefault()).format(DateTimeFormatter.RFC_1123_DATE_TIME)
}