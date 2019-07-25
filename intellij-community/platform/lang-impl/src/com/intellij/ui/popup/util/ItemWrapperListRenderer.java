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
package com.intellij.ui.popup.util;

import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ItemWrapperListRenderer extends ColoredListCellRenderer {
  private final JComponent myAccessory;
  private final Project myProject;

  public ItemWrapperListRenderer(Project project, JComponent accessory) {
    myAccessory = accessory;
    this.myProject = project;
  }

  @Override
  protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
    if (value instanceof ItemWrapper) {
      final ItemWrapper wrapper = (ItemWrapper)value;
      wrapper.setupRenderer(this, myProject, selected);
      if (myAccessory != null) {
        wrapper.updateAccessoryView(myAccessory);
      }
    }
  }
}
