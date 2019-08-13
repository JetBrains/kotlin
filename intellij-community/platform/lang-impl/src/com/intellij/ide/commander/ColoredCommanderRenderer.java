/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.ide.commander;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

final class ColoredCommanderRenderer extends ColoredListCellRenderer {
  private final CommanderPanel myCommanderPanel;

  ColoredCommanderRenderer(@NotNull final CommanderPanel commanderPanel) {
    myCommanderPanel = commanderPanel;
  }

  @Override
  public Component getListCellRendererComponent(final JList list, final Object value, final int index, boolean selected, boolean hasFocus){
    hasFocus = selected; // border around inactive items

    if (!myCommanderPanel.isActive()) {
      selected = false;
    }

    return super.getListCellRendererComponent(list, value, index, selected, hasFocus);
  }

  @Override
  protected void customizeCellRenderer(@NotNull final JList list, final Object value, final int index, final boolean selected, final boolean hasFocus) {
    Color color = UIUtil.getListForeground();
    SimpleTextAttributes attributes = null;
    String locationString = null;

    setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0)); // for separator, see below

    if (value instanceof NodeDescriptor) {
      final NodeDescriptor descriptor = (NodeDescriptor)value;
      setIcon(descriptor.getIcon());
      final Color elementColor = descriptor.getColor();

      if (elementColor != null) {
        color = elementColor;
      }

      if (descriptor instanceof AbstractTreeNode) {
        final AbstractTreeNode treeNode = (AbstractTreeNode)descriptor;
        final TextAttributesKey attributesKey = treeNode.getPresentation().getTextAttributesKey();

        if (attributesKey != null) {
          final TextAttributes textAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(attributesKey);

          if (textAttributes != null) attributes =  SimpleTextAttributes.fromTextAttributes(textAttributes);
        }
        locationString = treeNode.getPresentation().getLocationString();

        final PresentationData presentation = treeNode.getPresentation();
        if (presentation.hasSeparatorAbove() && !selected) {
          setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground()),
                                                       BorderFactory.createEmptyBorder(0, 0, 1, 0)));
        }
      }
    }

    if(attributes == null) attributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, color);
    final String text = value.toString();

    if (myCommanderPanel.isEnableSearchHighlighting()) {
      JList list1 = myCommanderPanel.getList();
      if (list1 != null) {
        SpeedSearchUtil.appendFragmentsForSpeedSearch(list1, text, attributes, selected, this);
      }
    }
    else {
      append(text != null ? text : "", attributes);
    }

    if (locationString != null && locationString.length() > 0) {
      append(" (" + locationString + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
    }
  }
}
