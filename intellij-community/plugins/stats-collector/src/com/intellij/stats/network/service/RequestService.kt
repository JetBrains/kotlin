// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.network.service

import java.io.File

abstract class RequestService {
  abstract fun postZipped(url: String, file: File): ResponseData?
  abstract fun get(url: String): ResponseData?
}