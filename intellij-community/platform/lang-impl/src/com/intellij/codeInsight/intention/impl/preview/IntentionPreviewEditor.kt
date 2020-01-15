// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.intention.impl.preview

import com.intellij.openapi.editor.impl.ImaginaryEditor
import com.intellij.psi.PsiFile

internal class IntentionPreviewEditor(psiFileCopy: PsiFile, caretOffset: Int)
  : ImaginaryEditor(psiFileCopy.viewProvider.document!!) {

  init {
    caretModel.moveToOffset(caretOffset)
  }

  override fun notImplemented(): RuntimeException = IntentionPreviewUnsupportedOperationException()

  override fun isViewer(): Boolean = true

}

class IntentionPreviewUnsupportedOperationException
  : UnsupportedOperationException("It's unexpected to invoke this method on an intention preview calculating.")