// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.tree.injected.changesHandler

import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.util.ProperTextRange
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.Trinity
import com.intellij.psi.*
import com.intellij.util.containers.ContainerUtil
import java.util.*
import kotlin.math.max

open class CommonInjectedFileChangesHandler(shreds: List<PsiLanguageInjectionHost.Shred>,
                                            editor: Editor,
                                            newDocument: Document,
                                            injectedFile: PsiFile) : AbstractMarkerBasedInjectedFileChangesHandler(editor, newDocument,
                                                                                                                   injectedFile) {
  override val markers: MutableList<Marker> =
    ContainerUtil.newLinkedList<Marker>().apply {
      addAll(getMarkersFromShreds(shreds))
    }


  protected fun getMarkersFromShreds(shreds: List<PsiLanguageInjectionHost.Shred>): List<Marker> {
    val result = ArrayList<Marker>(shreds.size)

    val smartPointerManager = SmartPointerManager.getInstance(myProject)
    var curOffset = -1
    for (shred in shreds) {
      val rangeMarker = localRangeMarkerFromShred(shred)
      val rangeInsideHost = shred.rangeInsideHost
      val host = shred.host ?: failAndReport("host should not be null", null, null)
      val origMarker = myOrigDocument.createRangeMarker(rangeInsideHost.shiftRight(host.textRange.startOffset))
      val elementPointer = smartPointerManager.createSmartPsiElementPointer(host)
      val markers = Trinity.create(origMarker, rangeMarker, elementPointer)
      result.add(markers)

      origMarker.isGreedyToRight = true
      rangeMarker.isGreedyToRight = true
      if (origMarker.startOffset > curOffset) {
        origMarker.isGreedyToLeft = true
        rangeMarker.isGreedyToLeft = true
      }
      curOffset = origMarker.endOffset
    }
    return result
  }


  override fun isValid(): Boolean =
    myInjectedFile.isValid && markers.all { it.first.isValid && it.second.isValid && it.third.element != null }

  override fun commitToOriginal(e: DocumentEvent) {
    val text = myNewDocument.text
    val map = markers.groupByTo(LinkedHashMap()) { it.host }

    val documentManager = PsiDocumentManager.getInstance(myProject)
    documentManager.commitDocument(myOrigDocument) // commit here and after each manipulator update
    var localInsideFileCursor = 0
    for (host in map.keys) {
      if (host == null) continue
      val hostText = host.text
      var insideHost: ProperTextRange? = null
      val sb = StringBuilder()
      for (entry in map[host].orEmpty()) {
        val origMarker = entry.origin // check for validity?
        val hostOffset = host.textRange.startOffset
        val localInsideHost = ProperTextRange(origMarker.startOffset - hostOffset, origMarker.endOffset - hostOffset)
        val rangeMarker = entry.second
        val localInsideFile = ProperTextRange(max(localInsideFileCursor, rangeMarker.startOffset), rangeMarker.endOffset)
        if (insideHost != null) {
          //append unchanged inter-markers fragment
          sb.append(hostText, insideHost.endOffset, localInsideHost.startOffset)
        }

        if (localInsideFile.endOffset <= text.length && !localInsideFile.isEmpty) {
          sb.append(localInsideFile.substring(text))
        }
        localInsideFileCursor = localInsideFile.endOffset
        insideHost = insideHost?.union(localInsideHost) ?: localInsideHost
      }
      if (insideHost == null) failAndReport("insideHost is null", e)
      updateInjectionHostElement(host, insideHost, sb.toString())
      documentManager.commitDocument(myOrigDocument)
    }
  }

  protected fun updateInjectionHostElement(host: PsiLanguageInjectionHost, insideHost: ProperTextRange, content: String) {
    ElementManipulators.getManipulator(host).handleContentChange(host, insideHost, content)
  }

  override fun handlesRange(range: TextRange): Boolean {
    if (markers.isNotEmpty()) {
      val hostRange = TextRange.create(markers[0].first.startOffset,
                                       markers[markers.size - 1].first.endOffset)
      return range.intersects(hostRange)
    }
    return false
  }
}
