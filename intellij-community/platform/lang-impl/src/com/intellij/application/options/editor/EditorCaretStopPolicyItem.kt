// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.actions.CaretStopOptions
import com.intellij.openapi.editor.actions.CaretStopPolicy
import com.intellij.openapi.util.SystemInfo

internal interface EditorCaretStopPolicyItem {
  val title: String
  val caretStopPolicy: CaretStopPolicy
  val isOsDefault: Boolean

  val osDefaultHint: String get () = if (isOsDefault) " ($currentOsDefaultString)" else ""

  companion object {
    val currentOsDefaultString: String = when {
      SystemInfo.isWindows -> ApplicationBundle.message("combobox.item.hint.os.default.windows")
      SystemInfo.isLinux -> ApplicationBundle.message("combobox.item.hint.os.default.linux")
      SystemInfo.isMac -> ApplicationBundle.message("combobox.item.hint.os.default.mac")
      else -> ApplicationBundle.message("combobox.item.hint.os.default.unix")
    }

    internal inline fun <reified E> findMatchingItem(caretStopPolicy: CaretStopPolicy): E
      where E : Enum<E>,
            E : EditorCaretStopPolicyItem = enumValues<E>().find { it.caretStopPolicy == caretStopPolicy }
                                            ?: checkNotNull(enumValues<E>().find { it.isOsDefault })
  }

  enum class WordBoundary(override val title: String,
                          override val caretStopPolicy: CaretStopPolicy,
                          override val isOsDefault: Boolean = false) : EditorCaretStopPolicyItem {
    STICK_TO_WORD_BOUNDARIES(ApplicationBundle.message("combobox.item.stick.to.word.boundaries"), CaretStopPolicy.CURRENT,
                             isOsDefault = SystemInfo.isUnix),
    JUMP_TO_WORD_START(ApplicationBundle.message("combobox.item.jump.to.word.start"), CaretStopPolicy.NEIGHBOR,
                       isOsDefault = SystemInfo.isWindows),
    JUMP_TO_WORD_END(ApplicationBundle.message("combobox.item.jump.to.word.end"), CaretStopPolicy.START),
    JUMP_TO_NEIGHBORING_WORD(ApplicationBundle.message("combobox.item.jump.to.neighboring.word"), CaretStopPolicy.END),
    STOP_AT_ALL_WORD_BOUNDARIES(ApplicationBundle.message("combobox.item.stop.at.all.word.boundaries"), CaretStopPolicy.BOTH);

    override fun toString(): String = title + osDefaultHint
    companion object {
      @JvmStatic
      fun itemForPolicy(caretStopOptions: CaretStopOptions): WordBoundary = findMatchingItem(caretStopOptions.wordBoundaryPolicy)
    }
  }

  enum class LineBoundary(override val title: String,
                          override val caretStopPolicy: CaretStopPolicy,
                          override val isOsDefault: Boolean = false) : EditorCaretStopPolicyItem {
    STAY_ON_CURRENT_LINE(ApplicationBundle.message("combobox.item.stay.on.current.line"), CaretStopPolicy.CURRENT),
    JUMP_TO_NEIGHBORING_LINE(ApplicationBundle.message("combobox.item.jump.to.neighboring.line"), CaretStopPolicy.NEIGHBOR),
    JUMP_TO_LINE_START(ApplicationBundle.message("combobox.item.stop.at.line.start"), CaretStopPolicy.START),
    JUMP_TO_LINE_END(ApplicationBundle.message("combobox.item.stop.at.line.end"), CaretStopPolicy.END),
    STOP_AT_BOTH_LINE_BOUNDARIES(ApplicationBundle.message("combobox.item.stop.at.both.line.ends"), CaretStopPolicy.BOTH,
                                 isOsDefault = SystemInfo.isWindows),
    SKIP_LINE_BREAK(ApplicationBundle.message("combobox.item.proceed.to.word.boundary"), CaretStopPolicy.NONE,
                    isOsDefault = SystemInfo.isUnix);

    override fun toString(): String = title + osDefaultHint
    companion object {
      @JvmStatic
      fun itemForPolicy(caretStopOptions: CaretStopOptions): LineBoundary = findMatchingItem(caretStopOptions.lineBoundaryPolicy)
    }
  }
}
