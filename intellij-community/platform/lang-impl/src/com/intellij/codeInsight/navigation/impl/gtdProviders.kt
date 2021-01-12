// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.impl

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.navigation.CtrlMouseInfo
import com.intellij.codeInsight.navigation.MultipleTargetElementsInfo
import com.intellij.codeInsight.navigation.PsiElementTargetPopupPresentation
import com.intellij.codeInsight.navigation.SingleTargetElementInfo
import com.intellij.codeInsight.navigation.action.GotoDeclarationUtil.findTargetElementsFromProviders
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList

internal fun fromGTDProviders(project: Project, editor: Editor, offset: Int): GTDActionData? {
  return processInjectionThenHost(editor, offset) { _editor, _offset ->
    fromGTDProvidersInner(project, _editor, _offset)
  }
}

/**
 * @see com.intellij.codeInsight.navigation.actions.GotoDeclarationAction.findTargetElementsFromProviders
 */
private fun fromGTDProvidersInner(project: Project, editor: Editor, offset: Int): GTDActionData? {
  val document = editor.document
  val file = PsiDocumentManager.getInstance(project).getPsiFile(document) ?: return null
  val adjustedOffset: Int = TargetElementUtil.adjustOffset(file, document, offset)
  val leafElement: PsiElement = file.findElementAt(adjustedOffset) ?: return null
  val fromProviders: Array<out PsiElement>? = findTargetElementsFromProviders(leafElement, adjustedOffset, editor)
  if (fromProviders.isNullOrEmpty()) {
    return null
  }
  return GTDProviderData(leafElement, fromProviders.toList())
}

private class GTDProviderData(
  private val leafElement: PsiElement,
  private val targetElements: Collection<PsiElement>
) : GTDActionData {

  init {
    require(targetElements.isNotEmpty())
  }

  override fun ctrlMouseInfo(): CtrlMouseInfo {
    val singleTarget = targetElements.singleOrNull()
    return if (singleTarget == null) {
      MultipleTargetElementsInfo(leafElement)
    }
    else {
      SingleTargetElementInfo(leafElement, singleTarget)
    }
  }

  override fun result(): GTDActionResult? {
    val singleTarget = targetElements.singleOrNull()
    if (singleTarget != null) {
      return gtdTargetNavigatable(singleTarget)?.let(GTDActionResult::SingleTarget)
    }
    val result: List<Pair<Navigatable, PsiElement>> = targetElements.mapNotNullTo(SmartList()) { targetElement ->
      psiNavigatable(targetElement)?.let { navigatable ->
        Pair(navigatable, targetElement)
      }
    }
    return when (result.size) {
      0 -> null
      1 -> GTDActionResult.SingleTarget(result.single().first)
      else -> GTDActionResult.MultipleTargets(result.map { (navigatable, targetElement) ->
        Pair(navigatable, PsiElementTargetPopupPresentation(targetElement))
      })
    }
  }
}
