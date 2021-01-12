// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing

import com.intellij.lang.ASTNode
import com.intellij.openapi.editor.Document
import com.intellij.psi.PsiFile

sealed class DocumentContent {
  abstract val text: CharSequence
  abstract val modificationStamp: Long
}

internal class AuthenticContent(private val myDocument: Document) : DocumentContent() {
  override val text: CharSequence = myDocument.immutableCharSequence

  override val modificationStamp: Long = myDocument.modificationStamp

}

internal class PsiContent(private val myDocument: Document,
                          private val myFile: PsiFile) : DocumentContent() {
  override val text: CharSequence
    get() {
      if (myFile.viewProvider.modificationStamp != myDocument.modificationStamp) {
        val node: ASTNode = myFile.node!!
        return node.chars
      }
      return myDocument.immutableCharSequence
    }

  override val modificationStamp: Long = myFile.viewProvider.modificationStamp

}