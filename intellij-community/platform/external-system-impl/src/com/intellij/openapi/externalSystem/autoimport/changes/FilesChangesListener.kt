// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport.changes

import com.intellij.openapi.externalSystem.autoimport.ProjectStatus.ModificationType
import java.util.*

interface FilesChangesListener : EventListener {
  fun init()

  fun onFileChange(path: String, modificationStamp: Long, modificationType: ModificationType)

  fun apply()
}