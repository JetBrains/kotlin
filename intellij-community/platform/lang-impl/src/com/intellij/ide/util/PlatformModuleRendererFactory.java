// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.util;

import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.*;

/**
 * @author yole
 */
public class PlatformModuleRendererFactory extends ModuleRendererFactory {
  @Override
  public DefaultListCellRenderer getModuleRenderer() {
    return new PlatformModuleRenderer();
  }

  @Override
  public boolean rendersLocationString() {
    return true;
  }

  public static class PlatformModuleRenderer extends DefaultListCellRenderer {
    @Override
    public Component getListCellRendererComponent(final JList list,
                                                  final Object value,
                                                  final int index,
                                                  final boolean isSelected,
                                                  final boolean cellHasFocus) {
      final Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

      String text = "";
      if (value instanceof NavigationItem) {
        final ItemPresentation presentation = ((NavigationItem)value).getPresentation();
        if (presentation != null) {
          String containerText = presentation.getLocationString();
          if (!StringUtil.isEmpty(containerText)) {
            text = " " + containerText;
          }
        }
      }


      setText(text);
      setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 2));
      setHorizontalTextPosition(SwingConstants.LEFT);
      setBackground(isSelected ? UIUtil.getListSelectionBackground(true) : UIUtil.getListBackground());
      setForeground(isSelected ? UIUtil.getListSelectionForeground() : UIUtil.getInactiveTextColor());
      return component;
    }
  }
}
