// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable

internal data class EditorCaretMovementOptions(val isMoveToNextWordCaretStopAtWordStart: Boolean = false,
                                               val isMoveToNextWordCaretStopAtWordEnd: Boolean = false,
                                               val isMoveToPreviousWordCaretStopAtWordStart: Boolean = false,
                                               val isMoveToPreviousWordCaretStopAtWordEnd: Boolean = false) {

  private constructor(editorSettings: EditorSettingsExternalizable) : this(editorSettings.isMoveToWordCaretStopAtWordStart(true),
                                                                           editorSettings.isMoveToWordCaretStopAtWordEnd(true),
                                                                           editorSettings.isMoveToWordCaretStopAtWordStart(false),
                                                                           editorSettings.isMoveToWordCaretStopAtWordEnd(false))

  fun isModified(editorSettings: EditorSettingsExternalizable): Boolean =
    this != EditorCaretMovementOptions(editorSettings)

  fun apply(editorSettings: EditorSettingsExternalizable) {
    editorSettings.setMoveToWordCaretStopAtWordStart(true, isMoveToNextWordCaretStopAtWordStart)
    editorSettings.setMoveToWordCaretStopAtWordEnd(true, isMoveToNextWordCaretStopAtWordEnd)
    editorSettings.setMoveToWordCaretStopAtWordStart(false, isMoveToPreviousWordCaretStopAtWordStart)
    editorSettings.setMoveToWordCaretStopAtWordEnd(false, isMoveToPreviousWordCaretStopAtWordEnd)
  }

  internal enum class Item(val title: String,
                           val options: EditorCaretMovementOptions) {
    STICK_TO_WORD_BOUNDARIES(ApplicationBundle.message("combobox.item.stick.to.word.boundaries"),
                             EditorCaretMovementOptions(isMoveToNextWordCaretStopAtWordEnd = true,
                                                        isMoveToPreviousWordCaretStopAtWordStart = true)),

    JUMP_TO_WORD_START(ApplicationBundle.message("combobox.item.jump.to.word.start"),
                       EditorCaretMovementOptions(isMoveToNextWordCaretStopAtWordStart = true,
                                                  isMoveToPreviousWordCaretStopAtWordStart = true)),

    JUMP_TO_WORD_END(ApplicationBundle.message("combobox.item.jump.to.word.end"),
                     EditorCaretMovementOptions(isMoveToNextWordCaretStopAtWordEnd = true,
                                                isMoveToPreviousWordCaretStopAtWordEnd = true)),

    JUMP_TO_NEIGHBORING_WORD(ApplicationBundle.message("combobox.item.jump.to.neighboring.word"),
                             EditorCaretMovementOptions(isMoveToNextWordCaretStopAtWordStart = true,
                                                        isMoveToPreviousWordCaretStopAtWordEnd = true)),

    STOP_AT_ALL_BOUNDARIES(ApplicationBundle.message("combobox.item.stop.at.all.word.boundaries"),
                           EditorCaretMovementOptions(isMoveToNextWordCaretStopAtWordStart = true,
                                                      isMoveToNextWordCaretStopAtWordEnd = true,
                                                      isMoveToPreviousWordCaretStopAtWordStart = true,
                                                      isMoveToPreviousWordCaretStopAtWordEnd = true));

    override fun toString(): String = title

    companion object {
      @JvmStatic
      fun forEditorSettings(editorSettings: EditorSettingsExternalizable) : Item {
        val options = EditorCaretMovementOptions(editorSettings)
        return Item.values().find { it.options == options } ?: STICK_TO_WORD_BOUNDARIES
      }
    }
  }
}
