// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.suggested

import com.intellij.lang.Language
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.RangeMarker
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.util.TextRange
import java.util.*

class NewIdentifierWatcher(private val maxIdentifiers: Int) {
  init {
    require(maxIdentifiers > 0)
  }

  private class Ref(var marker: RangeMarker)

  private val newIdentifierMarkers = ArrayDeque<Ref>()

  val lastDocument: Document?
    get() = newIdentifierMarkers.firstOrNull()?.marker?.takeIf { it.isValid }?.document

  fun lastNewIdentifierRanges(): List<TextRange> {
    return newIdentifierMarkers.map { it.marker.range!! }
  }

  fun documentChanged(event: DocumentEvent, language: Language) {
    val document = event.document
    val chars = document.charsSequence

    if (document != lastDocument) {
      reset()
    }

    val refactoringSupport = SuggestedRefactoringSupport.forLanguage(language) ?: return

    val iterator = newIdentifierMarkers.iterator()
    while (iterator.hasNext()) {
      val ref = iterator.next()
      val range = ref.marker.range
      if (range == null) {
        iterator.remove()
        continue
      }

      var start = range.startOffset
      while (start > 0 && refactoringSupport.isIdentifierPart(chars[start - 1])) {
        start--
      }

      var end = range.endOffset
      while (end < document.textLength && refactoringSupport.isIdentifierPart(chars[end])) {
        end++
      }

      if (start != range.startOffset || end != range.endOffset) {
        ref.marker.dispose()
        ref.marker = document.createRangeMarker(start, end)
      }
    }

    if (isNewIdentifierInserted(event, refactoringSupport)) {
      if (newIdentifierMarkers.size == maxIdentifiers) {
        val marker = newIdentifierMarkers.removeFirst().marker
        marker.dispose()
      }
      val marker = document.createRangeMarker(event.newRange)
      newIdentifierMarkers.addLast(Ref(marker))
    }
    else {
      if (newIdentifierMarkers.any { it.marker.range!!.contains(event.newRange) }) return

      if (event.newFragment.any { refactoringSupport.isIdentifierStart(it) }) {
        reset()
      }
    }
  }

  fun reset() {
    newIdentifierMarkers.forEach { it.marker.dispose() }
    newIdentifierMarkers.clear()
  }

  private fun isNewIdentifierInserted(event: DocumentEvent, refactoringSupport: SuggestedRefactoringSupport): Boolean {
    if (!refactoringSupport.isIdentifierLike(event.newFragment)) return false
    val chars = event.document.charsSequence
    val start = event.offset
    val end = event.offset + event.newLength
    if (start > 0 && refactoringSupport.isIdentifierPart(chars[start - 1])) return false
    if (end < event.document.textLength && refactoringSupport.isIdentifierPart(chars[end])) return false
    return true
  }

  private fun SuggestedRefactoringSupport.isIdentifierLike(text: CharSequence) =
    text.isNotEmpty() && isIdentifierStart(text[0]) && text.all { isIdentifierPart(it) }
}
