/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.usageView.impl;

import com.intellij.ide.DataManager;
import com.intellij.util.OpenSourceUtil;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author dyoma
 */
public class SelectInEditorHandler {
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
