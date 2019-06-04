// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.colors.fileStatus;

import com.intellij.ui.ColorUtil;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.*;

public class FileStatusColorsTable extends JBTable {

  public FileStatusColorsTable() {
    setShowGrid(false);
    setIntercellSpacing(new Dimension(0,0));
    getColumnModel().setColumnSelectionAllowed(false);
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setDefaultRenderer(String.class, new MyStatusCellRenderer());
    setDefaultRenderer(Boolean.class, new MyDefaultStatusRenderer());
    setTableHeader(null);
    setRowHeight(JBUIScale.scale(22));
  }

  public void adjustColumnWidths() {
    for (int col = 0; col < getColumnCount(); col++) {
      DefaultTableColumnModel colModel = (DefaultTableColumnModel) getColumnModel();
      TableColumn column = colModel.getColumn(col);
      Class colClass = getColumnClass(col);
      int width = 0;
      int rightGap = 0;
      if (getColumnClass(col).equals(Boolean.class)) {
        width = JBUIScale.scale(15);
      }
      else {
        rightGap = isColorColumn(col) ? JBUI.size(10, 1).width : 0;
        TableCellRenderer renderer;
        for (int row = 0; row < getRowCount(); row++) {
          renderer = getCellRenderer(row, col);
          Component comp = renderer.getTableCellRendererComponent(this, getValueAt(row, col),
                                                                  false, false, row, col);
          width = Math.max(width, comp.getPreferredSize().width);
        }
      }
      width += rightGap;
      column.setPreferredWidth(width);
      if (colClass.equals(Color.class) || colClass.equals(Boolean.class)) {
        column.setMinWidth(width);
        column.setMaxWidth(width);
      }
    }
  }

  private boolean isColorColumn(int col) {
    return getModel().getColumnClass(col).equals(Color.class);
  }

  private class MyStatusCellRenderer extends DefaultTableCellRenderer {

    private final JLabel myLabel = new JLabel();

    MyStatusCellRenderer() {
      myLabel.setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (value instanceof String) {
        FileStatusColorDescriptor descriptor = ((FileStatusColorsTableModel)getModel()).getDescriptorByName((String)value);
        if (descriptor != null) {
          myLabel.setText((String)value);
          myLabel.setForeground(isSelected ? UIUtil.getTableSelectionForeground() : descriptor.getColor());
          myLabel.setBackground(UIUtil.getTableBackground(isSelected));
          return myLabel;
        }
      }
      return c;
    }
  }

  private static class MyDefaultStatusRenderer extends DefaultTableCellRenderer {
    private final JLabel myLabel = new JLabel();
    private final Color myLabelColor;

    MyDefaultStatusRenderer() {
      myLabel.setOpaque(true);
      myLabelColor = ColorUtil.withAlpha(myLabel.getForeground(), 0.5);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (value instanceof Boolean) {
        myLabel.setForeground(isSelected ? UIUtil.getTableSelectionForeground() : myLabelColor);
        myLabel.setBackground(UIUtil.getTableBackground(isSelected));
        myLabel.setText((Boolean)value ? "" : "* ");
        myLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        return myLabel;
      }
      return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
    }
  }
}
