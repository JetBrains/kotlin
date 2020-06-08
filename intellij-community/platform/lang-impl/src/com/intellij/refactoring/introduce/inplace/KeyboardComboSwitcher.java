// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.introduce.inplace;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.wm.ToolWindowManager;

import javax.swing.*;
import java.awt.event.*;

public final class KeyboardComboSwitcher {

  public static void setupActions(final JComboBox comboBox, final Project project) {
    final Ref<Boolean> moveFocusBack = Ref.create(true);
    comboBox.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        if (!moveFocusBack.get()) {
          moveFocusBack.set(true);
          return;
        }

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
    });
    comboBox.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseEntered(MouseEvent e) {
        moveFocusBack.set(false);
      }
    });
    comboBox.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        moveFocusBack.set(true);
      }
    });
    comboBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        moveFocusBack.set(true);
        if (!project.isDisposed()) {
          ToolWindowManager.getInstance(project).activateEditorComponent();
        }
      }
    });
  }
}
