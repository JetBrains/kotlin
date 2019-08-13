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
package com.intellij.application.options.colors.fileStatus;

import com.intellij.application.options.colors.ColorAndFontOptions;
import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.editor.colors.ColorKey;
import com.intellij.openapi.editor.colors.EditorColorsScheme;
import com.intellij.openapi.editor.colors.impl.AbstractColorsScheme;
import com.intellij.openapi.editor.colors.impl.EditorColorsManagerImpl;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FileStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public class FileStatusColorsTableModel extends AbstractTableModel {
  private final EditorColorsScheme myScheme;
  private final List<FileStatusColorDescriptor> myDescriptors;

  private final static ColumnInfo[] COLUMNS_INFO = {
    new ColumnInfo(
      Boolean.class, "", descriptor -> descriptor.isDefault()
    ),
    new ColumnInfo(
      String.class, ApplicationBundle.message("file.status.colors.header.status"), descriptor -> descriptor.getStatus().getText())
  };

  private static class ColumnInfo {
    public Class columnClass;
    public String columnName;
    public Function<FileStatusColorDescriptor,Object> dataFunction;

    ColumnInfo(Class columnClass, String columnName, Function<FileStatusColorDescriptor,Object> dataFunction) {
      this.columnClass = columnClass;
      this.columnName = columnName;
      this.dataFunction = dataFunction;
    }
  }

  public FileStatusColorsTableModel(@NotNull FileStatus[] fileStatuses, @NotNull EditorColorsScheme scheme) {
    myScheme = scheme;
    myDescriptors = createDescriptors(fileStatuses, myScheme);
  }

  private static List<FileStatusColorDescriptor> createDescriptors(@NotNull FileStatus[] fileStatuses, @NotNull EditorColorsScheme scheme) {
    EditorColorsScheme baseScheme = scheme instanceof AbstractColorsScheme ? ((AbstractColorsScheme)scheme).getParentScheme() : null;
    List<FileStatusColorDescriptor> descriptors = new ArrayList<>();
    for (FileStatus fileStatus : fileStatuses) {
      Color color = scheme.getColor(fileStatus.getColorKey());
      Color originalColor = baseScheme != null ? baseScheme.getColor(fileStatus.getColorKey()) : null;
      descriptors.add(new FileStatusColorDescriptor(fileStatus, color, originalColor));
    }
    Collections.sort(descriptors, Comparator.comparing(d -> d.getStatus().getText()));
    return descriptors;
  }

  @Override
  public int getRowCount() {
    return myDescriptors.size();
  }

  @Override
  public int getColumnCount() {
    return COLUMNS_INFO.length;
  }

  @Override
  public String getColumnName(int columnIndex) {
    return COLUMNS_INFO[columnIndex].columnName;
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return COLUMNS_INFO[columnIndex].columnClass;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    FileStatusColorDescriptor descriptor = myDescriptors.get(rowIndex);
    return COLUMNS_INFO[columnIndex].dataFunction.apply(descriptor);
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    myDescriptors.get(rowIndex).setColor((Color)aValue);
    fireTableCellUpdated(rowIndex, columnIndex);
  }

  public void resetToDefault(int rowIndex) {
    myDescriptors.get(rowIndex).resetToDefault();
    fireTableCellUpdated(rowIndex, 1);
  }

  @Nullable
  FileStatusColorDescriptor getDescriptorByName(String statusName) {
    for (FileStatusColorDescriptor descriptor : myDescriptors) {
      if (statusName.equals(descriptor.getStatus().getText())) {
        return descriptor;
      }
    }
    return null;
  }

  public boolean isModified() {
    for (FileStatusColorDescriptor descriptor : myDescriptors) {
      ColorKey key = descriptor.getStatus().getColorKey();
      Color original = myScheme.getColor(key);
      Color current =  descriptor.getColor();
      if (!Comparing.equal(original, current)) return true;
    }
    return false;
  }

  public void reset() {
    for (FileStatusColorDescriptor descriptor : myDescriptors) {
      descriptor.setColor(myScheme.getColor(descriptor.getStatus().getColorKey()));
    }
    fireTableDataChanged();
  }

  public void apply() {
    for (FileStatusColorDescriptor descriptor : myDescriptors) {
      myScheme.setColor(descriptor.getStatus().getColorKey(), descriptor.getColor());
    }
    if (myScheme instanceof AbstractColorsScheme) {
      ((AbstractColorsScheme)myScheme).setSaveNeeded(true);
    }
    if (EditorColorsManagerImpl.isTempScheme(myScheme)) {
      ColorAndFontOptions.writeTempScheme(myScheme);
    }
  }

  @Nullable
  public FileStatusColorDescriptor getDescriptorAt(int index) {
    if (index >= 0 && index < myDescriptors.size()) {
      return myDescriptors.get(index);
    }
    return null;
  }

  public boolean containsCustomSettings() {
    for (FileStatusColorDescriptor descriptor : myDescriptors) {
      if (!descriptor.isDefault()) return true;
    }
    return false;
  }
}
