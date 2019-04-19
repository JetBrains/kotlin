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
package com.intellij.refactoring.introduce.inplace;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import java.awt.event.*;

public class KeyboardComboSwitcher {

  public static void setupActions(final JComboBox comboBox, final Project project) {
    final boolean toggleStrategy = !UIUtil.isUnderAquaLookAndFeel();
    final boolean[] moveFocusBack = new boolean[] {true};
    comboBox.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (!moveFocusBack[0]) {
          moveFocusBack[0] = true;
          return;
        }

        if (toggleStrategy) {
          final int size = comboBox.getModel().getSize();
          int next = comboBox.getSelectedIndex() + 1;
          if (size > 0) {
            if (next < 0 || next >= size) {
              if (!UISettings.getInstance().getCycleScrolling()) {
                return;
              }
              next = (next + size) % size;
            }
            comboBox.setSelectedIndex(next);
          }
          ToolWindowManager.getInstance(project).activateEditorComponent();
        }
        else {
          JBPopupFactory popupFactory = JBPopupFactory.getInstance();
          boolean fromTheSameBalloon = popupFactory.getParentBalloonFor(e.getComponent()) == popupFactory.getParentBalloonFor(e.getOppositeComponent());
          if (!fromTheSameBalloon) {
            comboBox.showPopup();
          }
        }
      }
    });
    comboBox.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        moveFocusBack[0] = false;
      }
    });
    comboBox.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        moveFocusBack[0] = true;
        if (!toggleStrategy && e.getKeyCode() == KeyEvent.VK_ESCAPE && e.getModifiers() == 0) {
          ToolWindowManager.getInstance(project).activateEditorComponent();
        }
      }
    });
    comboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        moveFocusBack[0] = true;
        if (!project.isDisposed()) {
          ToolWindowManager.getInstance(project).activateEditorComponent();
        }
      }
    });
  }
}
