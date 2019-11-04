// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.util;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageNamesValidation;
import com.intellij.openapi.project.Project;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.EditableModel;
import com.intellij.util.ui.ListTableModel;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.function.Predicate;

public abstract class AbstractParameterTablePanel<P extends AbstractVariableData> extends JPanel {
  private P[] myParameterData;

  protected final JBTable myTable;
  private final MyTableModel myTableModel;

  public P[] getVariableData() {
    return myParameterData;
  }

  protected abstract void updateSignature();

  protected abstract void doEnterAction();

  protected abstract void doCancelAction();

  protected boolean areTypesDirected() {
    return true;
  }

  public AbstractParameterTablePanel(P[] parameterData,
                                     ColumnInfo... columnInfos) {
    this(columnInfos);
    init(parameterData);
  }

  public AbstractParameterTablePanel(ColumnInfo... columnInfos) {
    super(new BorderLayout());
    myTableModel = new MyTableModel(columnInfos);
    myTable = new JBTable(myTableModel);

    for (int i = 0; i < columnInfos.length; i++) {
      if (columnInfos[i] instanceof PassParameterColumnInfo) {
        TableUtil.setupCheckboxColumn(myTable, i);
      }
    }

    DefaultCellEditor defaultEditor = (DefaultCellEditor)myTable.getDefaultEditor(Object.class);
    defaultEditor.setClickCountToStart(1);


    myTable.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    myTable.setCellSelectionEnabled(true);

    myTable.setPreferredScrollableViewportSize(new Dimension(250, myTable.getRowHeight() * 5));
    myTable.setShowGrid(false);
    myTable.setIntercellSpacing(new Dimension(0, 0));
    @NonNls final InputMap inputMap = myTable.getInputMap();
    inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "enable_disable");
    @NonNls final ActionMap actionMap = myTable.getActionMap();
    actionMap.put("enable_disable", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        if (myTable.isEditing()) return;
        int[] rows = myTable.getSelectedRows();
        if (rows.length > 0) {
          boolean valueToBeSet = false;
          for (int row : rows) {
            if (!getVariableData()[row].isPassAsParameter()) {
              valueToBeSet = true;
              break;
            }
          }
          for (int row : rows) {
            getVariableData()[row].passAsParameter = valueToBeSet;
          }
          myTableModel.fireTableRowsUpdated(rows[0], rows[rows.length - 1]);
          TableUtil.selectRows(myTable, rows);
        }
      }
    });

    // make ESCAPE work when the table has focus
    actionMap.put("doCancel", new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        TableCellEditor editor = myTable.getCellEditor();
        if (editor != null) {
          editor.stopCellEditing();
        }
        else {
          doCancelAction();
        }
      }
    });


    JPanel listPanel = ToolbarDecorator.createDecorator(myTable).disableAddAction().disableRemoveAction().createPanel();
    add(listPanel, BorderLayout.CENTER);


    myTableModel.addTableModelListener(e -> updateSignature());
  }

  public void init(P[] parameterData) {
    myParameterData = parameterData;
    myTableModel.setItems(Arrays.asList(parameterData));
    if (parameterData.length > 1) {
      myTable.getSelectionModel().setSelectionInterval(0, 0);
    }
  }


  @Override
  public void setEnabled(boolean enabled) {
    myTable.setEnabled(enabled);
    super.setEnabled(enabled);
  }

  public static class NameColumnInfo extends ColumnInfo<AbstractVariableData, String> {
    private final Predicate<? super String> myNameValidator;

    public NameColumnInfo(Predicate<? super String> nameValidator) {
      super("Name");
      myNameValidator = nameValidator;
    }

    public NameColumnInfo(Language lang, Project project) {
      super("Name");
      myNameValidator = (paramName) -> LanguageNamesValidation.isIdentifier(lang, paramName, project);
    }

    @Nullable
    @Override
    public String valueOf(AbstractVariableData data) {
      return data.getName();
    }

    @Override
    public void setValue(AbstractVariableData data, String value) {
      if (myNameValidator.test(value)) {
        data.name = value;
      }
    }

    @Override
    public boolean isCellEditable(AbstractVariableData data) {
      return true;
    }
  }

  public static class PassParameterColumnInfo extends ColumnInfo<AbstractVariableData, Boolean> {
    public PassParameterColumnInfo() {
      super("");
    }

    @Nullable
    @Override
    public TableCellRenderer getRenderer(AbstractVariableData data) {
      return new BooleanTableCellRenderer();
    }

    @Nullable
    @Override
    public Boolean valueOf(AbstractVariableData data) {
      return data.isPassAsParameter();
    }

    @Override
    public void setValue(AbstractVariableData data, Boolean value) {
      data.passAsParameter = value;
    }

    @Override
    public boolean isCellEditable(AbstractVariableData data) {
      return true;
    }

    @Override
    public Class<?> getColumnClass() {
      return Boolean.class;
    }
  }

  private class MyTableModel extends ListTableModel<AbstractVariableData> implements EditableModel {
    MyTableModel(@NotNull ColumnInfo... columnInfos) {
      super(columnInfos);
    }

    @Override
    public void addRow() {
      throw new IllegalAccessError("Not implemented");
    }

    @Override
    public void removeRow(int index) {
      throw new IllegalAccessError("Not implemented");
    }

    @Override
    public void exchangeRows(int row, int targetRow) {
      if (row < 0 || row >= getVariableData().length) return;
      if (targetRow < 0 || targetRow >= getVariableData().length) return;

      final P currentItem = getVariableData()[row];
      AbstractParameterTablePanel.this.exchangeRows(row, targetRow, currentItem);

      myTableModel.fireTableRowsUpdated(Math.min(targetRow, row), Math.max(targetRow, row));
      myTable.getSelectionModel().setSelectionInterval(targetRow, targetRow);
      updateSignature();
    }

    @Override
    public boolean canExchangeRows(int row, int targetRow) {
      if (row < 0 || row >= getVariableData().length) return false;
      if (targetRow < 0 || targetRow >= getVariableData().length) return false;
      return true;
    }
  }

  protected void exchangeRows(int row, int targetRow, P currentItem) {
    getVariableData()[row] = getVariableData()[targetRow];
    getVariableData()[targetRow] = currentItem;
  }
}
