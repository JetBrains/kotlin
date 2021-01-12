// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.usageView.impl;

import com.intellij.ide.DataManager;
import com.intellij.util.OpenSourceUtil;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public final class SelectInEditorHandler {
  public static void installKeyListener(final JComponent component) {
    component.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) selectInEditor(component);
      }
    });
  }

  public static void selectInEditor(final JComponent component) {
    OpenSourceUtil.openSourcesFrom(DataManager.getInstance().getDataContext(component), false);
  }

}
