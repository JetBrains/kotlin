// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.autoimport

import com.intellij.openapi.editor.toolbar.floating.AbstractFloatingToolbarProvider

class ProjectReloadFloatingProvider : AbstractFloatingToolbarProvider(ACTION_GROUP) {

  override val priority = 100

  override val autoHideable = false

  companion object {
    const val ACTION_GROUP = "ProjectReloadActionGroup"
  }
}