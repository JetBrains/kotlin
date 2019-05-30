// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.navigation.impl

import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.navigation.CtrlMouseInfo
import com.intellij.codeInsight.navigation.MultipleTargetElementsInfo
import com.intellij.codeInsight.navigation.SingleTargetElementInfo
import com.intellij.ide.util.EditSourceUtil
import com.intellij.injected.editor.EditorWindow
import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.model.psi.impl.PsiOrigin
import com.intellij.model.psi.impl.TargetData
import com.intellij.openapi.editor.Editor
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

internal fun <X : Any> processInjectionThenHost(file: PsiFile, offset: Int, function: (file: PsiFile, offset: Int) -> X?): X? {
  return function(file, offset)
         ?: fromHostFile(file, offset, function)
}

private fun <X : Any> fromHostFile(file: PsiFile, offset: Int, function: (file: PsiFile, offset: Int) -> X?): X? {
  val manager = InjectedLanguageManager.getInstance(file.project)
  val topLevelFile = manager.getTopLevelFile(file) ?: return null
  return function(topLevelFile, manager.injectedToHost(file, offset))
}

internal fun <X : Any> processInjectionThenHost(editor: Editor, offset: Int, function: (editor: Editor, offset: Int) -> X?): X? {
  return function(editor, offset)
         ?: fromHostEditor(editor, offset, function)
}

private fun <X : Any> fromHostEditor(editor: Editor, offset: Int, function: (editor: Editor, offset: Int) -> X?): X? {
  if (editor !is EditorWindow) {
    return null
  }
  return function(editor.delegate, editor.document.injectedToHost(offset))
}

internal fun TargetData.ctrlMouseInfo(): CtrlMouseInfo {
  return when (this) {
    is TargetData.Declared -> {
      DeclarationCtrlMouseInfo(declaration)
    }
    is TargetData.Referenced -> {
      val ranges = listOf(references.first().absoluteRange)
      val singleTarget = targets.singleOrNull()
      if (singleTarget != null) {
        SingleSymbolCtrlMouseInfo(singleTarget, ranges)
      }
      else {
        MultipleTargetElementsInfo(ranges)
      }
    }
    is TargetData.Evaluator -> {
      val singleTargetElement = targetElements.singleOrNull()
      if (singleTargetElement != null) {
        when (origin) {
          is PsiOrigin.Leaf -> SingleTargetElementInfo(origin.leaf, singleTargetElement)
          is PsiOrigin.Reference -> SingleTargetElementInfo(origin.reference, singleTargetElement)
        }
      }
      else {
        when (origin) {
          is PsiOrigin.Leaf -> MultipleTargetElementsInfo(origin.leaf)
          is PsiOrigin.Reference -> MultipleTargetElementsInfo(origin.reference)
        }
      }
    }
  }
}

internal fun gtdTargetNavigatable(targetElement: PsiElement): Navigatable? {
  return gtdTarget(targetElement)?.let(::psiNavigatable)
}

private fun gtdTarget(targetElement: PsiElement): PsiElement? {
  return TargetElementUtil.getInstance().getGotoDeclarationTarget(targetElement, targetElement.navigationElement)
}

internal fun psiNavigatable(targetElement: PsiElement): Navigatable? {
  return targetElement as? Navigatable
         ?: EditSourceUtil.getDescriptor(targetElement)
}
