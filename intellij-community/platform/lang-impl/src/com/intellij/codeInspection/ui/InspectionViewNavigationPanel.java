// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.ui;

import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.scale.JBUIScale;
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
      myLinks.add(Box.createVerticalStrut(JBUIScale.scale(10)));
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
