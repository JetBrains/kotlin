// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.diagnostic.dto

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.SystemInfo
import java.time.Instant

data class JsonIndexDiagnosticAppInfo(
  val build: String,
  val buildDate: JsonDateTime,
  val productCode: String,
  val generated: JsonDateTime,
  val os: String,
  val runtime: String
) {
  companion object {
    fun create(): JsonIndexDiagnosticAppInfo {
      val appInfo = ApplicationInfo.getInstance()
      return JsonIndexDiagnosticAppInfo(
        build = appInfo.build.asStringWithoutProductCode(),
        buildDate = JsonDateTime(appInfo.buildDate.toInstant()),
        productCode = appInfo.build.productCode,
        generated = JsonDateTime(Instant.now()),
        os = SystemInfo.getOsNameAndVersion(),
        runtime = SystemInfo.JAVA_VENDOR + " " + SystemInfo.JAVA_VERSION + " " + SystemInfo.JAVA_RUNTIME_VERSION
      )
    }
  }
}