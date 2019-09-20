// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.impl.ProjectViewFileNestingService.NestingRule;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.ui.table.TableView;
import com.intellij.util.Consumer;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

public class FileNestingInProjectViewDialog extends DialogWrapper {
  private static final Comparator<NestingRule> RULE_COMPARATOR =
    Comparator.comparing(o -> o.getParentFileSuffix() + " " + o.getChildFileSuffix());

  private final JBCheckBox myUseNestingRulesCheckBox;
  private final JPanel myRulesPanel;
  private final TableView<CombinedNestingRule> myTable;

  private final Action myOkAction = new OkAction() {
    @Override
    public void actionPerformed(ActionEvent e) {
      myTable.stopEditing();
      super.actionPerformed(e);
    }
  };

  public FileNestingInProjectViewDialog(@NotNull final Project project) {
    super(project);
    setTitle(IdeBundle.message("file.nesting.dialog.title"));

    myUseNestingRulesCheckBox = new JBCheckBox(IdeBundle.message("file.nesting.feature.enabled.checkbox"));
    myUseNestingRulesCheckBox.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        UIUtil.setEnabled(myRulesPanel, myUseNestingRulesCheckBox.isSelected(), true);
      }
    });

    myTable = createTable();
    myRulesPanel = createRulesPanel(myTable);

    init();
  }

  @Override
  protected String getHelpId() {
    return "project.view.file.nesting.dialog";
  }

  @Override
  protected JComponent createCenterPanel() {
    final JPanel mainPanel = new JPanel(new BorderLayout(0, JBUIScale.scale(16)));
    mainPanel.setBorder(JBUI.Borders.emptyTop(8)); // Resulting indent will be 16 = 8 (default) + 8 (set here)
    mainPanel.add(myUseNestingRulesCheckBox, BorderLayout.NORTH);
    mainPanel.add(myRulesPanel, BorderLayout.CENTER);
    return mainPanel;
  }

  private static JPanel createRulesPanel(@NotNull final TableView<CombinedNestingRule> table) {
    final ToolbarDecorator toolbarDecorator =
      ToolbarDecorator.createDecorator(table,
                                       new ElementProducer<CombinedNestingRule>() {
                                         @Override
                                         public boolean canCreateElement() {
                                           return true;
                                         }

                                         @Override
                                         public CombinedNestingRule createElement() {
                                           return new CombinedNestingRule("", "");
                                         }
                                       })
                      .disableUpDownActions();
    return UI.PanelFactory.panel(toolbarDecorator.createPanel())
                          .withLabel(IdeBundle.message("file.nesting.table.title")).moveLabelOnTop()
                          .resizeY(true)
                          .createPanel();
  }

  private static TableView<CombinedNestingRule> createTable() {
    final ListTableModel<CombinedNestingRule> model = new ListTableModel<>(
      new ColumnInfo<CombinedNestingRule, String>("Parent file suffix") {
        @Override
        public int getWidth(JTable table) {
          return JBUIScale.scale(125);
        }

        @Override
        public boolean isCellEditable(CombinedNestingRule rule) {
          return true;
        }

        @Override
        public String valueOf(CombinedNestingRule rule) {
          return rule.parentSuffix;
        }

        @Override
        public void setValue(CombinedNestingRule rule, String value) {
          rule.parentSuffix = value.trim();
        }
      },
      new ColumnInfo<CombinedNestingRule, String>("Child file suffix") {
        @Override
        public boolean isCellEditable(CombinedNestingRule rule) {
          return true;
        }

        @Override
        public String valueOf(CombinedNestingRule rule) {
          return rule.childSuffixes;
        }

        @Override
        public void setValue(CombinedNestingRule rule, String value) {
          rule.childSuffixes = value;
        }
      }
    );

    final TableView<CombinedNestingRule> table = new TableView<>(model);
    table.setRowHeight(new JTextField().getPreferredSize().height + table.getRowMargin());
    return table;
  }

  @NotNull
  @Override
  protected Action[] createLeftSideActions() {
    return new Action[]{new DialogWrapperAction(IdeBundle.message("file.nesting.reset.to.default.button")) {
      @Override
      protected void doAction(ActionEvent e) {
        resetTable(ProjectViewFileNestingService.loadDefaultNestingRules());
      }
    }};
  }

  @NotNull
  @Override
  protected Action getOKAction() {
    return myOkAction;
  }

  @Nullable
  @Override
  protected ValidationInfo doValidate() {
    if (!myUseNestingRulesCheckBox.isSelected()) return null;

    List<CombinedNestingRule> items = myTable.getListTableModel().getItems();
    for (int i = 0; i < items.size(); i++) {
      final CombinedNestingRule rule = items.get(i);
      final int row = i + 1;
      if (rule.parentSuffix.isEmpty()) {
        return new ValidationInfo("Parent file suffix must not be empty (see row " + row + ")", null);
      }
      if (rule.childSuffixes.isEmpty()) {
        return new ValidationInfo("Child file suffix must not be empty (see row " + row + ")", null);
      }

      for (String childSuffix : StringUtil.split(rule.childSuffixes, ";")) {
        if (rule.parentSuffix.equals(childSuffix.trim())) {
          return new ValidationInfo(
            "Parent and child file suffixes must not be equal ('" + rule.parentSuffix + "', see row " + row + ")", null);
        }
      }
    }

    return null;
  }

  public void reset(boolean useFileNestingRules) {
    myUseNestingRulesCheckBox.setSelected(useFileNestingRules);
    UIUtil.setEnabled(myRulesPanel, myUseNestingRulesCheckBox.isSelected(), true);

    resetTable(ProjectViewFileNestingService.getInstance().getRules());
  }

  private void resetTable(@NotNull final List<? extends NestingRule> rules) {
    final SortedMap<String, CombinedNestingRule> result = new TreeMap<>();
    for (NestingRule rule : ContainerUtil.sorted(rules, RULE_COMPARATOR)) {
      final CombinedNestingRule r = result.get(rule.getParentFileSuffix());
      if (r == null) {
        result.put(rule.getParentFileSuffix(), new CombinedNestingRule(rule.getParentFileSuffix(), rule.getChildFileSuffix()));
      }
      else {
        //noinspection StringConcatenationInLoop
        r.childSuffixes += "; " + rule.getChildFileSuffix();
      }
    }
    myTable.getListTableModel().setItems(new ArrayList<>(result.values()));
  }

  public void apply(@NotNull final Consumer<? super Boolean> useNestingRulesOptionConsumer) {
    useNestingRulesOptionConsumer.consume(myUseNestingRulesCheckBox.isSelected());

    if (myUseNestingRulesCheckBox.isSelected()) {
      final SortedSet<NestingRule> result = new TreeSet<>(RULE_COMPARATOR);
      for (CombinedNestingRule rule : myTable.getListTableModel().getItems()) {
        for (String childSuffix : StringUtil.split(rule.childSuffixes, ";")) {
          if (!StringUtil.isEmptyOrSpaces(childSuffix)) {
            result.add(new NestingRule(rule.parentSuffix, childSuffix.trim()));
          }
        }
      }
      ProjectViewFileNestingService.getInstance().setRules(new ArrayList<>(result));
    }
  }

  private static class CombinedNestingRule {
    @NotNull String parentSuffix;
    @NotNull String childSuffixes; // semicolon-separated, space symbols around each suffix are ignored

    private CombinedNestingRule(@NotNull String parentSuffix, @NotNull String childSuffixes) {
      this.parentSuffix = parentSuffix;
      this.childSuffixes = childSuffixes;
    }
  }
}
