/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.reference.RefElement;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.openapi.vcs.FileStatus;
import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.LoadingNode;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

/**
 * @author Dmitry Batkovich
 */
class InspectionTreeCellRenderer extends ColoredTreeCellRenderer {
  private final InspectionTreeTailRenderer myTailRenderer;

  InspectionTreeCellRenderer(InspectionResultsView view) {
    myTailRenderer = new InspectionTreeTailRenderer(view.getGlobalInspectionContext()) {
      @Override
      protected void appendText(String text, SimpleTextAttributes attributes) {
        append(text, attributes);
      }

      @Override
      protected void appendText(String text) {
        append(text);
      }
    };
  }

  @Override
  public void customizeCellRenderer(@NotNull JTree tree,
                                    Object value,
                                    boolean selected,
                                    boolean expanded,
                                    boolean leaf,
                                    int row,
                                    boolean hasFocus) {
    if (value instanceof InspectionRootNode) {
      return;
    }
    if (value instanceof LoadingNode) {
      append(LoadingNode.getText());
      return;
    }
    InspectionTreeNode node = (InspectionTreeNode)value;

    append(node.getPresentableText(),
           patchMainTextAttrs(node, node.appearsBold()
                                    ? SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
                                    : getMainForegroundAttributes(node)));
    myTailRenderer.appendTailText(node);
    setIcon(node.getIcon(expanded));
  }

  private static SimpleTextAttributes patchMainTextAttrs(InspectionTreeNode node, SimpleTextAttributes attributes) {
    if (node.isExcluded()) {
      return attributes.derive(attributes.getStyle() | SimpleTextAttributes.STYLE_STRIKEOUT, null, null, null);
    }
    if (node instanceof SuppressableInspectionTreeNode && ((SuppressableInspectionTreeNode)node).isQuickFixAppliedFromView()) {
      return attributes.derive(-1, SimpleTextAttributes.GRAYED_ATTRIBUTES.getFgColor(), null, null);
    }
    if (!node.isValid()) {
      return attributes.derive(-1, FileStatus.IGNORED.getColor(), null, null);
    }
    return attributes;
  }


  private static SimpleTextAttributes getMainForegroundAttributes(InspectionTreeNode node) {
    SimpleTextAttributes foreground = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    if (node instanceof RefElementNode) {
      RefEntity refElement = ((RefElementNode)node).getElement();

      if (refElement instanceof RefElement) {
        refElement = ((RefElement)refElement).getContainingEntry();
        if (((RefElement)refElement).isEntry() && ((RefElement)refElement).isPermanentEntry()) {
          foreground = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.blue);
        }
      }
    }
    return foreground;
  }
}
