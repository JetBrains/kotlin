// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
@file:JvmName("Declarations")

package com.intellij.model.psi.impl

import com.intellij.model.psi.PsiSymbolDeclaration
import com.intellij.model.psi.PsiSymbolDeclarationProvider
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.elementsAroundOffsetUp
import com.intellij.util.SmartList

/**
 * @return collection of declarations found around the given [offset][offsetInFile] in [file][this]
 */
fun PsiFile.allDeclarationsAround(offsetInFile: Int): Collection<PsiSymbolDeclaration> {
  for ((element: PsiElement, offsetInElement: Int) in elementsAroundOffsetUp(offsetInFile)) {
    ProgressManager.checkCanceled()
    val declarations: Collection<PsiSymbolDeclaration> = declarationsInElement(element, offsetInElement)
    if (declarations.isNotEmpty()) {
      return declarations
    }
  }
  return emptyList()
}

private val declarationProviderEP = ExtensionPointName.create<PsiSymbolDeclarationProvider>("com.intellij.psi.declarationProvider")

private fun declarationsInElement(element: PsiElement, offsetInElement: Int): Collection<PsiSymbolDeclaration> {
  val result = SmartList<PsiSymbolDeclaration>()
  for (extension: PsiSymbolDeclarationProvider in declarationProviderEP.extensions) {
    ProgressManager.checkCanceled()
    extension.getDeclarations(element, offsetInElement).filterTo(result) {
      element === it.declaringElement && offsetInElement in it.declarationRange
    }
  }
  return result
}
