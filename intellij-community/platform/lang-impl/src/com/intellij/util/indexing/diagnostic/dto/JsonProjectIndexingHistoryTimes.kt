// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

@Suppress("unused", "used for JSON")
data class JsonProjectIndexingHistoryTimes(
  val indexingTime: JsonDuration,
  val scanFilesTime: JsonDuration,
  val pushPropertiesTime: JsonDuration,
  val indexExtensionsTime: JsonDuration,

  val pushPropertiesStart: JsonDateTime,
  val pushPropertiesEnd: JsonDateTime,
  val scanFilesStart: JsonDateTime,
  val scanFilesEnd: JsonDateTime,
  val indexExtensionsStart: JsonDateTime,
  val indexExtensionsEnd: JsonDateTime,
  val indexingStart: JsonDateTime,
  val indexingEnd: JsonDateTime
)