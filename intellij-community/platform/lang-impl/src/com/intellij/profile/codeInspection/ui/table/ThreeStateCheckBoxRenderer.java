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
package com.intellij.profile.codeInspection.ui.table;

import com.intellij.profile.codeInspection.ui.inspectionsTree.InspectionsConfigTreeTable;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ThreeStateCheckBox;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.CellEditorListener;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.EventObject;
import java.util.List;

/**
 * @author Dmitry Batkovich
 */
public class ThreeStateCheckBoxRenderer extends ThreeStateCheckBox implements TableCellRenderer, TableCellEditor {

  private final List<CellEditorListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public ThreeStateCheckBoxRenderer() {
    setThirdStateEnabled(false);
    setHorizontalAlignment(CENTER);
    setVerticalAlignment(CENTER);
    setBorder(JBUI.Borders.empty(0, 0, 0, InspectionsConfigTreeTable.getAdditionalPadding()));
    addItemListener(new ItemListener() {
      @Override
      public void itemStateChanged(ItemEvent e) {
        stopCellEditing();
      }
    });
  }

  @Override
  public Component getTableCellEditorComponent(final JTable table, final Object value, final boolean isSelected, final int row, final int column) {
    JCheckBox checkBox = tune(value, isSelected, row, table, false);
    checkBox.setOpaque(true);
    return checkBox;
  }

  @Override
  public Component getTableCellRendererComponent(final JTable table, final Object value, final boolean isSelected, final boolean hasFocus, final int row, final int column) {
    return tune(value, isSelected, row, table, hasFocus);
  }

  private JCheckBox tune(final Object value, final boolean isSelected, final int row, final JTable table, boolean hasFocus) {
    final Color bg = table.getBackground();
    final Color fg = table.getForeground();
    final Color selBg = table.getSelectionBackground();
    final Color selFg = table.getSelectionForeground();

    setForeground(isSelected ? selFg : fg);
    setBackground(isSelected ? selBg : bg);

    if (value == null) {
      setState(State.DONT_CARE);
    } else {
      setSelected((Boolean) value);
    }

    return this;
  }

  @Nullable
  @Override
  public Object getCellEditorValue() {
    return getState() != State.DONT_CARE ? isSelected() : null;
  }

  @Override
  public boolean isCellEditable(final EventObject anEvent) {
    return true;
  }

  @Override
  public boolean shouldSelectCell(final EventObject anEvent) {
    return true;
  }

  @Override
  public boolean stopCellEditing() {
    final ChangeEvent e = new ChangeEvent(this);
    for (final CellEditorListener listener : myListeners) {
      listener.editingStopped(e);
    }
    return true;
  }

  @Override
  public void cancelCellEditing() {
    final ChangeEvent e = new ChangeEvent(this);
    for (final CellEditorListener listener : myListeners) {
      listener.editingCanceled(e);
    }
  }

  @Override
  public void addCellEditorListener(final CellEditorListener l) {
    myListeners.add(l);
  }

  @Override
  public void removeCellEditorListener(final CellEditorListener l) {
    myListeners.remove(l);
  }
}
