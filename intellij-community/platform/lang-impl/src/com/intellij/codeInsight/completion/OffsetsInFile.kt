/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.codeInsight.completion

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.openapi.editor.impl.DocumentImpl
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.ChangedPsiRangeUtil
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil
import com.intellij.psi.text.BlockSupport

/**
 * @author peter
 */
class OffsetsInFile(val file: PsiFile, val offsets: OffsetMap) {
  constructor(file: PsiFile) : this(file, OffsetMap(file.viewProvider.document!!))

  fun toTopLevelFile(): OffsetsInFile {
    val manager = InjectedLanguageManager.getInstance(file.project)
    val hostFile = manager.getTopLevelFile(file)
    if (hostFile == file) return this
    return OffsetsInFile(hostFile, offsets.mapOffsets(hostFile.viewProvider.document!!) { manager.injectedToHost(file, it) })
  }

  fun toInjectedIfAny(offset: Int): OffsetsInFile {
    val injected = InjectedLanguageUtil.findInjectedPsiNoCommit(file, offset) ?: return this
    val documentWindow = InjectedLanguageUtil.getDocumentWindow(injected)!!
    return OffsetsInFile(injected, offsets.mapOffsets(documentWindow) { documentWindow.hostToInjected(it) })
  }

  fun copyWithReplacement(startOffset: Int, endOffset: Int, replacement: String): OffsetsInFile {
    return replaceInCopy(file.copy() as PsiFile, startOffset, endOffset, replacement)
  }

  fun replaceInCopy(fileCopy: PsiFile, startOffset: Int, endOffset: Int, replacement: String): OffsetsInFile {
    val tempDocument = DocumentImpl(offsets.document.immutableCharSequence, true)
    val tempMap = offsets.copyOffsets(tempDocument)
    tempDocument.replaceString(startOffset, endOffset, replacement)

    reparseFile(fileCopy, tempDocument.immutableCharSequence)

    val copyOffsets = tempMap.copyOffsets(fileCopy.viewProvider.document!!)
    return OffsetsInFile(fileCopy, copyOffsets)
  }

  private fun reparseFile(file: PsiFile, newText: CharSequence) {
    val node = file.node as? FileElement ?: throw IllegalStateException("${file.javaClass} ${file.fileType}")
    val range = ChangedPsiRangeUtil.getChangedPsiRange(file, node, newText) ?: return
    val indicator = ProgressManager.getGlobalProgressIndicator() ?: EmptyProgressIndicator()
    val log = BlockSupport.getInstance(file.project).reparseRange(file, node, range, newText, indicator, file.viewProvider.contents)

    ProgressManager.getInstance().executeNonCancelableSection { log.doActualPsiChange(file) }
  }

}
