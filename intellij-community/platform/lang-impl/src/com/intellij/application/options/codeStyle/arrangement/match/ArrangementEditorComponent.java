/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.application.options.codeStyle.arrangement.match;

import com.intellij.application.options.codeStyle.arrangement.animation.ArrangementAnimationManager;
import com.intellij.application.options.codeStyle.arrangement.ui.ArrangementEditorAware;
import com.intellij.application.options.codeStyle.arrangement.ui.ArrangementRepresentationAware;
import com.intellij.application.options.codeStyle.arrangement.util.CalloutBorder;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author Denis Zhdanov
 */
public class ArrangementEditorComponent implements ArrangementRepresentationAware, ArrangementAnimationManager.Callback,
                                                   ArrangementEditorAware
{

  @NotNull private final ArrangementMatchingRulesControl myList;
  @NotNull private final JComponent                      myComponent;
  @NotNull private final Insets                          myBorderInsets;
  @NotNull private final ArrangementMatchingRuleEditor   myEditor;

  private final int myRow;

  public ArrangementEditorComponent(@NotNull ArrangementMatchingRulesControl list, int row, @NotNull ArrangementMatchingRuleEditor editor) {
    myList = list;
    myRow = row;
    myEditor = editor;
    JPanel borderPanel = new JPanel(new BorderLayout()) {
      @Override
      public String toString() {
        return "callout border panel for " + myEditor;
      }
    };
    borderPanel.setBackground(UIUtil.getListBackground());
    borderPanel.add(editor);
    CalloutBorder border = new CalloutBorder();
    borderPanel.setBorder(border);
    myBorderInsets = border.getBorderInsets(borderPanel);
    myComponent = borderPanel;
    myList.repaintRows(myRow, myList.getModel().getSize() - 1, true);
    //myComponent = new ArrangementAnimationPanel(borderPanel, true, false);
  }

  @NotNull
  @Override
  public JComponent getComponent() {
    return myComponent;
  }

  public void expand() {
    //new ArrangementAnimationManager(myComponent, this).startAnimation();
  }

  @Override
  public void onAnimationIteration(boolean finished) {
    myList.repaintRows(myRow, myList.getModel().getSize() - 1, false);
  }

  public void applyAvailableWidth(int width) {
    myEditor.applyAvailableWidth(width - myBorderInsets.left - myBorderInsets.right);
  }
}
