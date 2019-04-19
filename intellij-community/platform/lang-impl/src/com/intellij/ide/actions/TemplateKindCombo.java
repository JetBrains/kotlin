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
package com.intellij.ide.actions;

import com.intellij.ide.ui.UISettings;
import com.intellij.openapi.actionSystem.CustomShortcutSet;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.util.Trinity;
import com.intellij.ui.ComboboxSpeedSearch;
import com.intellij.ui.ComboboxWithBrowseButton;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.ui.SpeedSearchComparator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class TemplateKindCombo extends ComboboxWithBrowseButton {
  public TemplateKindCombo() {
    //noinspection unchecked
    getComboBox().setRenderer(new ListCellRendererWrapper() {
      @Override
      public void customize(final JList list, final Object value, final int index, final boolean selected, final boolean cellHasFocus) {
        if (value instanceof Trinity) {
          setText((String)((Trinity)value).first);
          setIcon ((Icon)((Trinity)value).second);
        }
      }
    });

    new ComboboxSpeedSearch(getComboBox()) {
      @Override
      protected String getElementText(Object element) {
        if (element instanceof Trinity) {
          return (String)((Trinity)element).first;
        }
        return null;
      }
    }.setComparator(new SpeedSearchComparator(true));
    setButtonListener(null);
  }

  public void addItem(@NotNull String presentableName, @Nullable Icon icon, @NotNull String templateName) {
    //noinspection unchecked
    getComboBox().addItem(new Trinity<>(presentableName, icon, templateName));
  }

  @NotNull
  public String getSelectedName() {
    //noinspection unchecked
    final Trinity<String, Icon, String> trinity = (Trinity<String, Icon, String>)getComboBox().getSelectedItem();
    if (trinity == null) {
      // design time
      return "yet_unknown";
    }
    return trinity.third;
  }

  public void setSelectedName(@Nullable String name) {
    if (name == null) return;
    ComboBoxModel model = getComboBox().getModel();
    for (int i = 0, n = model.getSize(); i < n; i++) {
      //noinspection unchecked
      Trinity<String, Icon, String> trinity = (Trinity<String, Icon, String>)model.getElementAt(i);
      if (name.equals(trinity.third)) {
        getComboBox().setSelectedItem(trinity);
        return;
      }
    }
  }

  public void registerUpDownHint(JComponent component) {
    DumbAwareAction.create(e -> {
      if (e.getInputEvent() instanceof KeyEvent) {
        int code = ((KeyEvent)e.getInputEvent()).getKeyCode();
        scrollBy(code == KeyEvent.VK_DOWN ? 1 : code == KeyEvent.VK_UP ? -1 : 0);
      }
    }).registerCustomShortcutSet(new CustomShortcutSet(KeyEvent.VK_UP, KeyEvent.VK_DOWN), component);
  }

  private void scrollBy(int delta) {
    final int size = getComboBox().getModel().getSize();
    if (delta == 0 || size == 0) return;
    int next = getComboBox().getSelectedIndex() + delta;
    if (next < 0 || next >= size) {
      if (!UISettings.getInstance().getCycleScrolling()) {
        return;
      }
      next = (next + size) % size;
    }
    getComboBox().setSelectedIndex(next);
  }

  /**
   * @param listener pass {@code null} to hide browse button
   */
  public void setButtonListener(@Nullable ActionListener listener) {
    getButton().setVisible(listener != null);
    if (listener != null) {
      addActionListener(listener);
    }
  }

  public void clear() {
    getComboBox().removeAllItems();
  }
}
