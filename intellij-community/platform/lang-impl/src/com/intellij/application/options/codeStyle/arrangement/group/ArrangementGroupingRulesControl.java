// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.group;

import com.intellij.application.options.codeStyle.arrangement.color.ArrangementColorsProvider;
import com.intellij.application.options.codeStyle.arrangement.ui.ArrangementEditorAware;
import com.intellij.application.options.codeStyle.arrangement.ui.ArrangementRepresentationAware;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.psi.codeStyle.arrangement.group.ArrangementGroupingRule;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementSettingsToken;
import com.intellij.psi.codeStyle.arrangement.std.ArrangementStandardSettingsManager;
import com.intellij.psi.codeStyle.arrangement.std.CompositeArrangementSettingsToken;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.AbstractTableCellEditor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * @author Denis Zhdanov
 */
public class ArrangementGroupingRulesControl extends JBTable {

  @NotNull public static final DataKey<ArrangementGroupingRulesControl> KEY = DataKey.create("Arrangement.Rule.Group.Control");

  @NotNull private final Map<ArrangementSettingsToken, ArrangementGroupingComponent> myComponents =
    new HashMap<>();

  @NotNull private final ArrangementStandardSettingsManager mySettingsManager;

  private int myRowUnderMouse = -1;

  public ArrangementGroupingRulesControl(@NotNull ArrangementStandardSettingsManager settingsManager,
                                         @NotNull ArrangementColorsProvider colorsProvider)
  {
    super(new DefaultTableModel(0, 1));
    mySettingsManager = settingsManager;
    setDefaultRenderer(Object.class, new MyRenderer());
    getColumnModel().getColumn(0).setCellEditor(new MyEditor());
    setShowColumns(false);
    setShowGrid(false);
    setBorder(IdeBorderFactory.createBorder());
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

    List<CompositeArrangementSettingsToken> groupingTokens = settingsManager.getSupportedGroupingTokens();
    if (groupingTokens != null) {
      for (CompositeArrangementSettingsToken token : groupingTokens) {
        ArrangementGroupingComponent component = new ArrangementGroupingComponent(token, colorsProvider, settingsManager);
        myComponents.put(token.getToken(), component);
        getModel().addRow(new Object[]{component});
      }
    }
  }

  @Override
  public DefaultTableModel getModel() {
    return (DefaultTableModel)super.getModel();
  }

  public void setRules(@Nullable List<? extends ArrangementGroupingRule> rules) {
    for (ArrangementGroupingComponent component : myComponents.values()) {
      component.setSelected(false);
    }

    if (rules == null) {
      return;
    }

    DefaultTableModel model = getModel();
    while (model.getRowCount() > 0) {
      model.removeRow(model.getRowCount() - 1);
    }

    final Set<ArrangementSettingsToken> groupingTokens =
      new HashSet<>(myComponents.keySet());
    for (ArrangementGroupingRule rule : rules) {
      final ArrangementSettingsToken groupingType = rule.getGroupingType();
      ArrangementGroupingComponent component = myComponents.get(groupingType);
      component.setSelected(true);
      component.setOrderType(rule.getOrderType());
      model.addRow(new Object[]{component});
      groupingTokens.remove(groupingType);
    }

    List<ArrangementSettingsToken> types =
      new ArrayList<>(groupingTokens);
    types = mySettingsManager.sort(types);
    for (ArrangementSettingsToken type : types) {
      model.addRow(new Object[]{myComponents.get(type)});
    }
  }

  @NotNull
  public List<ArrangementGroupingRule> getRules() {
    List<ArrangementGroupingRule> result = new ArrayList<>();
    DefaultTableModel model = getModel();
    for (int i = 0, max = model.getRowCount(); i < max; i++) {
      ArrangementGroupingComponent component = (ArrangementGroupingComponent)model.getValueAt(i, 0);
      if (!component.isSelected()) {
        continue;
      }
      ArrangementSettingsToken orderType = component.getOrderType();
      if (orderType == null) {
        result.add(new ArrangementGroupingRule(component.getGroupingType()));
      }
      else {
        result.add(new ArrangementGroupingRule(component.getGroupingType(), orderType));
      }
    }
    return result;
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_MOVED) {
      int oldRow = myRowUnderMouse;
      myRowUnderMouse = rowAtPoint(e.getPoint());
      if (oldRow >= 0 && myRowUnderMouse != oldRow) {
        getModel().fireTableRowsUpdated(oldRow, oldRow);
      }
      if (myRowUnderMouse >= 0 && myRowUnderMouse != oldRow) {
        getModel().fireTableRowsUpdated(myRowUnderMouse, myRowUnderMouse);
      }
    }
    super.processMouseMotionEvent(e);
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (e.getID() == MouseEvent.MOUSE_EXITED && myRowUnderMouse >= 0) {
      int row = myRowUnderMouse;
      myRowUnderMouse = -1;
      getModel().fireTableRowsUpdated(row, row);
    }
    super.processMouseEvent(e);
  }

  private class MyRenderer implements TableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (value instanceof ArrangementGroupingComponent) {
        ArrangementGroupingComponent component = (ArrangementGroupingComponent)value;
        component.setRowIndex(row + 1);
        component.setHighlight(myRowUnderMouse == row || table.isRowSelected(row));
        component.revalidate();
        return component;
      }
      else if (value instanceof ArrangementRepresentationAware) {
        return ((ArrangementRepresentationAware)value).getComponent();
      }
      return null;
    }
  }

  private static class MyEditor extends AbstractTableCellEditor {

    @Nullable Object myValue;

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      if (value instanceof ArrangementEditorAware) {
        myValue = value;
        return ((ArrangementEditorAware)value).getComponent();
      }
      return null;
    }

    @Override
    public Object getCellEditorValue() {
      return myValue;
    }

    @Override
    public boolean stopCellEditing() {
      super.stopCellEditing();
      myValue = null;
      return true;
    }
  }
}
