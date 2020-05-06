// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.console

import com.intellij.ide.IdeBundle
import com.intellij.openapi.application.ApplicationBundle
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.encoding.ChooseFileEncodingAction.NO_ENCODING
import com.intellij.openapi.vfs.encoding.EncodingManager
import com.intellij.ui.CollectionComboBoxModel
import com.intellij.ui.SeparatorWithText
import com.intellij.ui.SimpleListCellRenderer
import java.awt.Component
import java.nio.charset.Charset
import javax.swing.JList

class ConsoleEncodingComboBox : ComboBox<ConsoleEncodingComboBox.EncodingItem>() {
  interface EncodingItem

  private data class LabelItem(val label: String) : EncodingItem

  private interface SelectableItem : EncodingItem

  private data class CharsetItem(val charset: Charset) : SelectableItem {
    override fun toString(): String =
      charset.displayName()
  }

  private object NoEncodingItem : SelectableItem {
    override fun toString(): String =
      IdeBundle.message("encoding.name.system.default", CharsetToolkit.getDefaultSystemCharset().displayName())
  }

  init {
    model = object : CollectionComboBoxModel<EncodingItem>() {
      /**
       * don't allow selecting of non-selectable items
       */
      override fun setSelectedItem(item: Any?) {
        if (item !is SelectableItem) {
          return
        }
        super.setSelectedItem(item)
      }
    }

    renderer = object : SimpleListCellRenderer<EncodingItem>() {
      /**
       * replace LabelItem with separator
       */
      override fun getListCellRendererComponent(list: JList<out EncodingItem>?,
                                                value: EncodingItem,
                                                index: Int,
                                                selected: Boolean,
                                                hasFocus: Boolean): Component {
        return if (value is LabelItem) {
          val separator = SeparatorWithText()
          separator.caption = StringUtil.shortenTextWithEllipsis(value.label, 40, 3)
          separator.setCaptionCentered(false)
          separator
        }
        else {
          super.getListCellRendererComponent(list, value, index, selected, hasFocus)
        }
      }

      override fun customize(list: JList<out EncodingItem>, value: EncodingItem, index: Int, selected: Boolean, hasFocus: Boolean) {
        text = value.toString()
      }
    }
  }

  private val listModel: CollectionComboBoxModel<EncodingItem>
    get() = model as CollectionComboBoxModel<EncodingItem>

  /**
   * Construct combobox model and reset item
   * Model:
   * ```
   *    Use system encoding
   *    --- Favorites
   *    Favorite 1
   *    ...
   *    Favorite n
   *    --- More
   *    Charset 1
   *    ...
   *    Charset n
   * ```
   */
  fun reset(charset: Charset) {
    val encodingManager = EncodingManager.getInstance()
    val favorites = encodingManager.favorites.map { CharsetItem(it) }
    val available = Charset.availableCharsets().values.map { CharsetItem(it) }

    listModel.add(NoEncodingItem)
    listModel.add(LabelItem(ApplicationBundle.message("combobox.console.favorites.separator.label")))
    listModel.add(favorites)
    listModel.add(LabelItem(ApplicationBundle.message("combobox.console.more.separator.label")))
    listModel.add(available)

    listModel.selectedItem = if (charset == NO_ENCODING) {
      NoEncodingItem
    } else {
      CharsetItem(charset)
    }
  }

  fun getSelectedCharset(): Charset {
    return when (selectedItem) {
      is CharsetItem -> (selectedItem as CharsetItem).charset
      is NoEncodingItem -> NO_ENCODING
      else -> {
        LOG.error("Encoding should be either NO_ENCODING or an actual Charset. Got $selectedItem")
        NO_ENCODING
      }
    }
  }

  fun getSelectedCharsetName(): String {
    val selectedCharset = getSelectedCharset()
    return if (selectedCharset === NO_ENCODING) {
      ""
    } else {
      selectedCharset.name()
    }
  }
}

private val LOG = Logger.getInstance("#" + ConsoleEncodingComboBox::class.java.`package`.name)

