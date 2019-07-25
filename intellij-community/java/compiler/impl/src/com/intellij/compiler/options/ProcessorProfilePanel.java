/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.compiler.options;

import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.*;
import com.intellij.ui.table.JBTable;
import com.intellij.util.ui.EditableModel;
import com.intellij.util.ui.JBUI;
import org.jetbrains.jps.model.java.compiler.ProcessorConfigProfile;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;
import java.util.*;

/**
 * @author Eugene Zhuravlev
 */
public class ProcessorProfilePanel extends JPanel {
  private final Project myProject;

  private final JRadioButton myRbClasspath;
  private final JRadioButton myRbProcessorsPath;
  private final TextFieldWithBrowseButton myProcessorPathField;
  private final JTextField myGeneratedProductionDirField;
  private final JTextField myGeneratedTestsDirField;
  private final JRadioButton myRbRelativeToOutputRoot;
  private final JRadioButton myRbRelativeToContentRoot;
  private final ProcessorTableModel myProcessorsModel;
  private final JCheckBox myCbEnableProcessing;
  private final JBTable myProcessorTable;
  private final JBTable myOptionsTable;
  private final JPanel myProcessorPanel;
  private final JPanel myOptionsPanel;
  private final OptionsTableModel myOptionsModel;
  private final JLabel myStoreGenSourcesLabel;
  private final JLabel myProductionLabel;
  private final JLabel myTestLabel;
  private final JPanel myProcessorTablePanel;
  private final JPanel myOptionsTablePanel;


  public ProcessorProfilePanel(Project project) {
    super(new GridBagLayout());
    myProject = project;

    myCbEnableProcessing = new JCheckBox("Enable annotation processing");

    {
      myRbClasspath = new JRadioButton("Obtain processors from project classpath");
      myRbProcessorsPath = new JRadioButton("Processor path:");
      ButtonGroup group = new ButtonGroup();
      group.add(myRbClasspath);
      group.add(myRbProcessorsPath);
    }

    {
      myRbRelativeToContentRoot = new JRadioButton("Module content root");
      myRbRelativeToOutputRoot = new JRadioButton("Module output directory");
      final ButtonGroup group = new ButtonGroup();
      group.add(myRbRelativeToContentRoot);
      group.add(myRbRelativeToOutputRoot);
    }

    myProcessorPathField = new TextFieldWithBrowseButton(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createAllButJarContentsDescriptor();
        final VirtualFile[] files = FileChooser.chooseFiles(descriptor, myProcessorPathField, myProject, null);
        if (files.length > 0) {
          final StringBuilder builder = new StringBuilder();
          for (VirtualFile file : files) {
            if (builder.length() > 0) {
              builder.append(File.pathSeparator);
            }
            builder.append(FileUtil.toSystemDependentName(file.getPath()));
          }
          myProcessorPathField.setText(builder.toString());
        }
      }
    });

    myProcessorTablePanel = new JPanel(new BorderLayout());
    myProcessorsModel = new ProcessorTableModel();
    myProcessorTablePanel.setBorder(IdeBorderFactory.createTitledBorder("Annotation Processors", false));
    myProcessorTable = new JBTable(myProcessorsModel);
    myProcessorTable.getEmptyText().setText("Compiler will run all automatically discovered processors");
    myProcessorPanel = createTablePanel(myProcessorTable);
    myProcessorTablePanel.add(myProcessorPanel, BorderLayout.CENTER);

    myOptionsTablePanel = new JPanel(new BorderLayout());
    myOptionsModel = new OptionsTableModel();
    myOptionsTablePanel.setBorder(IdeBorderFactory.createTitledBorder("Annotation Processor options", false));
    myOptionsTable = new JBTable(myOptionsModel);
    myOptionsTable.getEmptyText().setText("No processor-specific options configured");
    myOptionsPanel = createTablePanel(myOptionsTable);
    myOptionsTablePanel.add(myOptionsPanel, BorderLayout.CENTER);

    myGeneratedProductionDirField = new JTextField();
    myGeneratedTestsDirField = new JTextField();

    add(myCbEnableProcessing,
        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 3, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, JBUI.emptyInsets(), 0, 0));
    add(myRbClasspath,
        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 3, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insetsTop(10), 0, 0));
    add(myRbProcessorsPath,
        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, JBUI.insetsTop(5), 0, 0));
    add(myProcessorPathField,
        new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(5, 5, 0, 0), 0, 0));

    myStoreGenSourcesLabel = new JLabel("Store generated sources relative to: ");
    add(myStoreGenSourcesLabel,
        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(15, 5, 0, 0), 0, 0));
    add(myRbRelativeToOutputRoot,
        new GridBagConstraints(1, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(15, 5, 0, 0), 0, 0));
    add(myRbRelativeToContentRoot,
        new GridBagConstraints(2, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(15, 5, 0, 0), 0, 0));

    myProductionLabel = new JLabel("Production sources directory:");
    add(myProductionLabel,
        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(10, 5, 0, 0), 0, 0));
    add(myGeneratedProductionDirField,
        new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(10, 5, 0, 0), 0, 0));

    myTestLabel = new JLabel("Test sources directory:");
    add(myTestLabel,
        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(10, 5, 0, 0), 0, 0));
    add(myGeneratedTestsDirField,
        new GridBagConstraints(1, GridBagConstraints.RELATIVE, 2, 1, 1.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, JBUI.insets(10, 5, 0, 0), 0, 0));

    add(myProcessorTablePanel,
        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 3, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, JBUI.insetsTop(10), 0, 0));
    add(myOptionsTablePanel,
        new GridBagConstraints(0, GridBagConstraints.RELATIVE, 3, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, JBUI.insetsTop(10), 0, 0));

    myRbClasspath.addItemListener(e -> updateEnabledState());

    myProcessorTable.getSelectionModel().addListSelectionListener(e -> {
      if (!e.getValueIsAdjusting()) {
        updateEnabledState();
      }
    });

    myCbEnableProcessing.addItemListener(e -> updateEnabledState());

    updateEnabledState();

  }

  public void setProfile(ProcessorConfigProfile config) {
    myCbEnableProcessing.setSelected(config.isEnabled());

    (config.isObtainProcessorsFromClasspath()? myRbClasspath : myRbProcessorsPath).setSelected(true);
    myProcessorPathField.setText(FileUtil.toSystemDependentName(config.getProcessorPath()));

    final String productionDirName = config.getGeneratedSourcesDirectoryName(false);
    myGeneratedProductionDirField.setText(productionDirName.trim());
    final String testsDirName = config.getGeneratedSourcesDirectoryName(true);
    myGeneratedTestsDirField.setText(testsDirName.trim());
    if (config.isOutputRelativeToContentRoot()) {
      myRbRelativeToContentRoot.setSelected(true);
    }
    else {
      myRbRelativeToOutputRoot.setSelected(true);
    }
    myProcessorsModel.setProcessors(config.getProcessors());
    myOptionsModel.setOptions(config.getProcessorOptions());

    updateEnabledState();
  }

  public void saveTo(ProcessorConfigProfile profile) {
    profile.setEnabled(myCbEnableProcessing.isSelected());
    profile.setObtainProcessorsFromClasspath(myRbClasspath.isSelected());
    profile.setProcessorPath(myProcessorPathField.getText().trim());

    final String productionDir = myGeneratedProductionDirField.getText().trim();
    profile.setGeneratedSourcesDirectoryName(StringUtil.isEmpty(productionDir)? null : productionDir, false);
    final String testsDir = myGeneratedTestsDirField.getText().trim();
    profile.setGeneratedSourcesDirectoryName(StringUtil.isEmpty(testsDir)? null : testsDir, true);

    profile.setOutputRelativeToContentRoot(myRbRelativeToContentRoot.isSelected());

    profile.clearProcessors();
    for (String processor : myProcessorsModel.getProcessors()) {
      profile.addProcessor(processor);
    }
    profile.clearProcessorOptions();
    for (Map.Entry<String, String> entry : myOptionsModel.getOptions().entrySet()) {
      profile.setOption(entry.getKey(), entry.getValue());
    }
  }

  private static JPanel createTablePanel(final JBTable table) {
    return ToolbarDecorator.createDecorator(table)
      .disableUpAction()
      .disableDownAction()
      .setAddAction(new AnActionButtonRunnable() {
        @Override
        public void run(AnActionButton anActionButton) {
          final TableCellEditor cellEditor = table.getCellEditor();
          if (cellEditor != null) {
            cellEditor.stopCellEditing();
          }
          final TableModel model = table.getModel();
          ((EditableModel)model).addRow();
          TableUtil.editCellAt(table, model.getRowCount() - 1, 0);
        }
      })
      .createPanel();
  }

  private void updateEnabledState() {
    final boolean enabled = myCbEnableProcessing.isSelected();
    final boolean useProcessorPath = !myRbClasspath.isSelected();
    myRbClasspath.setEnabled(enabled);
    myRbProcessorsPath.setEnabled(enabled);
    myProcessorPathField.setEnabled(enabled && useProcessorPath);
    updateTable(myProcessorPanel, myProcessorTable, enabled);
    updateTable(myOptionsPanel, myOptionsTable, enabled);
    myGeneratedProductionDirField.setEnabled(enabled);
    myGeneratedTestsDirField.setEnabled(enabled);
    myRbRelativeToOutputRoot.setEnabled(enabled);
    myRbRelativeToContentRoot.setEnabled(enabled);
    myStoreGenSourcesLabel.setEnabled(enabled);
    myProductionLabel.setEnabled(enabled);
    myTestLabel.setEnabled(enabled);
    myProcessorTablePanel.setEnabled(enabled);
    myOptionsTablePanel.setEnabled(enabled);
  }

  private static void updateTable(final JPanel tablePanel, final JBTable table, boolean enabled) {
    final AnActionButton addButton = ToolbarDecorator.findAddButton(tablePanel);
    if (addButton != null) {
      addButton.setEnabled(enabled);
    }
    final AnActionButton removeButton = ToolbarDecorator.findRemoveButton(tablePanel);
    if (removeButton != null) {
      removeButton.setEnabled(enabled && table.getSelectedRow() >= 0);
    }
    table.setEnabled(enabled);
    final JTableHeader header = table.getTableHeader();
    if (header != null) {
      header.repaint();
    }
  }

  private static class OptionsTableModel extends AbstractTableModel implements EditableModel {
    private final java.util.List<KeyValuePair> myRows = new ArrayList<>();

    @Override
    public String getColumnName(int column) {
      switch (column) {
        case 0: return "Option Name";
        case 1: return "Value";
      }
      return super.getColumnName(column);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return String.class;
    }

    @Override
    public int getRowCount() {
      return myRows.size();
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 0 || columnIndex == 1;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      switch (columnIndex) {
        case 0: return myRows.get(rowIndex).key;
        case 1: return myRows.get(rowIndex).value;
      }
      return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (aValue != null) {
        switch (columnIndex) {
          case 0:
            myRows.get(rowIndex).key = (String)aValue;
            break;
          case 1:
            myRows.get(rowIndex).value = (String)aValue;
            break;
        }
      }
    }

    @Override
    public void removeRow(int idx) {
      myRows.remove(idx);
      fireTableRowsDeleted(idx, idx);
    }

    @Override
    public void exchangeRows(int oldIndex, int newIndex) {
    }

    @Override
    public boolean canExchangeRows(int oldIndex, int newIndex) {
      return false;
    }

    @Override
    public void addRow() {
      myRows.add(new KeyValuePair());
      final int index = myRows.size() - 1;
      fireTableRowsInserted(index, index);
    }

    public void setOptions(Map<String, String> options) {
      clear();
      if (!options.isEmpty()) {
        for (Map.Entry<String, String> entry : options.entrySet()) {
          myRows.add(new KeyValuePair(entry.getKey(), entry.getValue()));
        }
        Collections.sort(myRows, (o1, o2) -> o1.key.compareToIgnoreCase(o2.key));
        fireTableRowsInserted(0, options.size()-1);
      }
    }

    public void clear() {
      final int count = myRows.size();
      if (count > 0) {
        myRows.clear();
        fireTableRowsDeleted(0, count-1);
      }
    }

    public Map<String, String> getOptions() {
      final Map<String, String> map = new HashMap<>();
      for (KeyValuePair pair : myRows) {
        map.put(pair.key.trim(), pair.value.trim());
      }
      map.remove("");
      return map;
    }

    private static final class KeyValuePair {
      String key;
      String value;

      KeyValuePair() {
        this("", "");
      }

      KeyValuePair(String key, String value) {
        this.key = key;
        this.value = value;
      }
    }
  }

  private static class ProcessorTableModel extends AbstractTableModel implements EditableModel {
    private final List<String> myRows = new ArrayList<>();

    @Override
    public String getColumnName(int column) {
      if (column == 0) {
        return "Processor FQ Name";
      }
      return super.getColumnName(column);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
      return String.class;
    }

    @Override
    public int getRowCount() {
      return myRows.size();
    }

    @Override
    public int getColumnCount() {
      return 1;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 0;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      if (columnIndex == 0) {
        return myRows.get(rowIndex);
      }
      return null;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (aValue != null && columnIndex == 0) {
        myRows.set(rowIndex, (String)aValue);
      }
    }

    @Override
    public void removeRow(int idx) {
      myRows.remove(idx);
      fireTableRowsDeleted(idx, idx);
    }

    @Override
    public void exchangeRows(int oldIndex, int newIndex) {
    }

    @Override
    public boolean canExchangeRows(int oldIndex, int newIndex) {
      return false;
    }

    @Override
    public void addRow() {
      myRows.add("");
      final int index = myRows.size() - 1;
      fireTableRowsInserted(index, index);
    }

    public void setProcessors(Collection<String> processors) {
      clear();
      if (!processors.isEmpty()) {
        myRows.addAll(processors);
        fireTableRowsInserted(0, processors.size()-1);
      }
    }

    public void clear() {
      final int count = myRows.size();
      if (count > 0) {
        myRows.clear();
        fireTableRowsDeleted(0, count-1);
      }
    }

    public Collection<String> getProcessors() {
      final Set<String> set = new LinkedHashSet<>();
      for (String row : myRows) {
        if (row != null) {
          set.add(row.trim());
        }
      }
      set.remove("");
      return set;
    }
  }

}
