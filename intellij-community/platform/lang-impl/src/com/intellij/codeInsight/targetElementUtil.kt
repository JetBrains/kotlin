// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:Experimental

package com.intellij.codeInsight

import com.intellij.injected.editor.EditorWindow
import com.intellij.navigation.NavigationTarget
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.jetbrains.annotations.ApiStatus.Experimental

fun findAllTargets(project: Project, editor: Editor, file: PsiFile): Collection<NavigationTarget> {
  val offset = editor.caretModel.offset
  if (TargetElementUtil.inVirtualSpace(editor, offset)) {
    return emptyList()
  }
  return findAllTargetsNoVS(project, editor, offset, file)
}

private fun findAllTargetsNoVS(project: Project, editor: Editor, offset: Int, file: PsiFile): Collection<NavigationTarget> {
  val targets = collectAllTargets(project, editor, file)
  if (!targets.isEmpty()) {
    return targets
  }
  // if no targets found in injected fragment, try outer document
  if (editor is EditorWindow) {
    return findAllTargetsNoVS(project, editor.delegate, editor.document.injectedToHost(offset), file)
  }
  return emptyList()
}

private fun collectAllTargets(project: Project, editor: Editor, file: PsiFile): Collection<NavigationTarget> {
  TODO()
}
