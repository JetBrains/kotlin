// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable

internal data class EditorCaretMovementOptions(val isNextCaretStopAtStart: Boolean = false,
                                               val isNextCaretStopAtEnd: Boolean = false,
                                               val isPreviousCaretStopAtStart: Boolean = false,
                                               val isPreviousCaretStopAtEnd: Boolean = false) {

  fun isModified(editorSettings: EditorSettingsExternalizable): Boolean =
    this != EditorCaretMovementOptions.fromWordBoundarySettings(editorSettings)

  fun apply(editorSettings: EditorSettingsExternalizable) {
    editorSettings.setCaretStopAtWordStart(true, isNextCaretStopAtStart)
    editorSettings.setCaretStopAtWordEnd(true, isNextCaretStopAtEnd)
    editorSettings.setCaretStopAtWordStart(false, isPreviousCaretStopAtStart)
    editorSettings.setCaretStopAtWordEnd(false, isPreviousCaretStopAtEnd)
  }

  companion object {
    fun fromWordBoundarySettings(editorSettings: EditorSettingsExternalizable): EditorCaretMovementOptions =
      EditorCaretMovementOptions(editorSettings.isCaretStopAtWordStart(true),
                                 editorSettings.isCaretStopAtWordEnd(true),
                                 editorSettings.isCaretStopAtWordStart(false),
                                 editorSettings.isCaretStopAtWordEnd(false))
  }

  internal interface Item {
    val title: String
    val options: EditorCaretMovementOptions
  }

  internal enum class WordBoundary(override val title: String,
                                   override val options: EditorCaretMovementOptions) : Item {
    STICK_TO_WORD_BOUNDARIES(ApplicationBundle.message("combobox.item.stick.to.word.boundaries"),
                             EditorCaretMovementOptions(isNextCaretStopAtEnd = true,
                                                        isPreviousCaretStopAtStart = true)),

    JUMP_TO_WORD_START(ApplicationBundle.message("combobox.item.jump.to.word.start"),
                       EditorCaretMovementOptions(isNextCaretStopAtStart = true,
                                                  isPreviousCaretStopAtStart = true)),

    JUMP_TO_WORD_END(ApplicationBundle.message("combobox.item.jump.to.word.end"),
                     EditorCaretMovementOptions(isNextCaretStopAtEnd = true,
                                                isPreviousCaretStopAtEnd = true)),

    JUMP_TO_NEIGHBORING_WORD(ApplicationBundle.message("combobox.item.jump.to.neighboring.word"),
                             EditorCaretMovementOptions(isNextCaretStopAtStart = true,
                                                        isPreviousCaretStopAtEnd = true)),

    STOP_AT_ALL_BOUNDARIES(ApplicationBundle.message("combobox.item.stop.at.all.word.boundaries"),
                           EditorCaretMovementOptions(isNextCaretStopAtStart = true,
                                                      isNextCaretStopAtEnd = true,
                                                      isPreviousCaretStopAtStart = true,
                                                      isPreviousCaretStopAtEnd = true));

    override fun toString(): String = title

    companion object {
      @JvmStatic
      fun forEditorSettings(editorSettings: EditorSettingsExternalizable) : Item {
        val options = EditorCaretMovementOptions.fromWordBoundarySettings(editorSettings)
        return WordBoundary.values().find { it.options == options } ?: STICK_TO_WORD_BOUNDARIES
      }
    }
  }
}
