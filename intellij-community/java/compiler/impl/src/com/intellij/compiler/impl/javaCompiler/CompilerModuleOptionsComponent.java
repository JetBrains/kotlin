// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl.javaCompiler;

import com.intellij.compiler.options.ModuleOptionsTableModel;
import com.intellij.compiler.options.ModuleTableCellRenderer;
import com.intellij.openapi.project.Project;
import com.intellij.ui.InsertPathAction;
import com.intellij.ui.TableSpeedSearch;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.fields.ExpandableTextField;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.Map;

/**
 * @author Eugene Zhuravlev
 */
public class CompilerModuleOptionsComponent extends JPanel {
  private final JBTable myTable;
  private final Project myProject;

  public CompilerModuleOptionsComponent(@NotNull Project project) {
    super(new GridBagLayout());
    myProject = project;

    myTable = new JBTable(new ModuleOptionsTableModel());
    myTable.setRowHeight(JBUI.scale(22));
    myTable.getEmptyText().setText("Additional compilation options will be the same for all modules");

    TableColumn moduleColumn = myTable.getColumnModel().getColumn(0);
    moduleColumn.setHeaderValue("Module");
    moduleColumn.setCellRenderer(new ModuleTableCellRenderer());

    TableColumn optionsColumn = myTable.getColumnModel().getColumn(1);
    String columnTitle = "Compilation options";
    optionsColumn.setHeaderValue(columnTitle);
    int width = myTable.getFontMetrics(myTable.getFont()).stringWidth(columnTitle) + 10;
    optionsColumn.setPreferredWidth(width);
    optionsColumn.setMinWidth(width);
    ExpandableTextField editor = new ExpandableTextField();
    InsertPathAction.addTo(editor, null, false);
    optionsColumn.setCellEditor(new DefaultCellEditor(editor));
    new TableSpeedSearch(myTable);

    JPanel table = ToolbarDecorator.createDecorator(myTable)
      .disableUpAction()
      .disableDownAction()
      .setAddAction(b -> addModules())
      .setRemoveAction(b -> removeSelectedModules())
      .createPanel();
    table.setPreferredSize(new Dimension(myTable.getWidth(), 150));
    JLabel header = new JLabel("Override compiler parameters per-module:");

    add(header, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insets(5, 5, 0, 0), 0, 0));
    add(table, new GridBagConstraints(0, 1, 1, 1, 1.0, 1.0, GridBagConstraints.WEST, GridBagConstraints.BOTH, JBUI.insets(5, 5, 0, 0), 0, 0));
  }

  private void addModules() {
    int i = ((ModuleOptionsTableModel)myTable.getModel()).addModulesToModel(myProject, this);
    if (i >= 0) {
      TableUtil.selectRows(myTable, new int[]{i});
      TableUtil.scrollSelectionToVisible(myTable);
    }
  }

  private void removeSelectedModules() {
    if (myTable.getSelectedRows().length > 0) {
      TableUtil.removeSelectedItems(myTable);
    }
  }

  @NotNull
  public Map<String, String> getModuleOptionsMap() {
    return ((ModuleOptionsTableModel)myTable.getModel()).getModuleOptions();
  }

  public void setModuleOptionsMap(@NotNull Map<String, String> moduleOptions) {
    ((ModuleOptionsTableModel)myTable.getModel()).setModuleOptions(myProject, moduleOptions);
  }
}