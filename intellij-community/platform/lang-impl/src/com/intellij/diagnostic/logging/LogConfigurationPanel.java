// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.diagnostic.logging;

import com.intellij.diagnostic.DiagnosticBundle;
import com.intellij.execution.configurations.LogFileOptions;
import com.intellij.execution.configurations.PredefinedLogFile;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.ui.TextComponentAccessor;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.table.TableView;
import com.intellij.util.SmartList;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.CellEditorComponentWithBrowseButton;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LogConfigurationPanel<T extends RunConfigurationBase> extends SettingsEditor<T> {
  private final TableView<LogFileOptions> myFilesTable;
  private final ListTableModel<LogFileOptions> myModel;
  private JPanel myWholePanel;
  private JPanel myScrollPanel;
  private JBCheckBox myRedirectOutputCb;
  private TextFieldWithBrowseButton myOutputFile;
  private JCheckBox myShowConsoleOnStdOutCb;
  private JCheckBox myShowConsoleOnStdErrCb;
  private final Map<LogFileOptions, PredefinedLogFile> myLog2Predefined = new THashMap<>();
  private final List<PredefinedLogFile> myUnresolvedPredefined = new SmartList<>();

  public LogConfigurationPanel() {
    ColumnInfo<LogFileOptions, Boolean> IS_SHOW = new MyIsActiveColumnInfo();
    ColumnInfo<LogFileOptions, LogFileOptions> FILE = new MyLogFileColumnInfo();
    ColumnInfo<LogFileOptions, Boolean> IS_SKIP_CONTENT = new MyIsSkipColumnInfo();

    myModel = new ListTableModel<>(IS_SHOW, FILE, IS_SKIP_CONTENT);
    myFilesTable = new TableView<>(myModel);
    myFilesTable.getEmptyText().setText(DiagnosticBundle.message("log.monitor.no.files"));

    final JTableHeader tableHeader = myFilesTable.getTableHeader();
    final FontMetrics fontMetrics = tableHeader.getFontMetrics(tableHeader.getFont());

    int preferredWidth = fontMetrics.stringWidth(IS_SHOW.getName()) + 20;
    setUpColumnWidth(tableHeader, preferredWidth, 0);

    preferredWidth = fontMetrics.stringWidth(IS_SKIP_CONTENT.getName()) + 20;
    setUpColumnWidth(tableHeader, preferredWidth, 2);

    myFilesTable.setColumnSelectionAllowed(false);
    myFilesTable.setShowGrid(false);
    myFilesTable.setDragEnabled(false);
    myFilesTable.setShowHorizontalLines(false);
    myFilesTable.setShowVerticalLines(false);
    myFilesTable.setIntercellSpacing(new Dimension(0, 0));

    myScrollPanel.add(
      ToolbarDecorator.createDecorator(myFilesTable)
        .setAddAction(button -> {
          ArrayList<LogFileOptions> newList = new ArrayList<>(myModel.getItems());
          LogFileOptions newOptions = new LogFileOptions("", "", true);
          if (showEditorDialog(newOptions)) {
            newList.add(newOptions);
            myModel.setItems(newList);
            int index = myModel.getRowCount() - 1;
            myModel.fireTableRowsInserted(index, index);
            myFilesTable.setRowSelectionInterval(index, index);
          }
        }).setRemoveAction(button -> {
          TableUtil.stopEditing(myFilesTable);
          final int[] selected = myFilesTable.getSelectedRows();
          if (selected.length == 0) return;
          for (int i = selected.length - 1; i >= 0; i--) {
            myModel.removeRow(selected[i]);
          }
          for (int i = selected.length - 1; i >= 0; i--) {
            int idx = selected[i];
            myModel.fireTableRowsDeleted(idx, idx);
          }
          int selection = selected[0];
          if (selection >= myModel.getRowCount()) {
            selection = myModel.getRowCount() - 1;
          }
          if (selection >= 0) {
            myFilesTable.setRowSelectionInterval(selection, selection);
          }
          IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myFilesTable, true));
        }).setEditAction(button -> {
          final int selectedRow = myFilesTable.getSelectedRow();
          //noinspection ConstantConditions
          showEditorDialog(myFilesTable.getSelectedObject());
          myModel.fireTableDataChanged();
          myFilesTable.setRowSelectionInterval(selectedRow, selectedRow);
        }).setRemoveActionUpdater(e -> myFilesTable.getSelectedRowCount() >= 1 &&
                                     !myLog2Predefined.containsKey(myFilesTable.getSelectedObject())).setEditActionUpdater(
        e -> myFilesTable.getSelectedRowCount() >= 1 &&
           !myLog2Predefined.containsKey(myFilesTable.getSelectedObject()) &&
               myFilesTable.getSelectedObject() != null).disableUpDownActions().createPanel(), BorderLayout.CENTER);

    myWholePanel.setPreferredSize(new Dimension(-1, 150));
    myOutputFile.addBrowseFolderListener("Choose File to Save Console Output", "Console output would be saved to the specified file", null,
                                         FileChooserDescriptorFactory.createSingleFileOrFolderDescriptor(),
                                         TextComponentAccessor.TEXT_FIELD_WHOLE_TEXT);
    myRedirectOutputCb.addActionListener(e -> myOutputFile.setEnabled(myRedirectOutputCb.isSelected()));
  }

  private void setUpColumnWidth(final JTableHeader tableHeader, final int preferredWidth, int columnIdx) {
    myFilesTable.getColumnModel().getColumn(columnIdx).setCellRenderer(new BooleanTableCellRenderer());
    final TableColumn tableColumn = tableHeader.getColumnModel().getColumn(columnIdx);
    tableColumn.setWidth(preferredWidth);
    tableColumn.setPreferredWidth(preferredWidth);
    tableColumn.setMinWidth(preferredWidth);
    tableColumn.setMaxWidth(preferredWidth);
  }

  public void refreshPredefinedLogFiles(RunConfigurationBase configurationBase) {
    List<LogFileOptions> items = myModel.getItems();
    List<LogFileOptions> newItems = new ArrayList<>();
    boolean changed = false;
    for (LogFileOptions item : items) {
      final PredefinedLogFile predefined = myLog2Predefined.get(item);
      if (predefined != null) {
        final LogFileOptions options = configurationBase.getOptionsForPredefinedLogFile(predefined);
        if (LogFileOptions.areEqual(item, options)) {
          newItems.add(item);
        }
        else {
          changed = true;
          myLog2Predefined.remove(item);
          if (options == null) {
            myUnresolvedPredefined.add(predefined);
          }
          else {
            newItems.add(options);
            myLog2Predefined.put(options, predefined);
          }
        }
      }
      else {
        newItems.add(item);
      }
    }

    final PredefinedLogFile[] unresolved = myUnresolvedPredefined.toArray(new PredefinedLogFile[0]);
    for (PredefinedLogFile logFile : unresolved) {
      final LogFileOptions options = configurationBase.getOptionsForPredefinedLogFile(logFile);
      if (options != null) {
        changed = true;
        myUnresolvedPredefined.remove(logFile);
        myLog2Predefined.put(options, logFile);
        newItems.add(options);
      }
    }

    if (changed) {
      myModel.setItems(newItems);
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull final RunConfigurationBase configuration) {
    ArrayList<LogFileOptions> list = new ArrayList<>();
    final List<LogFileOptions> logFiles = configuration.getLogFiles();
    for (LogFileOptions setting : logFiles) {
      list.add(
        new LogFileOptions(setting.getName(), setting.getPathPattern(), setting.isEnabled(), setting.isSkipContent(), setting.isShowAll()));
    }
    myLog2Predefined.clear();
    myUnresolvedPredefined.clear();
    final List<PredefinedLogFile> predefinedLogFiles = configuration.getPredefinedLogFiles();
    for (PredefinedLogFile predefinedLogFile : predefinedLogFiles) {
      PredefinedLogFile logFile = new PredefinedLogFile();
      logFile.copyFrom(predefinedLogFile);
      final LogFileOptions options = configuration.getOptionsForPredefinedLogFile(logFile);
      if (options != null) {
        myLog2Predefined.put(options, logFile);
        list.add(options);
      }
      else {
        myUnresolvedPredefined.add(logFile);
      }
    }
    myModel.setItems(list);
    final boolean redirectOutputToFile = configuration.isSaveOutputToFile();
    myRedirectOutputCb.setSelected(redirectOutputToFile);
    final String fileOutputPath = configuration.getOutputFilePath();
    myOutputFile.setText(fileOutputPath != null ? FileUtil.toSystemDependentName(fileOutputPath) : "");
    myOutputFile.setEnabled(redirectOutputToFile);
    myShowConsoleOnStdOutCb.setSelected(configuration.isShowConsoleOnStdOut());
    myShowConsoleOnStdErrCb.setSelected(configuration.isShowConsoleOnStdErr());
  }

  @Override
  protected void applyEditorTo(@NotNull final RunConfigurationBase configuration) throws ConfigurationException {
    myFilesTable.stopEditing();
    configuration.removeAllLogFiles();
    configuration.removeAllPredefinedLogFiles();

    for (int i = 0; i < myModel.getRowCount(); i++) {
      LogFileOptions options = (LogFileOptions)myModel.getValueAt(i, 1);
      if (Comparing.equal(options.getPathPattern(), "")) {
        continue;
      }
      final Boolean checked = (Boolean)myModel.getValueAt(i, 0);
      final Boolean skipped = (Boolean)myModel.getValueAt(i, 2);
      final PredefinedLogFile predefined = myLog2Predefined.get(options);
      if (predefined != null) {
        PredefinedLogFile file = new PredefinedLogFile();
        file.setId(predefined.getId());
        file.setEnabled(options.isEnabled());
        configuration.addPredefinedLogFile(file);
      }
      else {
        configuration
          .addLogFile(options.getPathPattern(), options.getName(), checked.booleanValue(), skipped.booleanValue(), options.isShowAll());
      }
    }
    for (PredefinedLogFile logFile : myUnresolvedPredefined) {
      configuration.addPredefinedLogFile(logFile);
    }
    final String text = myOutputFile.getText();
    configuration.setFileOutputPath(StringUtil.isEmpty(text) ? null : FileUtil.toSystemIndependentName(text));
    configuration.setSaveOutputToFile(myRedirectOutputCb.isSelected());
    configuration.setShowConsoleOnStdOut(myShowConsoleOnStdOutCb.isSelected());
    configuration.setShowConsoleOnStdErr(myShowConsoleOnStdErrCb.isSelected());
  }

  @Override
  @NotNull
  protected JComponent createEditor() {
    return myWholePanel;
  }

  private static boolean showEditorDialog(@NotNull LogFileOptions options) {
    EditLogPatternDialog dialog = new EditLogPatternDialog();
    dialog.init(options.getName(), options.getPathPattern(), options.isShowAll());
    if (dialog.showAndGet()) {
      options.setName(dialog.getName());
      options.setPathPattern(dialog.getLogPattern());
      options.setShowAll(dialog.isShowAllFiles());
      return true;
    }
    return false;
  }

  private class MyLogFileColumnInfo extends ColumnInfo<LogFileOptions, LogFileOptions> {
    MyLogFileColumnInfo() {
      super(DiagnosticBundle.message("log.monitor.log.file.column"));
    }

    @Override
    public TableCellRenderer getRenderer(final LogFileOptions p0) {
      return new DefaultTableCellRenderer() {
        @NotNull
        @Override
        public Component getTableCellRendererComponent(@NotNull JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
          final Component renderer = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
          setText(((LogFileOptions)value).getName());
          setBackground(isSelected ? table.getSelectionBackground() : table.getBackground());
          setBorder(null);
          return renderer;
        }
      };
    }

    @Override
    public LogFileOptions valueOf(final LogFileOptions object) {
      return object;
    }

    @Override
    public TableCellEditor getEditor(final LogFileOptions item) {
      return new LogFileCellEditor(item);
    }

    @Override
    public void setValue(final LogFileOptions o, final LogFileOptions aValue) {
      if (aValue != null) {
        if (!o.getName().equals(aValue.getName()) || !o.getPathPattern().equals(aValue.getPathPattern())
            || o.isShowAll() != aValue.isShowAll()) {
          myLog2Predefined.remove(o);
        }
        o.setName(aValue.getName());
        o.setShowAll(aValue.isShowAll());
        o.setPathPattern(aValue.getPathPattern());
      }
    }

    @Override
    public boolean isCellEditable(final LogFileOptions o) {
      return !myLog2Predefined.containsKey(o);
    }
  }

  private class MyIsActiveColumnInfo extends ColumnInfo<LogFileOptions, Boolean> {
    protected MyIsActiveColumnInfo() {
      super(DiagnosticBundle.message("log.monitor.is.active.column"));
    }

    @Override
    public Class getColumnClass() {
      return Boolean.class;
    }

    @Override
    public Boolean valueOf(final LogFileOptions object) {
      return object.isEnabled();
    }

    @Override
    public boolean isCellEditable(LogFileOptions element) {
      return true;
    }

    @Override
    public void setValue(LogFileOptions element, Boolean checked) {
      final PredefinedLogFile predefinedLogFile = myLog2Predefined.get(element);
      if (predefinedLogFile != null) {
        predefinedLogFile.setEnabled(checked.booleanValue());
      }
      element.setEnabled(checked.booleanValue());
    }
  }

  private class MyIsSkipColumnInfo extends ColumnInfo<LogFileOptions, Boolean> {
    protected MyIsSkipColumnInfo() {
      super(DiagnosticBundle.message("log.monitor.is.skipped.column"));
    }

    @Override
    public Class getColumnClass() {
      return Boolean.class;
    }

    @Override
    public Boolean valueOf(final LogFileOptions element) {
      return element.isSkipContent();
    }

    @Override
    public boolean isCellEditable(LogFileOptions element) {
      return !myLog2Predefined.containsKey(element);
    }

    @Override
    public void setValue(LogFileOptions element, Boolean skipped) {
      element.setSkipContent(skipped.booleanValue());
    }
  }

  private class LogFileCellEditor extends AbstractTableCellEditor {
    private final CellEditorComponentWithBrowseButton<JTextField> myComponent;
    private final LogFileOptions myLogFileOptions;

    LogFileCellEditor(LogFileOptions options) {
      myLogFileOptions = options;
      myComponent = new CellEditorComponentWithBrowseButton<>(new TextFieldWithBrowseButton(), this);
      getChildComponent().setEditable(false);
      getChildComponent().setBorder(null);
      myComponent.getComponentWithButton().getButton().addActionListener(e -> {
        showEditorDialog(myLogFileOptions);
        JTextField textField = getChildComponent();
        textField.setText(myLogFileOptions.getName());
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(textField, true));
        myModel.fireTableDataChanged();
      });
    }

    @Override
    public Object getCellEditorValue() {
      return myLogFileOptions;
    }

    private JTextField getChildComponent() {
      return myComponent.getChildComponent();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      getChildComponent().setText(((LogFileOptions)value).getName());
      return myComponent;
    }
  }
}
