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
package com.intellij.execution.startup;

import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.util.Processor;
import com.intellij.util.ui.EditableModel;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.AbstractTableModel;
import java.util.*;

/**
 * @author Irina.Chernushina on 8/26/2015.
 */
public class ProjectStartupTasksTableModel extends AbstractTableModel implements EditableModel {
  public static final int NAME_COLUMN = 0;
  public static final int IS_SHARED_COLUMN = 1;

  private final Set<RunnerAndConfigurationSettings> mySharedConfigurations;
  private final List<RunnerAndConfigurationSettings> myAllConfigurations;

  public ProjectStartupTasksTableModel() {
    mySharedConfigurations = new HashSet<>();
    myAllConfigurations = new ArrayList<>();
  }

  public void setData(final Collection<? extends RunnerAndConfigurationSettings> shared,
                      final Collection<? extends RunnerAndConfigurationSettings> local) {
    mySharedConfigurations.clear();
    myAllConfigurations.clear();

    mySharedConfigurations.addAll(shared);
    myAllConfigurations.addAll(shared);
    myAllConfigurations.addAll(local);
    Collections.sort(myAllConfigurations, new RunnerAndConfigurationSettingsComparator());
  }

  @Override
  public void addRow() {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public void exchangeRows(int oldIndex, int newIndex) {
    throw new UnsupportedOperationException("Not implemented");
  }

  @Override
  public boolean canExchangeRows(int oldIndex, int newIndex) {
    return false;
  }

  @Override
  public void removeRow(int idx) {
    final RunnerAndConfigurationSettings settings = myAllConfigurations.remove(idx);
    if (settings != null) {
      mySharedConfigurations.remove(settings);
      fireTableDataChanged();
    }
  }

  @Override
  public int getRowCount() {
    return myAllConfigurations.size();
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @NotNull
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if (IS_SHARED_COLUMN == columnIndex) {
      return Boolean.class;
    }
    return super.getColumnClass(columnIndex);
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (NAME_COLUMN == columnIndex) {
      return myAllConfigurations.get(rowIndex).getName();
    } else if (IS_SHARED_COLUMN == columnIndex) {
      return mySharedConfigurations.contains(myAllConfigurations.get(rowIndex));
    }
    return null;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    if (IS_SHARED_COLUMN == columnIndex) {
      if (Boolean.TRUE.equals(aValue)) {
        mySharedConfigurations.add(myAllConfigurations.get(rowIndex));
      } else {
        mySharedConfigurations.remove(myAllConfigurations.get(rowIndex));
      }
      fireTableRowsUpdated(rowIndex, rowIndex + 1);
    }
  }

  @NotNull
  @Override
  public String getColumnName(int column) {
    if (NAME_COLUMN == column) return "Run Configuration";
    if (IS_SHARED_COLUMN == column) return "Shared";
    return "";
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    if (NAME_COLUMN == columnIndex) return false;
    return myAllConfigurations.get(rowIndex).isShared();
  }

  public void addConfiguration(final @NotNull RunnerAndConfigurationSettings configuration) {
    if (myAllConfigurations.contains(configuration)) return;
    myAllConfigurations.add(configuration);
    Collections.sort(myAllConfigurations, RunnerAndConfigurationSettingsComparator.getInstance());
    if (configuration.isShared()) {
      mySharedConfigurations.add(configuration);
    }
    fireTableDataChanged();
  }

  public Set<RunnerAndConfigurationSettings> getSharedConfigurations() {
    return mySharedConfigurations;
  }

  public List<RunnerAndConfigurationSettings> getAllConfigurations() {
    return myAllConfigurations;
  }

  public void reValidateConfigurations(final Processor<? super RunnerAndConfigurationSettings> existenceChecker) {
    final Iterator<RunnerAndConfigurationSettings> iterator = myAllConfigurations.iterator();
    while (iterator.hasNext()) {
      final RunnerAndConfigurationSettings settings = iterator.next();
      if (!existenceChecker.process(settings)) {
        iterator.remove();
        mySharedConfigurations.remove(settings);
      }
    }
  }

  public static class RunnerAndConfigurationSettingsComparator implements Comparator<RunnerAndConfigurationSettings> {
    private static final RunnerAndConfigurationSettingsComparator ourInstance = new RunnerAndConfigurationSettingsComparator();

    public static RunnerAndConfigurationSettingsComparator getInstance() {
      return ourInstance;
    }

    @Override
    public int compare(RunnerAndConfigurationSettings o1, RunnerAndConfigurationSettings o2) {
      return o1.getName().compareToIgnoreCase(o2.getName());
    }
  }
}
