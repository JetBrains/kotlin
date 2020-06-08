// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

@Suppress("unused", "used for JSON")
data class JsonProjectIndexingHistoryTimes(
  val indexingTime: JsonDuration,
  val scanFilesTime: JsonDuration,
  val pushPropertiesTime: JsonDuration,
  val indexExtensionsTime: JsonDuration,

  val indexingStart: PresentableTime,
  val indexingEnd: PresentableTime,
  val pushPropertiesStart: PresentableTime,
  val pushPropertiesEnd: PresentableTime,
  val indexExtensionsStart: PresentableTime,
  val indexExtensionsEnd: PresentableTime,
  val scanFilesStart: PresentableTime,
  val scanFilesEnd: PresentableTime
)