// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.editor

import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.editor.actions.CaretStopBoundary
import com.intellij.openapi.editor.actions.CaretStopOptions
import com.intellij.openapi.editor.actions.CaretStopOptionsTransposed
import com.intellij.openapi.util.SystemInfo
import com.intellij.ui.ColoredListCellRenderer
import com.intellij.ui.SimpleTextAttributes
import javax.swing.JList

internal interface EditorCaretStopPolicyItem {
  val title: String
  val caretStopBoundary: CaretStopBoundary
  val osDefault: OsDefault

  companion object {
    internal inline fun <reified E> findMatchingItem(caretStopBoundary: CaretStopBoundary): E
      where E : Enum<E>,
            E : EditorCaretStopPolicyItem = enumValues<E>().find { it.caretStopBoundary == caretStopBoundary }
                                            ?: checkNotNull(enumValues<E>().find { it.osDefault.isCurrentOsDefault })
    internal fun String.appendHint(hint: String): String =
      if (hint.isBlank()) this else "$this ($hint)"
  }

  class ListItemRenderer : ColoredListCellRenderer<EditorCaretStopPolicyItem?>() {
    init {
      ipad.bottom = 0
      ipad.top = 0
      ipad.right = 0
    }

    override fun customizeCellRenderer(list: JList<out EditorCaretStopPolicyItem?>,
                                       item: EditorCaretStopPolicyItem?,
                                       index: Int,
                                       selected: Boolean,
                                       hasFocus: Boolean) {
      if (item == null) return
      append(item.title)
      val hint = item.osDefault.hint
      if (hint.isNotBlank()) {
        append("  $hint", SimpleTextAttributes.GRAYED_ATTRIBUTES)
      }
    }
  }

  enum class OsDefault(val isCurrentOsDefault: Boolean = false,
                       open val hint: String = "") {
    UNIX(SystemInfo.isUnix,
         hint = when {
           SystemInfo.isLinux -> ApplicationBundle.message("combobox.item.hint.os.default.linux")
           SystemInfo.isMac -> ApplicationBundle.message("combobox.item.hint.os.default.mac")
           else -> ApplicationBundle.message("combobox.item.hint.os.default.unix")
         }),
    WINDOWS(SystemInfo.isWindows,
            hint = ApplicationBundle.message("combobox.item.hint.os.default.windows")),
    NONE;
  }

  enum class WordBoundary(override val title: String,
                          override val caretStopBoundary: CaretStopBoundary,
                          override val osDefault: OsDefault = OsDefault.NONE) : EditorCaretStopPolicyItem {
    STICK_TO_WORD_BOUNDARIES(ApplicationBundle.message("combobox.item.stick.to.word.boundaries"), CaretStopBoundary.CURRENT,
                             OsDefault.UNIX),
    JUMP_TO_WORD_START(ApplicationBundle.message("combobox.item.jump.to.word.start"), CaretStopBoundary.START,
                       OsDefault.WINDOWS),
    JUMP_TO_WORD_END(ApplicationBundle.message("combobox.item.jump.to.word.end"), CaretStopBoundary.END),
    JUMP_TO_NEIGHBORING_WORD(ApplicationBundle.message("combobox.item.jump.to.neighboring.word"), CaretStopBoundary.NEIGHBOR),
    STOP_AT_ALL_WORD_BOUNDARIES(ApplicationBundle.message("combobox.item.stop.at.all.word.boundaries"), CaretStopBoundary.BOTH);

    override fun toString(): String = title.appendHint(osDefault.hint)
    companion object {
      @JvmStatic
      fun itemForPolicy(caretStopOptions: CaretStopOptions): WordBoundary =
        findMatchingItem(CaretStopOptionsTransposed.fromCaretStopOptions(caretStopOptions).wordBoundary)
    }
  }

  enum class LineBoundary(override val title: String,
                          override val caretStopBoundary: CaretStopBoundary,
                          override val osDefault: OsDefault = OsDefault.NONE) : EditorCaretStopPolicyItem {
    SKIP_LINE_BREAK(ApplicationBundle.message("combobox.item.proceed.to.word.boundary"), CaretStopBoundary.NONE,
                    OsDefault.UNIX),
    STOP_AT_BOTH_LINE_BOUNDARIES(ApplicationBundle.message("combobox.item.stop.at.both.line.ends"), CaretStopBoundary.BOTH,
                                 OsDefault.WINDOWS),
    STAY_ON_CURRENT_LINE(ApplicationBundle.message("combobox.item.stay.on.current.line"), CaretStopBoundary.CURRENT),
    JUMP_TO_NEIGHBORING_LINE(ApplicationBundle.message("combobox.item.jump.to.neighboring.line"), CaretStopBoundary.NEIGHBOR),
    JUMP_TO_LINE_START(ApplicationBundle.message("combobox.item.stop.at.line.start"), CaretStopBoundary.START),
    JUMP_TO_LINE_END(ApplicationBundle.message("combobox.item.stop.at.line.end"), CaretStopBoundary.END);

    override fun toString(): String = title.appendHint(osDefault.hint)
    companion object {
      @JvmStatic
      fun itemForPolicy(caretStopOptions: CaretStopOptions): LineBoundary =
        findMatchingItem(CaretStopOptionsTransposed.fromCaretStopOptions(caretStopOptions).lineBoundary)
    }
  }
}
