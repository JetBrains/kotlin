// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.tree.injected.changesHandler

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.util.ProperTextRange
import com.intellij.openapi.util.Segment
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.ApiStatus
import java.util.*
import kotlin.math.max
import kotlin.math.min

open class CommonInjectedFileChangesHandler(
  shreds: List<PsiLanguageInjectionHost.Shred>,
  editor: Editor,
  newDocument: Document,
  injectedFile: PsiFile
) : BaseInjectedFileChangesHandler(editor, newDocument, injectedFile) {

  protected val markers: MutableList<MarkersMapping> =
    ContainerUtil.newLinkedList<MarkersMapping>().apply {
      addAll(getMarkersFromShreds(shreds))
    }


  protected fun getMarkersFromShreds(shreds: List<PsiLanguageInjectionHost.Shred>): List<MarkersMapping> {
    val result = ArrayList<MarkersMapping>(shreds.size)

    val smartPointerManager = SmartPointerManager.getInstance(myProject)
    var curOffset = -1
    for (shred in shreds) {
      val rangeMarker = localRangeMarkerFromShred(shred)
      val rangeInsideHost = shred.rangeInsideHost
      val host = shred.host ?: failAndReport("host should not be null", null, null)
      val origMarker = myOrigDocument.createRangeMarker(rangeInsideHost.shiftRight(host.textRange.startOffset))
      val elementPointer = smartPointerManager.createSmartPsiElementPointer(host)
      result.add(MarkersMapping(origMarker, rangeMarker, elementPointer))

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


  override fun isValid(): Boolean = myInjectedFile.isValid && markers.all { it.isValid() }

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
        val rangeMarker = entry.local
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
    if (markers.isEmpty()) return false

    val hostRange = TextRange.create(markers[0].origin.startOffset,
                                     markers[markers.size - 1].origin.endOffset)
    return range.intersects(hostRange)
  }

  protected fun localRangeMarkerFromShred(shred: PsiLanguageInjectionHost.Shred): RangeMarker =
    myNewDocument.createRangeMarker(shred.innerRange)

  protected fun failAndReport(message: String, e: DocumentEvent? = null, exception: Exception? = null): Nothing =
    throw getReportException(message, e, exception)

  protected fun getReportException(message: String,
                                   e: DocumentEvent?,
                                   exception: Exception?): RuntimeExceptionWithAttachments =
    RuntimeExceptionWithAttachments("${this.javaClass.simpleName}: $message (event = $e)," +
                                    " myInjectedFile.isValid = ${myInjectedFile.isValid}, isValid = $isValid",
                                    *listOfNotNull(
                                      Attachment("hosts", markers.joinToString("\n\n") { it.host?.text ?: "<null>" }),
                                      Attachment("markers", markers.logMarkersRanges()),
                                      Attachment("injected document", this.myNewDocument.text),
                                      exception?.let { Attachment("exception", it) }
                                    ).toTypedArray()
    )

  protected fun MutableList<MarkersMapping>.logMarkersRanges(): String = joinToString("\n") { mm ->
    "local:${myNewDocument.logMarker(mm.localRange)} orig:${logHostMarker(mm.origin.range)}"
  }

  protected fun logHostMarker(rangeInHost: TextRange?) = myOrigDocument.logMarker(rangeInHost)

  protected fun Document.logMarker(rangeInHost: TextRange?): String = "$rangeInHost -> '${rangeInHost?.let {
    try {
      getText(it)
    }
    catch (e: IndexOutOfBoundsException) {
      e.toString()
    }
  }}'"

  protected fun String.substringVerbose(start: Int, cursor: Int): String = try {
    substring(start, cursor)
  }
  catch (e: StringIndexOutOfBoundsException) {
    failAndReport("can't get substring ($start, $cursor) of '${this}'[$length]", exception = e)
  }

  fun distributeTextToMarkers(affectedMarkers: List<MarkersMapping>,
                              affectedRange: TextRange,
                              limit: Int): List<Pair<MarkersMapping, String>> {
    var cursor = 0
    return affectedMarkers.indices.map { i ->
      val marker = affectedMarkers[i]
      val localMarker = marker.local

      marker to if (localMarker.isValid) {
        val start = max(cursor, localMarker.startOffset)
        val text = localMarker.document.text
        cursor = if (affectedLength(marker, affectedRange) == 0 && affectedLength(affectedMarkers.getOrNull(i + 1), affectedRange) > 1)
          affectedMarkers.getOrNull(i + 1)!!.local.startOffset
        else
          min(text.length, max(localMarker.endOffset, limit))

        text.substringVerbose(start, cursor)
      }
      else ""
    }
  }

}


data class MarkersMapping(val origin: RangeMarker,
                          val local: RangeMarker,
                          val hostPointer: SmartPsiElementPointer<PsiLanguageInjectionHost>) {
  val host: PsiLanguageInjectionHost? get() = hostPointer.element
  val hostRange: TextRange? get() = hostPointer.range?.range
  val localRange: TextRange get() = local.range
  fun isValid(): Boolean = origin.isValid && local.isValid && hostPointer.element?.isValid == true
}

inline val Segment.range: TextRange get() = TextRange.create(this)

inline val PsiLanguageInjectionHost.Shred.innerRange: TextRange
  get() = TextRange.create(this.range.startOffset + this.prefix.length,
                           this.range.endOffset - this.suffix.length)

val PsiLanguageInjectionHost.contentRange
  get() = ElementManipulators.getManipulator(this).getRangeInElement(this).shiftRight(textRange.startOffset)

private val PsiElement.withNextSiblings: Sequence<PsiElement>
  get() = generateSequence(this) { it.nextSibling }

@ApiStatus.Internal
fun getInjectionHostAtRange(origPsiFile: PsiFile, contextRange: Segment): PsiLanguageInjectionHost? =
  origPsiFile.findElementAt(contextRange.startOffset)?.withNextSiblings.orEmpty()
    .takeWhile { it.textRange.startOffset < contextRange.endOffset }
    .flatMap { sequenceOf(it, it.parent) }
    .filterIsInstance<PsiLanguageInjectionHost>().firstOrNull()

private fun affectedLength(markersMapping: MarkersMapping?, affectedRange: TextRange): Int =
  markersMapping?.localRange?.let { affectedRange.intersection(it)?.length } ?: -1