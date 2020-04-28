// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ThreeState

interface SilentChangeVetoer {
  companion object {
    @JvmField
    val EP_NAME = ExtensionPointName.create<SilentChangeVetoer>("com.intellij.silentChangeVetoer")
  }

  fun canChangeFileSilently(project: Project, virtualFile: VirtualFile): ThreeState = ThreeState.UNSURE
}