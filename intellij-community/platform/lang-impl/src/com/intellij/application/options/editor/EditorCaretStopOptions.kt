// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.ex.EditorSettingsExternalizable

internal data class EditorCaretStopOptions(val isNextCaretStopAtStart: Boolean = false,
                                           val isNextCaretStopAtEnd: Boolean = false,
                                           val isPreviousCaretStopAtStart: Boolean = false,
                                           val isPreviousCaretStopAtEnd: Boolean = false) {

  fun areWordBoundarySettingsModified(editorSettings: EditorSettingsExternalizable): Boolean =
    this != EditorCaretStopOptions.fromWordBoundarySettings(editorSettings)

  fun areLineBoundarySettingsModified(editorSettings: EditorSettingsExternalizable): Boolean =
    this != EditorCaretStopOptions.fromLineBoundarySettings(editorSettings)

  fun applyWordBoundarySettings(editorSettings: EditorSettingsExternalizable) {
    editorSettings.setCaretStopAtWordStart(true, isNextCaretStopAtStart)
    editorSettings.setCaretStopAtWordEnd(true, isNextCaretStopAtEnd)
    editorSettings.setCaretStopAtWordStart(false, isPreviousCaretStopAtStart)
    editorSettings.setCaretStopAtWordEnd(false, isPreviousCaretStopAtEnd)
  }

  fun applyLineBoundarySettings(editorSettings: EditorSettingsExternalizable) {
    editorSettings.setCaretStopAtLineStart(true, isNextCaretStopAtStart)
    editorSettings.setCaretStopAtLineEnd(true, isNextCaretStopAtEnd)
    editorSettings.setCaretStopAtLineStart(false, isPreviousCaretStopAtStart)
    editorSettings.setCaretStopAtLineEnd(false, isPreviousCaretStopAtEnd)
  }

  companion object {
    val SKIP = EditorCaretStopOptions()
    val STICK_ON_CURRENT = EditorCaretStopOptions(isNextCaretStopAtEnd = true, isPreviousCaretStopAtStart = true)
    val JUMP_TO_NEIGHBORING = EditorCaretStopOptions(isNextCaretStopAtStart = true, isPreviousCaretStopAtEnd = true)
    val JUMP_TO_START = EditorCaretStopOptions(isNextCaretStopAtStart = true, isPreviousCaretStopAtStart = true)
    val JUMP_TO_END = EditorCaretStopOptions(isNextCaretStopAtEnd = true, isPreviousCaretStopAtEnd = true)
    val STOP_AT_BOTH_BOUNDARIES = EditorCaretStopOptions(isNextCaretStopAtStart = true,
                                                         isNextCaretStopAtEnd = true,
                                                         isPreviousCaretStopAtStart = true,
                                                         isPreviousCaretStopAtEnd = true)

    fun fromWordBoundarySettings(editorSettings: EditorSettingsExternalizable): EditorCaretStopOptions =
      EditorCaretStopOptions(editorSettings.isCaretStopAtWordStart(true),
                             editorSettings.isCaretStopAtWordEnd(true),
                             editorSettings.isCaretStopAtWordStart(false),
                             editorSettings.isCaretStopAtWordEnd(false))

    fun fromLineBoundarySettings(editorSettings: EditorSettingsExternalizable): EditorCaretStopOptions =
      EditorCaretStopOptions(editorSettings.isCaretStopAtLineStart(true),
                             editorSettings.isCaretStopAtLineEnd(true),
                             editorSettings.isCaretStopAtLineStart(false),
                             editorSettings.isCaretStopAtLineEnd(false))
  }

  internal interface Item {
    val title: String
    val options: EditorCaretStopOptions

    companion object {
      internal inline fun <reified E> findMatchingItem(options: EditorCaretStopOptions): E?
        where E : Enum<E>,
              E : Item = enumValues<E>().find { it.options == options }
    }
  }

  internal enum class WordBoundary(override val title: String,
                                   override val options: EditorCaretStopOptions) : Item {
    STICK_TO_WORD_BOUNDARIES(ApplicationBundle.message("combobox.item.stick.to.word.boundaries"), STICK_ON_CURRENT),
    JUMP_TO_WORD_START(ApplicationBundle.message("combobox.item.jump.to.word.start"), JUMP_TO_NEIGHBORING),
    JUMP_TO_WORD_END(ApplicationBundle.message("combobox.item.jump.to.word.end"), JUMP_TO_START),
    JUMP_TO_NEIGHBORING_WORD(ApplicationBundle.message("combobox.item.jump.to.neighboring.word"), JUMP_TO_END),
    STOP_AT_ALL_WORD_BOUNDARIES(ApplicationBundle.message("combobox.item.stop.at.all.word.boundaries"), STOP_AT_BOTH_BOUNDARIES);

    override fun toString(): String = title

    companion object {
      @JvmStatic
      fun forEditorSettings(editorSettings: EditorSettingsExternalizable): WordBoundary {
        val options = EditorCaretStopOptions.fromWordBoundarySettings(editorSettings)
        return Item.findMatchingItem(options) ?: STICK_TO_WORD_BOUNDARIES
      }
    }
  }

  internal enum class LineBoundary(override val title: String,
                                   override val options: EditorCaretStopOptions) : Item {
    SKIP_LINE_BREAK(ApplicationBundle.message("combobox.item.proceed.to.word.boundary"), SKIP),
    STAY_ON_CURRENT_LINE(ApplicationBundle.message("combobox.item.stay.on.current.line"), STICK_ON_CURRENT),
    JUMP_TO_NEIGHBORING_LINE(ApplicationBundle.message("combobox.item.jump.to.neighboring.line"), JUMP_TO_NEIGHBORING),
    JUMP_TO_LINE_START(ApplicationBundle.message("combobox.item.stop.at.line.start"), JUMP_TO_START),
    JUMP_TO_LINE_END(ApplicationBundle.message("combobox.item.stop.at.line.end"), JUMP_TO_END),
    STOP_AT_BOTH_LINE_BOUNDARIES(ApplicationBundle.message("combobox.item.stop.at.both.line.ends"), STOP_AT_BOTH_BOUNDARIES);

    override fun toString(): String = title

    companion object {
      @JvmStatic
      fun forEditorSettings(editorSettings: EditorSettingsExternalizable): LineBoundary {
        val options = EditorCaretStopOptions.fromLineBoundarySettings(editorSettings)
        return Item.findMatchingItem(options) ?: STOP_AT_BOTH_LINE_BOUNDARIES
      }
    }
  }
}
