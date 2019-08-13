// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.todo;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.todo.nodes.TodoItemNode;
import com.intellij.ui.HighlightableCellRenderer;
import com.intellij.ui.HighlightedRegion;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.List;

public class MultiLineTodoRenderer extends JPanel implements TreeCellRenderer {
  private static final int MAX_DISPLAYED_LINES = 10;

  private final HighlightableCellRenderer myPrefixRenderer;
  private final HighlightableCellRenderer[] myLineRenderers = new HighlightableCellRenderer[MAX_DISPLAYED_LINES];
  private final JLabel myMoreLabel;

  public MultiLineTodoRenderer() {
    super(new GridBagLayout());
    GridBagConstraints c = new GridBagConstraints();
    add(myPrefixRenderer = new HighlightableCellRenderer(), c);
    c.gridx = 1;
    c.weightx = 1;
    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.WEST;
    for (int i = 0; i < MAX_DISPLAYED_LINES; i++) {
      c.gridy = i;
      add(myLineRenderers[i] = new HighlightableCellRenderer(), c);
    }
    c.gridy++;
    add(myMoreLabel = new JLabel(IdeBundle.message("node.todo.more.items")), c);
  }

  @Override
  public Component getTreeCellRendererComponent(JTree tree,
                                                Object value,
                                                boolean selected,
                                                boolean expanded,
                                                boolean leaf,
                                                int row,
                                                boolean hasFocus) {
    TodoItemNode node = (TodoItemNode)((DefaultMutableTreeNode)value).getUserObject();
    String text = value.toString();
    int parenPos = text.indexOf(')');
    int contentStartPos = (parenPos >= 0 && parenPos < (text.length() - 1)) ? parenPos + 2 : 0;
    myPrefixRenderer.getTreeCellRendererComponent(tree, text.substring(0, contentStartPos), selected, expanded, leaf, row, hasFocus);
    myPrefixRenderer.setIcon(node.getIcon());

    List<HighlightedRegionProvider> additionalLines = node.getAdditionalLines();
    for (int i = 0; i < MAX_DISPLAYED_LINES; i++) {
      if (i > additionalLines.size()) {
        myLineRenderers[i].setVisible(false);
      }
      else {
        myLineRenderers[i].setVisible(true);
        myLineRenderers[i].getTreeCellRendererComponent(tree, i == 0 ? text.substring(contentStartPos) : additionalLines.get(i - 1),
                                                        selected, expanded, leaf, row, hasFocus);
        HighlightedRegionProvider provider = i == 0 ? node : additionalLines.get(i - 1);
        for (HighlightedRegion region : provider.getHighlightedRegions()) {
          myLineRenderers[i].addHighlighter(region.startOffset - (i == 0 ? contentStartPos : 0),
                                            region.endOffset - (i == 0 ? contentStartPos : 0),
                                            region.textAttributes);
        }
      }
    }
    myMoreLabel.setVisible(additionalLines.size() >= MAX_DISPLAYED_LINES);
    return this;
  }
}
