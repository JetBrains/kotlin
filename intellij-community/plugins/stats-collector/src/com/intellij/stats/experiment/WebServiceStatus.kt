// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.stats.experiment

import com.intellij.openapi.components.service

interface WebServiceStatus {
  companion object {
    fun getInstance() = service<WebServiceStatus>()
  }

  fun isServerOk(): Boolean
  fun dataServerUrl(): String

  fun isExperimentOnCurrentIDE(): Boolean
  fun experimentVersion(): Int

  fun updateStatus()
}