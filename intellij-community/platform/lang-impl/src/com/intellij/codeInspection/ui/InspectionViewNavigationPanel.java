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

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Dmitry Batkovich
 */
public class InspectionViewNavigationPanel extends JPanel implements InspectionTreeLoadingProgressAware {
  private final InspectionTreeNode myNode;
  private final InspectionTree myTree;
  private final JPanel myLinks;
  private int myShownChildrenCount;

  public InspectionViewNavigationPanel(InspectionTreeNode node, InspectionTree tree) {
    myNode = node;
    myTree = tree;
    setLayout(new BorderLayout());
    setBorder(JBUI.Borders.empty(18, 12, 0, 0));
    final String titleLabelText = getTitleText(true);
    add(new JBLabel(titleLabelText), BorderLayout.NORTH);
    myLinks = new JPanel();
    myLinks.setLayout(new BoxLayout(myLinks, BoxLayout.Y_AXIS));

    add(BorderLayout.CENTER, myLinks);
    resetChildrenNavigation();
  }

  @Override
  public void updateLoadingProgress() {
    resetChildrenAndRepaint();
  }

  @Override
  public void treeLoaded() {
    resetChildrenAndRepaint();
  }

  @NotNull
  public static String getTitleText(boolean addColon) {
    return "Select inspection to see problems" + (addColon ? ":" : ".");
  }

  private void resetChildrenNavigation() {
    final int currentChildrenCount = myNode.getChildCount();
    if (myShownChildrenCount != currentChildrenCount) {
      myLinks.removeAll();
      myLinks.add(Box.createVerticalStrut(JBUI.scale(10)));
      for (int i = 0; i < currentChildrenCount; i++) {
        final InspectionTreeNode child = myNode.getChildAt(i);
        final LinkLabel link = new LinkLabel(child.getPresentableText(), null) {
          @Override
          public void doClick() {
            myTree.selectNode(child);
          }
        };
        link.setBorder(JBUI.Borders.empty(1, 17, 3, 1));
        myLinks.add(link);
      }
      myShownChildrenCount = currentChildrenCount;
    }
  }

  private void resetChildrenAndRepaint() {
    resetChildrenNavigation();
    revalidate();
    repaint();
  }
}
