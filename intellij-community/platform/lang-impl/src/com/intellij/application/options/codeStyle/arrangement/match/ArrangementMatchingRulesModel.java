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

import com.intellij.application.options.codeStyle.arrangement.ArrangementConstants;
import com.intellij.application.options.codeStyle.arrangement.ui.ArrangementEditorAware;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.DefaultTableModel;

/**
 * @author Denis Zhdanov
 */
public class ArrangementMatchingRulesModel extends DefaultTableModel {

  private static final Logger LOG = Logger.getInstance(ArrangementMatchingRulesModel.class);

  @Override
  public int getColumnCount() {
    return 1;
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return Object.class;
  }

  public Object getElementAt(int row) {
    return getValueAt(row, 0);
  }
  
  public void set(int row, Object value) {
    setValueAt(value, row, 0);
  }

  public void insert(int row, Object value) {
    insertRow(row, new Object[] { value });
  }
  
  @Override
  public void setValueAt(Object aValue, int row, int column) {
    if (ArrangementConstants.LOG_RULE_MODIFICATION) {
      LOG.info(String.format("Setting match rule '%s' to index %d", aValue, row));
    }
    super.setValueAt(aValue, row, column);
  }
  
  public void add(@NotNull Object data) {
    addRow(new Object[] { data });
  }
  
  @Override
  public void addRow(Object[] rowData) {
    if (ArrangementConstants.LOG_RULE_MODIFICATION) {
      LOG.info(String.format("Adding match rule '%s' (to index %d)", rowData[0], getRowCount()));
    }
    super.addRow(rowData);
  }

  @Override
  public void removeRow(int row) {
    if (ArrangementConstants.LOG_RULE_MODIFICATION) {
      LOG.info(String.format("Removing match rule '%s' from index %d", getValueAt(row, 0), row));
    }
    super.removeRow(row);
  }

  public void clear() {
    getDataVector().removeAllElements();
  }
  
  public int getSize() {
    return getRowCount();
  }

  @Override
  public boolean isCellEditable(int row, int column) {
    return getValueAt(row, column) instanceof ArrangementEditorAware;
  }
}
