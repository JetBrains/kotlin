// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.packageDependencies.ui;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.ide.util.scopeChooser.PackageSetChooserCombo;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.packageDependencies.DependencyRule;
import com.intellij.packageDependencies.DependencyValidationManager;
import com.intellij.psi.search.scope.packageSet.CustomScopesProviderEx;
import com.intellij.psi.search.scope.packageSet.NamedScope;
import com.intellij.psi.search.scope.packageSet.PackageSet;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class DependencyConfigurable implements Configurable {
  private final Project myProject;
  private MyTableModel myDenyRulesModel;
  private MyTableModel myAllowRulesModel;
  private TableView<DependencyRule> myDenyTable;
  private TableView<DependencyRule> myAllowTable;

  private final ColumnInfo<DependencyRule, NamedScope> DENY_USAGES_OF = new LeftColumn(AnalysisScopeBundle.message("dependency.configurable.deny.table.column1"));
  private final ColumnInfo<DependencyRule, NamedScope> DENY_USAGES_IN = new RightColumn(AnalysisScopeBundle.message("dependency.configurable.deny.table.column2"));
  private final ColumnInfo<DependencyRule, NamedScope> ALLOW_USAGES_OF = new LeftColumn(AnalysisScopeBundle.message("dependency.configurable.allow.table.column1"));
  private final ColumnInfo<DependencyRule, NamedScope> ALLOW_USAGES_ONLY_IN = new RightColumn(AnalysisScopeBundle.message("dependency.configurable.allow.table.column2"));

  private JPanel myWholePanel;
  private JPanel myDenyPanel;
  private JPanel myAllowPanel;
  private JCheckBox mySkipImports;
  private static final Logger LOG = Logger.getInstance(DependencyConfigurable.class);

  public DependencyConfigurable(Project project) {
    myProject = project;
  }

  @Override
  public String getDisplayName() {
    return AnalysisScopeBundle.message("dependency.configurable.display.name");
  }

  @Override
  public String getHelpTopic() {
    return "editing.analyzeDependencies.validation";
  }

  @Override
  public JComponent createComponent() {
    myDenyRulesModel = new MyTableModel(new ColumnInfo[]{DENY_USAGES_OF, DENY_USAGES_IN}, true);
    myDenyRulesModel.setSortable(false);

    myAllowRulesModel = new MyTableModel(new ColumnInfo[]{ALLOW_USAGES_OF, ALLOW_USAGES_ONLY_IN}, false);
    myAllowRulesModel.setSortable(false);

    myDenyTable = new TableView<>(myDenyRulesModel);
    myDenyPanel.add(createRulesPanel(myDenyTable), BorderLayout.CENTER);
    myAllowTable = new TableView<>(myAllowRulesModel);
    myAllowPanel.add(createRulesPanel(myAllowTable), BorderLayout.CENTER);
    return myWholePanel;
  }

  private JPanel createRulesPanel(TableView<DependencyRule> table) {
    table.setSurrendersFocusOnKeystroke(true);
    table.setPreferredScrollableViewportSize(JBUI.size(300, 150));
    table.setShowGrid(true);
    table.setRowHeight(new PackageSetChooserCombo(myProject, null).getPreferredSize().height);

    return ToolbarDecorator.createDecorator(table).createPanel();
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myDenyTable;
  }

  @Override
  public void apply() throws ConfigurationException {
    stopTableEditing();
    DependencyValidationManager validationManager = DependencyValidationManager.getInstance(myProject);
    validationManager.removeAllRules();
    final HashMap<String, PackageSet> unUsed = new HashMap<>(validationManager.getUnnamedScopes());
    List<DependencyRule> modelItems = new ArrayList<>();
    modelItems.addAll(myDenyRulesModel.getItems());
    modelItems.addAll(myAllowRulesModel.getItems());
    for (DependencyRule rule : modelItems) {
      validationManager.addRule(rule);
      final NamedScope fromScope = rule.getFromScope();
      if (fromScope instanceof NamedScope.UnnamedScope) {
        final PackageSet fromPackageSet = fromScope.getValue();
        LOG.assertTrue(fromPackageSet != null);
        unUsed.remove(fromPackageSet.getText());
      }
      final NamedScope toScope = rule.getToScope();
      if (toScope instanceof NamedScope.UnnamedScope) {
        final PackageSet toPackageSet = toScope.getValue();
        LOG.assertTrue(toPackageSet != null);
        unUsed.remove(toPackageSet.getText());
      }
    }
    for (String text : unUsed.keySet()) {//cleanup
      validationManager.getUnnamedScopes().remove(text);
    }

    validationManager.setSkipImportStatements(mySkipImports.isSelected());

    DaemonCodeAnalyzer.getInstance(myProject).restart();
  }

  private void stopTableEditing() {
    myDenyTable.stopEditing();
    myAllowTable.stopEditing();
  }

  @Override
  public void reset() {
    final DependencyValidationManager validationManager = DependencyValidationManager.getInstance(myProject);
    DependencyRule[] rules = validationManager.getAllRules();
    final ArrayList<DependencyRule> denyList = new ArrayList<>();
    final ArrayList<DependencyRule> allowList = new ArrayList<>();
    for (DependencyRule rule : rules) {
      if (rule.isDenyRule()) {
        denyList.add(rule.createCopy());
      }
      else {
        allowList.add(rule.createCopy());
      }
    }
    myDenyRulesModel.setItems(denyList);
    myAllowRulesModel.setItems(allowList);
    mySkipImports.setSelected(validationManager.skipImportStatements());
  }

  @Override
  public boolean isModified() {
    final DependencyValidationManager validationManager = DependencyValidationManager.getInstance(myProject);
    if (validationManager.skipImportStatements() != mySkipImports.isSelected()) return true;
    final List<DependencyRule> rules = new ArrayList<>();
    rules.addAll(myDenyRulesModel.getItems());
    rules.addAll(myAllowRulesModel.getItems());
    return !Arrays.asList(validationManager.getAllRules()).equals(rules);
  }

  private static final DefaultTableCellRenderer
    CELL_RENDERER = new DefaultTableCellRenderer() {
      @Override
      public Component getTableCellRendererComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     boolean hasFocus,
                                                     int row,
                                                     int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        setText(value == null ? "" : ((NamedScope)value).getName());
        return this;
      }
    };

  public abstract class MyColumnInfo extends ColumnInfo<DependencyRule, NamedScope> {
    protected MyColumnInfo(String name) {
      super(name);
    }

    @Override
    public boolean isCellEditable(DependencyRule rule) {
      return true;
    }

    @Override
    public TableCellRenderer getRenderer(DependencyRule rule) {
      return CELL_RENDERER;
    }

    @Override
    public TableCellEditor getEditor(DependencyRule packageSetDependencyRule) {
      return new AbstractTableCellEditor() {
        private PackageSetChooserCombo myCombo;

        @Override
        public Object getCellEditorValue() {
          return myCombo.getSelectedScope();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
          myCombo = new PackageSetChooserCombo(myProject, value == null ? null : ((NamedScope)value).getName());
          return new CellEditorComponentWithBrowseButton<>(myCombo, this);
        }
      };
    }

    @Override
    public abstract void setValue(DependencyRule rule, NamedScope packageSet);
  }


  private class RightColumn extends MyColumnInfo {
    RightColumn(final String name) {
      super(name);
    }

    @Override
    public NamedScope valueOf(DependencyRule rule) {
      return rule.getFromScope();
    }

    @Override
    public void setValue(DependencyRule rule, NamedScope set) {
      rule.setFromScope(set);
    }
  }

  private class LeftColumn extends MyColumnInfo {
    LeftColumn(final String name) {
      super(name);
    }

    @Override
    public NamedScope valueOf(DependencyRule rule) {
      return rule.getToScope();
    }

    @Override
    public void setValue(DependencyRule rule, NamedScope set) {
      rule.setToScope(set);
    }
  }

  private static class MyTableModel extends ListTableModel<DependencyRule> implements EditableModel {
    private final boolean myDenyRule;

    MyTableModel(final ColumnInfo[] columnInfos, final boolean isDenyRule) {
      super(columnInfos);
      myDenyRule = isDenyRule;
    }

    @Override
    public void addRow() {
      ArrayList<DependencyRule> newList = new ArrayList<>(getItems());
      final NamedScope scope = CustomScopesProviderEx.getAllScope();
      newList.add(new DependencyRule(scope, scope, myDenyRule));
      setItems(newList);
    }

    @Override
    public void exchangeRows(int index1, int index2) {
      ArrayList<DependencyRule> newList = new ArrayList<>(getItems());
      DependencyRule r1 = newList.get(index1);
      DependencyRule r2 = newList.get(index2);
      newList.set(index1, r2);
      newList.set(index2, r1);
      setItems(newList);
    }
  }
}
