// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.ui

import com.intellij.navigation.TargetPopupPresentation
import com.intellij.ui.components.JBLabel
import com.intellij.ui.speedSearch.SearchAwareRenderer
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.jetbrains.annotations.ApiStatus.Experimental
import java.awt.Component
import javax.swing.JList
import javax.swing.ListCellRenderer
import javax.swing.SwingConstants

@Experimental
internal abstract class TargetPresentationRightRenderer<T> : ListCellRenderer<T>, SearchAwareRenderer<T> {

  companion object {
    private val ourBorder = JBUI.Borders.emptyRight(UIUtil.getListCellHPadding())
  }

  protected abstract fun getPresentation(value: T): TargetPopupPresentation?

  private val component = JBLabel().apply {
    border = ourBorder
    horizontalTextPosition = SwingConstants.LEFT
    horizontalAlignment = SwingConstants.RIGHT // align icon to the right
  }

  final override fun getItemSearchString(item: T): String? = getPresentation(item)?.locationText

  final override fun getListCellRendererComponent(list: JList<out T>,
                                                  value: T,
                                                  index: Int,
                                                  isSelected: Boolean,
                                                  cellHasFocus: Boolean): Component {
    component.apply {
      text = ""
      icon = null
      background = UIUtil.getListBackground(isSelected)
      foreground = if (isSelected) UIUtil.getListSelectionForeground() else UIUtil.getInactiveTextColor()
      font = list.font
    }
    getPresentation(value)?.let { presentation ->
      presentation.rightText?.let { rightText ->
        component.text = rightText
        component.icon = presentation.rightIcon
      }
    }
    return component
  }
}
