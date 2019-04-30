// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.impl.source.tree.injected.changesHandler

import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.util.Segment
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.Trinity
import com.intellij.psi.*
import org.jetbrains.annotations.ApiStatus
import kotlin.math.max
import kotlin.math.min

abstract class AbstractMarkerBasedInjectedFileChangesHandler(editor: Editor,
                                                             newDocument: Document,
                                                             injectedFile: PsiFile) :
  BaseInjectedFileChangesHandler(editor, newDocument, injectedFile) {

  protected abstract val markers: MutableList<Marker>

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

  protected fun MutableList<Marker>.logMarkersRanges(): String = joinToString("\n") { mm ->
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

  fun distributeTextToMarkers(affectedMarkers: List<Marker>, affectedRange: TextRange, limit: Int): List<Pair<Marker, String>> {
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

private fun affectedLength(marker: Marker?, affectedRange: TextRange): Int =
  marker?.localRange?.let { affectedRange.intersection(it)?.length } ?: -1

typealias Marker = Trinity<RangeMarker, RangeMarker, SmartPsiElementPointer<PsiLanguageInjectionHost>>

inline val Marker.host: PsiLanguageInjectionHost? get() = this.third.element

inline val Marker.hostSegment: Segment? get() = this.third.range

inline val Marker.origin: RangeMarker get() = this.first

inline val Marker.local: RangeMarker get() = this.second

inline val Marker.localRange: TextRange get() = this.second.range

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
