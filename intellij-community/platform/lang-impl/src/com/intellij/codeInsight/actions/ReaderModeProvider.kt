// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.actions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project

/**
 * Use it to provide an activity that should be applied for editor in Read Mode
 */
interface ReaderModeProvider {
  /**
   * It's triggered on Reader Mode turning on or turning off.
   *
   * If {@param fileIsOpenAlready} is true then provider should apply changes only for already opened files,
   * otherwise, if it's false, it should apply changes for every opening file
   */
  fun applyModeChanged(project: Project, editor: Editor, readerMode: Boolean, fileIsOpenAlready: Boolean) {}
}