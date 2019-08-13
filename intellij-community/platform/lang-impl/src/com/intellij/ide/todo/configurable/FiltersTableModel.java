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

import com.intellij.ide.IdeBundle;
import com.intellij.ide.todo.TodoFilter;
import com.intellij.psi.search.TodoPattern;
import com.intellij.util.ui.ItemRemovable;

import javax.swing.table.AbstractTableModel;
import java.util.Iterator;
import java.util.List;

final class FiltersTableModel extends AbstractTableModel implements ItemRemovable {
  private final String[] ourColumnNames = new String[]{
    IdeBundle.message("column.todo.filters.name"),
    IdeBundle.message("column.todo.filter.patterns")
  };
  private final Class[] ourColumnClasses = new Class[]{String.class, String.class};

  private final List<? extends TodoFilter> myFilters;

  FiltersTableModel(List<? extends TodoFilter> filters) {
    myFilters = filters;
  }

  @Override
  public String getColumnName(int column) {
    return ourColumnNames[column];
  }

  @Override
  public Class getColumnClass(int column) {
    return ourColumnClasses[column];
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public int getRowCount() {
    return myFilters.size();
  }

  @Override
  public Object getValueAt(int row, int column) {
    TodoFilter filter = myFilters.get(row);
    switch (column) {
      case 0: { // "Name" column
        return filter.getName();
      }
      case 1: {
        StringBuilder sb = new StringBuilder();
        for (Iterator i = filter.iterator(); i.hasNext(); ) {
          TodoPattern pattern = (TodoPattern)i.next();
          sb.append(pattern.getPatternString());
          if (i.hasNext()) {
            sb.append(" | ");
          }
        }
        return sb.toString();
      }
      default: {
        throw new IllegalArgumentException();
      }
    }
  }

  @Override
  public void removeRow(int index) {
    myFilters.remove(index);
    fireTableRowsDeleted(index, index);
  }
}
