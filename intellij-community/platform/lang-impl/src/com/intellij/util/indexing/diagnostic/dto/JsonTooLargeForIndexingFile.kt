// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

data class JsonTooLargeForIndexingFile(
  val fileName: String,
  val fileSize: JsonFileSize
)