// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
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
import org.jetbrains.annotations.ApiStatus
import java.util.*
import kotlin.math.max
import kotlin.math.min

open class CommonInjectedFileChangesHandler(
  shreds: List<PsiLanguageInjectionHost.Shred>,
  hostEditor: Editor,
  fragmentDocument: Document,
  injectedFile: PsiFile
) : BaseInjectedFileChangesHandler(hostEditor, fragmentDocument, injectedFile) {

  protected val markers: MutableList<MarkersMapping> =
    LinkedList<MarkersMapping>().apply {
      addAll(getMarkersFromShreds(shreds))
    }


  protected fun getMarkersFromShreds(shreds: List<PsiLanguageInjectionHost.Shred>): List<MarkersMapping> {
    val result = ArrayList<MarkersMapping>(shreds.size)

    val smartPointerManager = SmartPointerManager.getInstance(myProject)
    var curOffset = -1
    for (shred in shreds) {
      val rangeMarker = fragmentMarkerFromShred(shred)
      val rangeInsideHost = shred.rangeInsideHost
      val host = shred.host ?: failAndReport("host should not be null", null, null)
      val origMarker = myHostDocument.createRangeMarker(rangeInsideHost.shiftRight(host.textRange.startOffset))
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
    val text = myFragmentDocument.text
    val map = markers.groupByTo(LinkedHashMap()) { it.host }

    val documentManager = PsiDocumentManager.getInstance(myProject)
    documentManager.commitDocument(myHostDocument) // commit here and after each manipulator update
    var localInsideFileCursor = 0
    for (host in map.keys) {
      if (host == null) continue
      val hostText = host.text
      var insideHost: ProperTextRange? = null
      val sb = StringBuilder()
      for ((hostMarker, fragmentMarker, _) in map[host].orEmpty()) {
        val hostOffset = host.textRange.startOffset
        val localInsideHost = ProperTextRange(hostMarker.startOffset - hostOffset, hostMarker.endOffset - hostOffset)
        val localInsideFile = ProperTextRange(max(localInsideFileCursor, fragmentMarker.startOffset), fragmentMarker.endOffset)
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
      documentManager.commitDocument(myHostDocument)
    }
  }

  protected fun updateInjectionHostElement(host: PsiLanguageInjectionHost, insideHost: ProperTextRange, content: String) {
    ElementManipulators.handleContentChange(host, insideHost, content)
  }

  override fun dispose() {
    markers.forEach(MarkersMapping::dispose)
    markers.clear()
    super.dispose()
  }

  override fun handlesRange(range: TextRange): Boolean {
    if (markers.isEmpty()) return false

    val hostRange = TextRange.create(markers[0].hostMarker.startOffset,
                                     markers[markers.size - 1].hostMarker.endOffset)
    return range.intersects(hostRange)
  }

  protected fun fragmentMarkerFromShred(shred: PsiLanguageInjectionHost.Shred): RangeMarker =
    myFragmentDocument.createRangeMarker(shred.innerRange)

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
                                      Attachment("injected document", this.myFragmentDocument.text),
                                      exception?.let { Attachment("exception", it) }
                                    ).toTypedArray()
    )

  protected fun MutableList<MarkersMapping>.logMarkersRanges(): String = joinToString("\n") { mm ->
    "fragment:${myFragmentDocument.logMarker(mm.fragmentRange)} host:${logHostMarker(mm.hostMarker.range)}"
  }

  protected fun logHostMarker(rangeInHost: TextRange?) = myHostDocument.logMarker(rangeInHost)

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
      val fragmentMarker = marker.fragmentMarker

      marker to if (fragmentMarker.isValid) {
        val start = max(cursor, fragmentMarker.startOffset)
        val text = fragmentMarker.document.text
        cursor = if (affectedLength(marker, affectedRange) == 0 && affectedLength(affectedMarkers.getOrNull(i + 1), affectedRange) > 1)
          affectedMarkers.getOrNull(i + 1)!!.fragmentMarker.startOffset
        else
          min(text.length, max(fragmentMarker.endOffset, limit))

        text.substringVerbose(start, cursor)
      }
      else ""
    }
  }

}


data class MarkersMapping(val hostMarker: RangeMarker,
                          val fragmentMarker: RangeMarker,
                          val hostPointer: SmartPsiElementPointer<PsiLanguageInjectionHost>) {
  val host: PsiLanguageInjectionHost? get() = hostPointer.element
  val hostElementRange: TextRange? get() = hostPointer.range?.range
  val fragmentRange: TextRange get() = fragmentMarker.range
  fun isValid(): Boolean = hostMarker.isValid && fragmentMarker.isValid && hostPointer.element?.isValid == true
  fun dispose() {
    fragmentMarker.dispose()
    hostMarker.dispose()
  }
}

inline val Segment.range: TextRange get() = TextRange.create(this)

inline val PsiLanguageInjectionHost.Shred.innerRange: TextRange
  get() = TextRange.create(this.range.startOffset + this.prefix.length,
                           this.range.endOffset - this.suffix.length)

val PsiLanguageInjectionHost.contentRange
  get() = ElementManipulators.getValueTextRange(this).shiftRight(textRange.startOffset)

private val PsiElement.withNextSiblings: Sequence<PsiElement>
  get() = generateSequence(this) { it.nextSibling }

@ApiStatus.Internal
fun getInjectionHostAtRange(hostPsiFile: PsiFile, contextRange: Segment): PsiLanguageInjectionHost? =
  hostPsiFile.findElementAt(contextRange.startOffset)?.withNextSiblings.orEmpty()
    .takeWhile { it.textRange.startOffset < contextRange.endOffset }
    .flatMap { sequenceOf(it, it.parent) }
    .filterIsInstance<PsiLanguageInjectionHost>().firstOrNull()

private fun affectedLength(markersMapping: MarkersMapping?, affectedRange: TextRange): Int =
  markersMapping?.fragmentRange?.let { affectedRange.intersection(it)?.length } ?: -1