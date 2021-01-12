// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.bookmarks.actions

import com.intellij.ide.bookmarks.BookmarkManager
import com.intellij.ide.bookmarks.actions.ToggleBookmarkAction.getBookmarkInfo
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import javax.swing.JComponent

class EditBookmarkAction : DumbAwareAction() {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val bookmark = getBookmarkInfo(e)?.bookmarkAtPlace ?: return
    val contextComponent = e.getData(PlatformDataKeys.CONTEXT_COMPONENT) as? JComponent ?: return
    BookmarkManager.getInstance(project).editDescription(bookmark, contextComponent)
  }

  override fun update(e: AnActionEvent) {
    val info = getBookmarkInfo(e)
    e.presentation.isEnabledAndVisible = info != null && info.bookmarkAtPlace != null
  }
}
