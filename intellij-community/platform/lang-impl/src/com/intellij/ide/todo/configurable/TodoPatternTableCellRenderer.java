/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package com.intellij.ide.todo.configurable;

import com.intellij.psi.search.TodoPattern;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.List;

final class TodoPatternTableCellRenderer extends DefaultTableCellRenderer {
  private final List<? extends TodoPattern> myPatterns;

  TodoPatternTableCellRenderer(List<? extends TodoPattern> patterns) {
    myPatterns = patterns;
  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    TodoPattern pattern = myPatterns.get(row);
    if (isSelected) {
      setForeground(UIUtil.getTableSelectionForeground());
    }
    else {
      setForeground(pattern.getPattern() != null ? UIUtil.getTableForeground() : JBColor.RED);
    }
    return this;
  }
}
