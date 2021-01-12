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

package com.intellij.refactoring.util;

import com.intellij.openapi.wm.IdeFocusManager;

import javax.swing.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * @author yole
*/
public class RadioUpDownListener extends KeyAdapter {
  private final JRadioButton[] myRadioButtons;

  public RadioUpDownListener(final JRadioButton... radioButtons) {
    myRadioButtons = radioButtons;
    for (JRadioButton radioButton : radioButtons) {
      radioButton.addKeyListener(this);
    }
  }

  @Override
  public void keyPressed(final KeyEvent e) {
    final int selected = getSelected();
    if (selected != -1) {
      if (e.getKeyCode() == KeyEvent.VK_UP) {
        up(selected, selected);
        e.consume();
      }
      else if (e.getKeyCode() == KeyEvent.VK_DOWN) {
        down(selected, selected);
        e.consume();
      }
    }
  }

  private void down(int selected, int stop) {
    int newIdx = selected + 1;
    if (newIdx > myRadioButtons.length - 1) newIdx = 0;
    if (!click(myRadioButtons[newIdx]) && stop != newIdx) {
      down(newIdx, selected);
    }
  }

  private void up(int selected, int stop) {
    int newIdx = selected - 1;
    if (newIdx < 0) newIdx = myRadioButtons.length - 1;
    if (!click(myRadioButtons[newIdx]) && stop != newIdx) {
      up(newIdx, selected);
    }
  }

  private int getSelected() {
    for (int i = 0; i < myRadioButtons.length; i++) {
      if (myRadioButtons[i].isSelected()) {
        return i;
      }
    }
    return -1;
  }

  private static boolean click(final JRadioButton button) {
    if (button.isEnabled() && button.isVisible()) {
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(button, true));
      button.doClick();
      return true;
    }
    return false;
  }
}
