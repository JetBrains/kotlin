// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.configuration;

import com.google.common.collect.ImmutableMap;
import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.util.EnvVariablesTable;
import com.intellij.execution.util.EnvironmentVariable;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.AnActionButton;
import com.intellij.ui.AnActionButtonRunnable;
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.UserActivityProviderComponent;
import com.intellij.ui.table.JBTable;
import com.intellij.ui.table.TableView;
import com.intellij.util.ArrayUtil;
import com.intellij.util.EnvironmentUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.ListTableModel;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

public class EnvironmentVariablesTextFieldWithBrowseButton extends TextFieldWithBrowseButton implements UserActivityProviderComponent {

  private EnvironmentVariablesData myData = EnvironmentVariablesData.DEFAULT;
  private final Map<String, String> myParentDefaults = new LinkedHashMap<>();
  private final List<ChangeListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

  public EnvironmentVariablesTextFieldWithBrowseButton() {
    super();
    addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        setEnvs(EnvVariablesTable.parseEnvsFromText(getText()));
        new MyEnvironmentVariablesDialog().show();
      }
    });
    getTextField().getDocument().addDocumentListener(new DocumentAdapter() {
      @Override
      protected void textChanged(@NotNull DocumentEvent e) {
        if (!StringUtil.equals(stringifyEnvs(myData.getEnvs()), getText())) {
          Map<String, String> textEnvs = EnvVariablesTable.parseEnvsFromText(getText());
          myData = EnvironmentVariablesData.create(textEnvs, myData.isPassParentEnvs());
          fireStateChanged();
        }
      }
    });
  }

  /**
   * @return unmodifiable Map instance
   */
  @NotNull
  public Map<String, String> getEnvs() {
    return myData.getEnvs();
  }

  /**
   * @param envs Map instance containing user-defined environment variables
   *             (iteration order should be reliable user-specified, like {@link LinkedHashMap} or {@link ImmutableMap})
   */
  public void setEnvs(@NotNull Map<String, String> envs) {
    setData(EnvironmentVariablesData.create(envs, myData.isPassParentEnvs()));
  }

  @NotNull
  public EnvironmentVariablesData getData() {
    return myData;
  }

  public void setData(@NotNull EnvironmentVariablesData data) {
    EnvironmentVariablesData oldData = myData;
    myData = data;
    setText(stringifyEnvs(data.getEnvs()));
    if (!oldData.equals(data)) {
      fireStateChanged();
    }
  }

  @NotNull
  @Override
  protected Icon getDefaultIcon() {
    return AllIcons.General.InlineVariables;
  }

  @NotNull
  @Override
  protected Icon getHoveredIcon() {
    return AllIcons.General.InlineVariablesHover;
  }

  @NotNull
  private static String stringifyEnvs(@NotNull Map<String, String> envs) {
    if (envs.isEmpty()) {
      return "";
    }
    StringBuilder buf = new StringBuilder();
    for (Map.Entry<String, String> entry : envs.entrySet()) {
      if (buf.length() > 0) {
        buf.append(";");
      }
      buf.append(StringUtil.escapeChar(entry.getKey(), ';'))
        .append("=")
        .append(StringUtil.escapeChar(entry.getValue(), ';'));
    }
    return buf.toString();
  }

  public boolean isPassParentEnvs() {
    return myData.isPassParentEnvs();
  }

  public void setPassParentEnvs(boolean passParentEnvs) {
    setData(EnvironmentVariablesData.create(myData.getEnvs(), passParentEnvs));
  }

  @Override
  public void addChangeListener(@NotNull ChangeListener changeListener) {
    myListeners.add(changeListener);
  }

  @Override
  public void removeChangeListener(@NotNull ChangeListener changeListener) {
    myListeners.remove(changeListener);
  }

  private void fireStateChanged() {
    for (ChangeListener listener : myListeners) {
      listener.stateChanged(new ChangeEvent(this));
    }
  }

  private static List<EnvironmentVariable> convertToVariables(Map<String, String> map, final boolean readOnly) {
    return ContainerUtil.map(map.entrySet(), entry -> new EnvironmentVariable(entry.getKey(), entry.getValue(), readOnly) {
      @Override
      public boolean getNameIsWriteable() {
        return !readOnly;
      }
    });
  }

  private class MyEnvironmentVariablesDialog extends DialogWrapper {
    private final EnvVariablesTable myUserTable;
    private final EnvVariablesTable mySystemTable;
    private final JCheckBox myIncludeSystemVarsCb;
    private final JPanel myWholePanel;

    protected MyEnvironmentVariablesDialog() {
      super(EnvironmentVariablesTextFieldWithBrowseButton.this, true);
      Map<String, String> userMap = new LinkedHashMap<>(myData.getEnvs());
      Map<String, String> parentMap = new TreeMap<>(new GeneralCommandLine().getParentEnvironment());

      myParentDefaults.putAll(parentMap);
      for (Iterator<Map.Entry<String, String>> iterator = userMap.entrySet().iterator(); iterator.hasNext(); ) {
        Map.Entry<String, String> entry = iterator.next();
        if (parentMap.containsKey(entry.getKey())) { //User overrides system variable, we have to show it in 'parent' table as bold
          parentMap.put(entry.getKey(), entry.getValue());
          iterator.remove();
        }
      }

      List<EnvironmentVariable> userList = convertToVariables(userMap, false);
      List<EnvironmentVariable> systemList = convertToVariables(parentMap, true);
      myUserTable = new MyEnvVariablesTable(userList, true);

      mySystemTable = new MyEnvVariablesTable(systemList, false);

      myIncludeSystemVarsCb = new JCheckBox(ExecutionBundle.message("env.vars.system.title"));
      myIncludeSystemVarsCb.setSelected(isPassParentEnvs());
      myIncludeSystemVarsCb.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          updateSysTableState();
        }
      });
      JLabel label = new JLabel(ExecutionBundle.message("env.vars.user.title"));
      label.setLabelFor(myUserTable.getTableView().getComponent());

      myWholePanel = new JPanel(new MigLayout("fill, ins 0, gap 0, hidemode 3"));
      myWholePanel.add(label, "hmax pref, wrap");
      myWholePanel.add(myUserTable.getComponent(), "push, grow, wrap, gaptop 5");
      myWholePanel.add(myIncludeSystemVarsCb, "hmax pref, wrap, gaptop 5");
      myWholePanel.add(mySystemTable.getComponent(), "push, grow, wrap, gaptop 5");

      updateSysTableState();
      setTitle(ExecutionBundle.message("environment.variables.dialog.title"));
      init();
    }

    @Nullable
    @Override
    protected String getDimensionServiceKey() {
      return "EnvironmentVariablesDialog";
    }

    private void updateSysTableState() {
      mySystemTable.getTableView().setEnabled(myIncludeSystemVarsCb.isSelected());
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
      return myWholePanel;
    }

    @Nullable
    @Override
    protected ValidationInfo doValidate() {
      for (EnvironmentVariable variable : myUserTable.getEnvironmentVariables()) {
        String name = variable.getName(), value = variable.getValue();
        if (StringUtil.isEmpty(name) && StringUtil.isEmpty(value)) continue;

        if (!EnvironmentUtil.isValidName(name)) return new ValidationInfo(IdeBundle.message("run.configuration.invalid.env.name", name));
        if (!EnvironmentUtil.isValidValue(value)) return new ValidationInfo(IdeBundle.message("run.configuration.invalid.env.value", name, value));
      }
      return super.doValidate();
    }

    @Override
    protected void doOKAction() {
      myUserTable.stopEditing();
      final Map<String, String> envs = new LinkedHashMap<>();
      for (EnvironmentVariable variable : myUserTable.getEnvironmentVariables()) {
        if (StringUtil.isEmpty(variable.getName()) && StringUtil.isEmpty(variable.getValue())) continue;
        envs.put(variable.getName(), variable.getValue());
      }
      for (EnvironmentVariable variable : mySystemTable.getEnvironmentVariables()) {
        if (isModifiedSysEnv(variable)) {
          envs.put(variable.getName(), variable.getValue());
        }
      }
      setEnvs(envs);
      setPassParentEnvs(myIncludeSystemVarsCb.isSelected());
      super.doOKAction();
    }
  }

  private class MyEnvVariablesTable extends EnvVariablesTable {
    private final boolean myUserList;

    private MyEnvVariablesTable(List<EnvironmentVariable> list, boolean userList) {
      myUserList = userList;
      TableView<EnvironmentVariable> tableView = getTableView();
      tableView.setPreferredScrollableViewportSize(
        new Dimension(tableView.getPreferredScrollableViewportSize().width,
                      tableView.getRowHeight() * JBTable.PREFERRED_SCROLLABLE_VIEWPORT_HEIGHT_IN_ROWS));
      setValues(list);
      setPasteActionEnabled(myUserList);
    }

    @Nullable
    @Override
    protected AnActionButtonRunnable createAddAction() {
      return myUserList ? super.createAddAction() : null;
    }

    @Nullable
    @Override
    protected AnActionButtonRunnable createRemoveAction() {
      return myUserList ? super.createRemoveAction() : null;
    }

    @NotNull
    @Override
    protected AnActionButton[] createExtraActions() {
      return myUserList
             ? super.createExtraActions()
             : ArrayUtil.append(super.createExtraActions(),
                                new AnActionButton(ActionsBundle.message("action.ChangesView.Revert.text"), AllIcons.Actions.Rollback) {
                                  @Override
                                  public void actionPerformed(@NotNull AnActionEvent e) {
                                    stopEditing();
                                    List<EnvironmentVariable> variables = getSelection();
                                    for (EnvironmentVariable environmentVariable : variables) {
                                      if (isModifiedSysEnv(environmentVariable)) {
                                        environmentVariable.setValue(myParentDefaults.get(environmentVariable.getName()));
                                        setModified();
                                      }
                                    }
                                    getTableView().revalidate();
                                    getTableView().repaint();
                                  }

                                  @Override
                                  public boolean isEnabled() {
                                    List<EnvironmentVariable> selection = getSelection();
                                    for (EnvironmentVariable variable : selection) {
                                      if (isModifiedSysEnv(variable)) return true;
                                    }
                                    return false;
                                  }
                                });
    }

    @Override
    protected ListTableModel createListModel() {
      return new ListTableModel(new MyNameColumnInfo(), new MyValueColumnInfo());
    }

    protected class MyNameColumnInfo extends EnvVariablesTable.NameColumnInfo {
      private final DefaultTableCellRenderer myModifiedRenderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
          Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
          component.setEnabled(table.isEnabled() && (hasFocus || isSelected));
          return component;
        }
      };

      @Override
      public TableCellRenderer getCustomizedRenderer(EnvironmentVariable o, TableCellRenderer renderer) {
        return o.getNameIsWriteable() ? renderer : myModifiedRenderer;
      }
    }
    protected class MyValueColumnInfo extends EnvVariablesTable.ValueColumnInfo {
      private final DefaultTableCellRenderer myModifiedRenderer = new DefaultTableCellRenderer() {
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
          Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
          component.setFont(component.getFont().deriveFont(Font.BOLD));
          if (!hasFocus && !isSelected) {
            component.setForeground(JBUI.CurrentTheme.Link.linkColor());
          }
          return component;
        }
      };

      @Override
      public boolean isCellEditable(EnvironmentVariable environmentVariable) {
        return true;
      }

      @Override
      public TableCellRenderer getCustomizedRenderer(EnvironmentVariable o, TableCellRenderer renderer) {
        return isModifiedSysEnv(o) ? myModifiedRenderer : renderer;
      }
    }
  }
  private boolean isModifiedSysEnv(@NotNull EnvironmentVariable v) {
    return !v.getNameIsWriteable() && !Comparing.equal(v.getValue(), myParentDefaults.get(v.getName()));
  }
}