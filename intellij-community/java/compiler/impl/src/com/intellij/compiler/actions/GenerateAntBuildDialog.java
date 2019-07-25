// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.actions;

import com.intellij.compiler.HelpID;
import com.intellij.compiler.ModuleCompilerUtil;
import com.intellij.compiler.ant.BuildProperties;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.compiler.CompilerBundle;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.MultiLineLabelUI;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.Chunk;
import com.intellij.util.ListWithSelection;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.Table;
import com.intellij.util.ui.table.ComboBoxTableCellEditor;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class GenerateAntBuildDialog extends DialogWrapper {
  private JPanel myPanel;
  private JRadioButton myRbGenerateSingleFileBuild;
  private JRadioButton myRbGenerateMultipleFilesBuild;
  private JCheckBox myCbEnableUIFormsCompilation;
  private JRadioButton myRbBackupFiles;
  private JRadioButton myRbOverwriteFiles;
  private JCheckBox myCbForceTargetJdk;
  private JCheckBox myCbInlineRuntimeClasspath;
  private JPanel myChunksPanel;
  private JCheckBox myGenerateIdeaHomeProperty;
  private JTextField myOutputFileNameField;
  private final Project myProject;
  @NonNls private static final String SINGLE_FILE_PROPERTY = "GenerateAntBuildDialog.generateSingleFile";
  @NonNls private static final String UI_FORM_PROPERTY = "GenerateAntBuildDialog.enableUiFormCompile";
  @NonNls private static final String FORCE_TARGET_JDK_PROPERTY = "GenerateAntBuildDialog.forceTargetJdk";
  @NonNls private static final String BACKUP_FILES_PROPERTY = "GenerateAntBuildDialog.backupFiles";
  @NonNls private static final String INLINE_RUNTIME_CLASSPATH_PROPERTY = "GenerateAntBuildDialog.inclineRuntiemClasspath";
  @NonNls private static final String GENERATE_IDEA_HOME_PROPERTY = "GenerateAntBuildDialog.generateIdeaHomeProperty";
  @NonNls private static final String OUTPUT_FILE_NAME_PROPERTY = "GenerateAntBuildDialog.outputFileNameProperty";
  private MyTableModel myTableModel;
  private Table myTable;

  public GenerateAntBuildDialog(Project project) {
    super(project, false);
    myProject = project;
    setTitle(CompilerBundle.message("generate.ant.build.title"));
    init();
    loadSettings();
  }

  private List<Chunk<Module>> getCycleChunks() {
    List<Chunk<Module>> chunks =
      ModuleCompilerUtil.getSortedModuleChunks(myProject, Arrays.asList(ModuleManager.getInstance(myProject).getModules()));
    for (Iterator<Chunk<Module>> it = chunks.iterator(); it.hasNext();) {
      final Chunk<Module> chunk = it.next();
      if (chunk.getNodes().size() == 1) {
        it.remove();
      }
    }
    return chunks;
  }

  private void loadSettings() {
    final PropertiesComponent properties = PropertiesComponent.getInstance(myProject);
    if (properties.isValueSet(SINGLE_FILE_PROPERTY)) {
      final boolean singleFile = properties.isTrueValue(SINGLE_FILE_PROPERTY);
      myRbGenerateSingleFileBuild.setSelected(singleFile);
      myRbGenerateMultipleFilesBuild.setSelected(!singleFile);
    }
    if (properties.isValueSet(UI_FORM_PROPERTY)) {
      myCbEnableUIFormsCompilation.setSelected(properties.isTrueValue(UI_FORM_PROPERTY));
    }
    if (properties.isValueSet(FORCE_TARGET_JDK_PROPERTY)) {
      myCbForceTargetJdk.setSelected(properties.isTrueValue(FORCE_TARGET_JDK_PROPERTY));
    }
    if (properties.isValueSet(BACKUP_FILES_PROPERTY)) {
      final boolean backup = properties.isTrueValue(BACKUP_FILES_PROPERTY);
      myRbBackupFiles.setSelected(backup);
      myRbOverwriteFiles.setSelected(!backup);
    }
    if (properties.isValueSet(INLINE_RUNTIME_CLASSPATH_PROPERTY)) {
      myCbInlineRuntimeClasspath.setSelected(properties.isTrueValue(INLINE_RUNTIME_CLASSPATH_PROPERTY));
    }
    if (properties.isValueSet(GENERATE_IDEA_HOME_PROPERTY)) {
      myGenerateIdeaHomeProperty.setSelected(properties.isTrueValue(GENERATE_IDEA_HOME_PROPERTY));
    }
    if (properties.isValueSet(OUTPUT_FILE_NAME_PROPERTY)) {
      myOutputFileNameField.setText(properties.getValue(OUTPUT_FILE_NAME_PROPERTY));
    }
    else {
      myOutputFileNameField.setText(BuildProperties.getProjectBuildFileName(myProject));
    }
  }

  private void saveSettings() {
    final PropertiesComponent properties = PropertiesComponent.getInstance(myProject);
    properties.setValue(SINGLE_FILE_PROPERTY, myRbGenerateSingleFileBuild.isSelected());
    properties.setValue(UI_FORM_PROPERTY, myCbEnableUIFormsCompilation.isSelected());
    properties.setValue(FORCE_TARGET_JDK_PROPERTY, myCbForceTargetJdk.isSelected());
    properties.setValue(BACKUP_FILES_PROPERTY, myRbBackupFiles.isSelected());
    properties.setValue(INLINE_RUNTIME_CLASSPATH_PROPERTY, myCbInlineRuntimeClasspath.isSelected());
    properties.setValue(GENERATE_IDEA_HOME_PROPERTY, myGenerateIdeaHomeProperty.isSelected());
    properties.setValue(OUTPUT_FILE_NAME_PROPERTY, StringUtil.nullize(getOutputFileName()));
  }

  @Override
  public void dispose() {
    saveSettings();
    super.dispose();
  }

  @Override
  protected JComponent createCenterPanel() {
    final ButtonGroup group = new ButtonGroup();
    group.add(myRbGenerateMultipleFilesBuild);
    group.add(myRbGenerateSingleFileBuild);

    final ButtonGroup group1 = new ButtonGroup();
    group1.add(myRbBackupFiles);
    group1.add(myRbOverwriteFiles);

    myRbGenerateMultipleFilesBuild.setSelected(true);
    myRbBackupFiles.setSelected(true);
    myCbEnableUIFormsCompilation.setSelected(true);
    myCbForceTargetJdk.setSelected(true);
    myCbInlineRuntimeClasspath.setSelected(false);

    initChunksPanel();

    return myPanel;
  }

  private void initChunksPanel() {
    List<Chunk<Module>> chunks = getCycleChunks();
    if (chunks.isEmpty()) {
      return;
    }
    myChunksPanel.setLayout(new BorderLayout());
    myChunksPanel.setBorder(
      IdeBorderFactory.createTitledBorder(CompilerBundle.message("generate.ant.build.dialog.cyclic.modules.table.title")));
    JLabel textLabel = new JLabel(CompilerBundle.message("generate.ant.build.dialog.cyclic.modules.table.description"));
    textLabel.setUI(new MultiLineLabelUI());
    textLabel.setBorder(JBUI.Borders.empty(4, 4, 6, 4));
    myChunksPanel.add(textLabel, BorderLayout.NORTH);

    myTableModel = new MyTableModel(chunks);
    myTable = new Table(myTableModel);
    final MyTableCellRenderer cellRenderer = new MyTableCellRenderer();
    final TableColumn nameColumn = myTable.getColumnModel().getColumn(MyTableModel.NAME_COLUMN);
    nameColumn.setCellEditor(ComboBoxTableCellEditor.INSTANCE);
    nameColumn.setCellRenderer(cellRenderer);
    final TableColumn labelColumn = myTable.getColumnModel().getColumn(MyTableModel.NUMBER_COLUMN);
    labelColumn.setCellRenderer(cellRenderer);

    final Dimension preferredSize = new Dimension(myTable.getPreferredSize());
    preferredSize.height = (myTableModel.getRowCount() + 2) * myTable.getRowHeight() + myTable.getTableHeader().getHeight();

    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTable);
    scrollPane.setPreferredSize(preferredSize);
    myChunksPanel.add(scrollPane, BorderLayout.CENTER);
  }

  @Override
  protected void doOKAction() {
    if (myTable != null) {
      TableCellEditor cellEditor = myTable.getCellEditor();
      if (cellEditor != null) {
        cellEditor.stopCellEditing();
      }
    }
    super.doOKAction();
  }

  public boolean isGenerateSingleFileBuild() {
    return myRbGenerateSingleFileBuild.isSelected();
  }

  public boolean isFormsCompilationEnabled() {
    return myCbEnableUIFormsCompilation.isSelected();
  }

  public boolean isForceTargetJdk() {
    return myCbForceTargetJdk.isSelected();
  }

  public boolean isBackupFiles() {
    return myRbBackupFiles.isSelected();
  }

  public boolean isRuntimeClasspathInlined() {
    return myCbInlineRuntimeClasspath.isSelected();
  }

  public String[] getRepresentativeModuleNames() {
    return myTableModel != null ? myTableModel.getModuleRepresentatives() : ArrayUtilRt.EMPTY_STRING_ARRAY;
  }

  /**
   * @return true if user has selected to generate IDEA_HOME property
   */
  public boolean isIdeaHomeGenerated() {
    return myGenerateIdeaHomeProperty.isSelected();
  }

  public String getOutputFileName() {
    return myOutputFileNameField.getText().trim();
  }

  private static class MyTableModel extends AbstractTableModel {
    private static final int NUMBER_COLUMN = 0;
    private static final int NAME_COLUMN = 1;

    private final List<Pair<String, ListWithSelection>> myItems = new ArrayList<>();

    private MyTableModel(List<? extends Chunk<Module>> chunks) {
      for (final Chunk<Module> chunk : chunks) {
        final ListWithSelection<String> item = new ListWithSelection<>();
        for (final Module module : chunk.getNodes()) {
          item.add(module.getName());
        }
        item.selectFirst();
        myItems.add(new Pair<>(createCycleName(chunk), item));
      }
    }

    private static String createCycleName(Chunk<? extends Module> chunk) {
      final StringBuilder buf = new StringBuilder();
      for (Module module : chunk.getNodes()) {
        if (buf.length() > 0) {
          buf.append(", ");
        }
        buf.append(module.getName());
      }
      buf.insert(0, "[");
      buf.append("]");
      return buf.toString();
    }

    public String[] getModuleRepresentatives() {
      final String[] names = new String[myItems.size()];
      int index = 0;
      for (final Pair<String, ListWithSelection> pair : myItems) {
        names[index++] = (String)pair.getSecond().getSelection();
      }
      return names;
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public int getRowCount() {
      return myItems.size();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return columnIndex == 1;
    }

    @Override
    public Class getColumnClass(int columnIndex) {
      switch (columnIndex) {
        case NUMBER_COLUMN:
          return String.class;
        case NAME_COLUMN:
          return ListWithSelection.class;
        default:
          return super.getColumnClass(columnIndex);
      }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
      switch (columnIndex) {
        case NUMBER_COLUMN:
          return myItems.get(rowIndex).getFirst();
        case NAME_COLUMN:
          return myItems.get(rowIndex).getSecond();
        default:
          return null;
      }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      if (columnIndex == NAME_COLUMN) {
        myItems.get(rowIndex).getSecond().select(aValue);
      }
    }

    @Override
    public String getColumnName(int columnIndex) {
      switch (columnIndex) {
        case NUMBER_COLUMN:
          return CompilerBundle.message("generate.ant.build.dialog.cyclic.modules.table.number.column.header");
        case NAME_COLUMN:
          return CompilerBundle.message("generate.ant.build.dialog.cyclic.modules.table.name.column.header");
      }
      return super.getColumnName(columnIndex);
    }
  }

  private static class MyTableCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      if (value instanceof ListWithSelection) {
        value = ((ListWithSelection)value).getSelection();
      }
      final JLabel component = (JLabel)super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      component.setHorizontalAlignment(SwingConstants.CENTER);
      return component;
    }
  }

  @Override
  protected String getHelpId() {
    return HelpID.GENERATE_ANT_BUILD;
  }
}