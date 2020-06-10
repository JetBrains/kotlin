// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.toolWindow

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.impl.CompoundIconProvider.findIcon
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil.getLocationRelativeToUserHome
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.PsiUtilCore.findFileSystemItem
import com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES
import com.intellij.ui.tree.LeafState

internal class FileNode(parent: Node, val file: VirtualFile) : Node(parent) {

  override fun getLeafState() = if (parentDescriptor is Root) LeafState.NEVER else LeafState.DEFAULT

  override fun getName() = file.presentableName ?: file.name

  override fun update(project: Project, presentation: PresentationData) {
    presentation.setIcon(findIcon(findFileSystemItem(project, file), 0) ?: when (file.isDirectory) {
      true -> AllIcons.Nodes.Folder
      else -> AllIcons.FileTypes.Any_type
    })
    if (parentDescriptor !is FileNode) {
      val url = file.parent?.presentableUrl ?: return
      presentation.addText("  ${getLocationRelativeToUserHome(url)}", GRAYED_ATTRIBUTES)
    }
    val root = findAncestor(Root::class.java)
    val count = root?.getProblemsCount(file) ?: 0
    if (count > 0) {
      val text = ProblemsViewBundle.message("problems.view.file.problems", count)
      presentation.addText("  $text", GRAYED_ATTRIBUTES)
    }
  }

  override fun getChildren(): Collection<Node> {
    val root = findAncestor(Root::class.java)
    return root?.getChildren(file) ?: super.getChildren()
  }
}
