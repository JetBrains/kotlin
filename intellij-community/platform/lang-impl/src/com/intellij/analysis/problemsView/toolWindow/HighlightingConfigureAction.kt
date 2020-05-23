// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.codeInsight.daemon.impl.analysis.FileHighlightingSetting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightLevelUtil.forceRootHighlighting
import com.intellij.codeInsight.daemon.impl.analysis.HighlightingLevelManager
import com.intellij.icons.AllIcons.General
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.editor.markup.InspectionsLevel
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.DumbAwareToggleAction
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.FileViewProvider
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.ui.LayeredIcon
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.scale.JBUIScale.scale
import java.awt.Point

internal class HighlightingConfigureAction : DumbAwareAction(LayeredIcon(General.GearPlain, General.Dropdown)) {
  override fun update(event: AnActionEvent) {
    val root = getHighlightingFileRoot(event)
    event.presentation.isVisible = root != null
    val enabled = root?.let { getFileViewProvider(it)?.languages?.isNotEmpty() }
    event.presentation.isEnabled = enabled == true
  }

  override fun actionPerformed(event: AnActionEvent) {
    val root = getHighlightingFileRoot(event) ?: return
    val provider = getFileViewProvider(root) ?: return

    val languages = provider.languages
    if (languages.isEmpty()) return

    val index = ProjectFileIndex.getInstance(root.project!!)
    val library = index.isInLibrary(root.file) && !index.isInContent(root.file)
    val separator = languages.count() > 1

    val group = DefaultActionGroup()
    languages.sortedBy { it.displayName }.forEach {
      if (separator) group.add(Separator.create(it.displayName))
      group.add(LevelAction(InspectionsLevel.NONE, it, provider))
      group.add(LevelAction(InspectionsLevel.ERRORS, it, provider))
      if (!library) group.add(LevelAction(InspectionsLevel.ALL, it, provider))
    }
    val title = ProblemsViewBundle.message("problems.view.highlighting.configure", root.file.name)
    val popup = JBPopupFactory.getInstance().createActionGroupPopup(title, group, event.dataContext, true, null, 100)
    val component = event.inputEvent?.component
    if (component != null) {
      val horizontal = ProblemsView.getToolWindow(event.project)?.anchor?.isHorizontal
      popup.show(RelativePoint(component, when (horizontal) {
        true -> Point(component.width, scale(2))
        else -> Point(scale(2), component.height)
      }))

    }
    else {
      popup.showInBestPositionFor(event.dataContext)
    }
  }

  private fun getHighlightingFileRoot(event: AnActionEvent?): HighlightingFileRoot? {
    val panel = ProblemsView.getSelectedPanel(event?.project)
    return panel?.treeModel?.root as? HighlightingFileRoot
  }

  private fun getFileViewProvider(root: HighlightingFileRoot): FileViewProvider? {
    val project = root.project ?: return null
    if (project.isDisposed || root.file.isDirectory) return null
    return PsiManagerEx.getInstanceEx(project)?.fileManager?.findViewProvider(root.file)
  }
}

internal class LevelAction(val level: InspectionsLevel, val language: Language, val provider: FileViewProvider)
  : DumbAwareToggleAction(level.toString()) {

  override fun isSelected(event: AnActionEvent): Boolean {
    val file = provider.getPsi(language) ?: return false
    val manager = HighlightingLevelManager.getInstance(file.project) ?: return false
    return level == when {
      manager.shouldInspect(file) -> InspectionsLevel.ALL
      manager.shouldHighlight(file) -> InspectionsLevel.ERRORS
      else -> InspectionsLevel.NONE
    }
  }

  override fun setSelected(event: AnActionEvent, state: Boolean) {
    if (!state) return
    val file = provider.getPsi(language) ?: return
    forceRootHighlighting(file, when (level) {
      InspectionsLevel.NONE -> FileHighlightingSetting.SKIP_HIGHLIGHTING
      InspectionsLevel.ERRORS -> FileHighlightingSetting.SKIP_INSPECTION
      InspectionsLevel.ALL -> FileHighlightingSetting.FORCE_HIGHLIGHTING
    })
  }
}
