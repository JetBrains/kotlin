// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.ui.*;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.editors.JBComboBoxTableCellEditorComponent;
import com.intellij.ui.components.fields.IntegerField;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableCellRenderer;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashMap;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.accessibility.AccessibleRole;
import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * @author max
 */
public abstract class OptionTableWithPreviewPanel extends CustomizableLanguageCodeStylePanel {
  private static final Logger LOG = Logger.getInstance(OptionTableWithPreviewPanel.class);

  private final static KeyStroke ENTER_KEY_STROKE = KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0, false);

  protected TreeTable myTreeTable;
  private final JPanel myPanel = new JPanel();

  private final List<Option> myOptions = new ArrayList<>();
  private final List<Option> myCustomOptions = new ArrayList<>();
  private final Set<String> myAllowedOptions = new THashSet<>();
  private final Map<String, String> myRenamedFields = new THashMap<>();
  private boolean myShowAllStandardOptions;
  protected boolean isFirstUpdate = true;

  private SpeedSearchHelper mySearchHelper;

  public OptionTableWithPreviewPanel(CodeStyleSettings settings) {
    super(settings);
  }

  @Override
  protected void init() {
    super.init();

    myPanel.setLayout(new GridBagLayout());
    initTables();

    myTreeTable = createOptionsTree(getSettings());
    myTreeTable.setBackground(UIUtil.getPanelBackground());
    myTreeTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
    JBScrollPane scrollPane = new JBScrollPane(myTreeTable) {
      @Override
      public Dimension getMinimumSize() {
        return super.getPreferredSize();
      }
    };
    myPanel.add(scrollPane
      , new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                               JBUI.emptyInsets(), 0, 0));

    final JPanel previewPanel = createPreviewPanel();
    myPanel.add(previewPanel,
                new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                       JBUI.emptyInsets(), 0, 0));

    installPreviewPanel(previewPanel);
    addPanelToWatch(myPanel);

    isFirstUpdate = false;
    customizeSettings();
  }

  @Override
  protected void resetDefaultNames() {
    myRenamedFields.clear();
  }

  @Override
  public void showAllStandardOptions() {
    myShowAllStandardOptions = true;
    for (Option each : myOptions) {
      each.setEnabled(true);
    }
    for (Option each : myCustomOptions) {
      each.setEnabled(false);
    }
  }

  @Override
  public void showStandardOptions(String... optionNames) {
    Collections.addAll(myAllowedOptions, optionNames);
    for (Option each : myOptions) {
      each.setEnabled(false);
      for (String optionName : optionNames) {
        if (each.getOptionName().equals(optionName)) {
          each.setEnabled(true);
        }
      }
    }
    for (Option each : myCustomOptions) {
      each.setEnabled(false);
    }
  }

  @Override
  public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass,
                               String fieldName,
                               String title,
                               String groupName, Object... options) {
    showCustomOption(settingsClass, fieldName, title, groupName, null, null, options);
  }


  @Override
  public void showCustomOption(Class<? extends CustomCodeStyleSettings> settingsClass,
                               String fieldName,
                               String title,
                               String groupName,
                               @Nullable OptionAnchor anchor,
                               @Nullable String anchorFieldName,
                               Object... options) {
    if (isFirstUpdate) {
      Option option;
      if (options.length == 2) {
        option =
          new SelectionOption(settingsClass, fieldName, title, groupName, anchor, anchorFieldName, (String[])options[0], (int[])options[1]);
      }
      else {
        option = new BooleanOption(settingsClass, fieldName, title, groupName, anchor, anchorFieldName);
      }
      myCustomOptions.add(option);
      option.setEnabled(true);
    }
    else {
      for (Option each : myCustomOptions) {
        if (each instanceof FieldOption && ((FieldOption)each).clazz == settingsClass && each.getOptionName().equals(fieldName)) {
          each.setEnabled(true);
        }
      }
    }
  }

  @Override
  public void renameStandardOption(String fieldName, String newTitle) {
    myRenamedFields.put(fieldName, newTitle);
  }

  public void showOption(@NotNull String optionName) {
    myAllowedOptions.add(optionName);
  }

  protected TreeTable createOptionsTree(CodeStyleSettings settings) {
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    Map<String, DefaultMutableTreeNode> groupsMap = new THashMap<>();

    List<Option> sorted = sortOptions(ContainerUtil.concat(myOptions, myCustomOptions));
    for (Option each : sorted) {
      if (!(myCustomOptions.contains(each) || myAllowedOptions.contains(each.getOptionName()) || myShowAllStandardOptions)) continue;

      String group = each.groupName;
      MyTreeNode newNode = new MyTreeNode(each, each.title, settings);

      DefaultMutableTreeNode groupNode = groupsMap.get(group);
      if (groupNode != null) {
        groupNode.add(newNode);
      }
      else {
        String groupName;

        if (group == null) {
          groupName = each.title;
          groupNode = newNode;
        }
        else {
          groupName = group;
          groupNode = new DefaultMutableTreeNode(groupName);
          groupNode.add(newNode);
        }
        groupsMap.put(groupName, groupNode);
        rootNode.add(groupNode);
      }
    }

    ListTreeTableModel model = new ListTreeTableModel(rootNode, COLUMNS);
    TreeTable treeTable = new TreeTable(model) {
      @Override
      public TreeTableCellRenderer createTableRenderer(TreeTableModel treeTableModel) {
        TreeTableCellRenderer tableRenderer = super.createTableRenderer(treeTableModel);
        tableRenderer.setRootVisible(false);
        tableRenderer.setShowsRootHandles(true);
        return tableRenderer;
      }

      @Override
      public TableCellRenderer getCellRenderer(int row, int column) {
        TreePath treePath = getTree().getPathForRow(row);
        if (treePath == null) return super.getCellRenderer(row, column);

        Object node = treePath.getLastPathComponent();

        @SuppressWarnings("unchecked")
        TableCellRenderer renderer = COLUMNS[column].getRenderer(node);
        return renderer == null ? super.getCellRenderer(row, column) : renderer;
      }

      @Override
      public TableCellEditor getCellEditor(int row, int column) {
        TreePath treePath = getTree().getPathForRow(row);
        if (treePath == null) return super.getCellEditor(row, column);

        Object node = treePath.getLastPathComponent();
        @SuppressWarnings("unchecked")
        TableCellEditor editor = COLUMNS[column].getEditor(node);
        return editor == null ? super.getCellEditor(row, column) : editor;
      }
    };
    TreeTableSpeedSearch speedSearch = new TreeTableSpeedSearch(treeTable);
    speedSearch.setComparator(new SpeedSearchComparator(false));
    mySearchHelper = new SpeedSearchHelper(speedSearch);

    treeTable.setRootVisible(false);

    final JTree tree = treeTable.getTree();
    TreeCellRenderer titleRenderer = new MyTitleRenderer(mySearchHelper);
    tree.setCellRenderer(titleRenderer);
    tree.setShowsRootHandles(true);
    //myTreeTable.setRowHeight(new JComboBox(new String[]{"Sample Text"}).getPreferredSize().height);
    treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    treeTable.setTableHeader(null);

    TreeUtil.expandAll(tree);

    treeTable.getColumnModel().getSelectionModel().setAnchorSelectionIndex(1);
    treeTable.getColumnModel().getSelectionModel().setLeadSelectionIndex(1);

    int maxWidth = tree.getPreferredScrollableViewportSize().width + 10;
    final TableColumn titleColumn = treeTable.getColumnModel().getColumn(0);
    titleColumn.setPreferredWidth(maxWidth);
    titleColumn.setMinWidth(maxWidth);
    titleColumn.setMaxWidth(maxWidth);
    titleColumn.setResizable(false);

    //final TableColumn levelColumn = treeTable.getColumnModel().getColumn(1);
    //TODO[max]: better preffered size...
    //TODO[kb]: Did I fixed it by making the last column floating?
    //levelColumn.setPreferredWidth(valueSize.width);
    //levelColumn.setMaxWidth(valueSize.width);
    //levelColumn.setMinWidth(valueSize.width);
    //levelColumn.setResizable(false);

    final Dimension valueSize = new JLabel(ApplicationBundle.message("option.table.sizing.text")).getPreferredSize();
    treeTable.setPreferredScrollableViewportSize(new Dimension(maxWidth + valueSize.width + 10, 20));

    return treeTable;
  }

  private String getRenamedTitle(String fieldOrGroupName, String defaultName) {
    String result = myRenamedFields.get(fieldOrGroupName);
    return result == null ? defaultName : result;
  }

  protected abstract void initTables();

  private static void resetNode(TreeNode node, CodeStyleSettings settings) {
    if (node instanceof MyTreeNode) {
      ((MyTreeNode)node).reset(settings);
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      resetNode(child, settings);
    }
  }

  private static void applyNode(TreeNode node, final CodeStyleSettings settings) {
    if (node instanceof MyTreeNode) {
      ((MyTreeNode)node).apply(settings);
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      applyNode(child, settings);
    }
  }

  private static boolean isModified(TreeNode node, final CodeStyleSettings settings) {
    if (node instanceof MyTreeNode) {
      if (((MyTreeNode)node).isModified(settings)) return true;
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      if (isModified(child, settings)) {
        return true;
      }
    }
    return false;
  }

  protected void addOption(@NotNull String fieldName, @NotNull String title) {
    addOption(fieldName, title, null);
  }

  protected void addOption(@NotNull String fieldName, @NotNull String title, @NotNull String[] options, @NotNull int[] values) {
    addOption(fieldName, title, null, options, values);
  }

  protected void addOption(@NotNull String fieldName,
                           @NotNull String title,
                           @Nullable String groupName,
                           int minValue,
                           int maxValue,
                           int defaultValue,
                           @Nullable Function<? super Integer, String> defaultValueRenderer) {
    myOptions.add(new IntOption(null, fieldName, title, groupName, null, null, minValue, maxValue, defaultValue, defaultValueRenderer));
  }

  protected void addOption(@NotNull String fieldName, @NotNull String title, @Nullable String groupName) {
    myOptions.add(new BooleanOption(null, fieldName, title, groupName, null, null));
  }

  protected void addOption(@NotNull String fieldName, @NotNull String title, @Nullable String groupName,
                           @NotNull String[] options, @NotNull int[] values) {
    myOptions.add(new SelectionOption(null, fieldName, title, groupName, null, null, options, values));
  }

  protected void addCustomOption(@NotNull Option option) {
    myOptions.add(option);
  }

  protected abstract static class Option extends OrderedOption {
    @NotNull final String title;
    @Nullable final String groupName;
    private boolean myEnabled = false;

    protected Option(@NotNull String optionName,
                     @NotNull String title,
                     @Nullable String groupName,
                     @Nullable OptionAnchor anchor,
                     @Nullable String anchorOptionName) {
      super(optionName, anchor, anchorOptionName);
      this.title = title;
      this.groupName = groupName;
    }

    public void setEnabled(boolean enabled) {
      myEnabled = enabled;
    }

    public boolean isEnabled() {
      return myEnabled;
    }

    public abstract Object getValue(CodeStyleSettings settings);

    public abstract void setValue(Object value, CodeStyleSettings settings);
  }

  private abstract class FieldOption extends Option {
    @Nullable final Class<? extends CustomCodeStyleSettings> clazz;
    @NotNull final Field field;

    FieldOption(@Nullable Class<? extends CustomCodeStyleSettings> clazz,
                  @NotNull String fieldName,
                  @NotNull String title,
                  @Nullable String groupName,
                  @Nullable OptionAnchor anchor,
                  @Nullable String anchorFiledName) {
      super(fieldName, title, groupName, anchor, anchorFiledName);
      this.clazz = clazz;

      try {
        Class styleSettingsClass = clazz == null ? CommonCodeStyleSettings.class : clazz;
        this.field = styleSettingsClass.getField(fieldName);
      }
      catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
    }

    protected Object getSettings(CodeStyleSettings settings) {
      if (clazz != null) return settings.getCustomSettings(clazz);
      return settings.getCommonSettings(getDefaultLanguage());
    }

  }

  private class BooleanOption extends FieldOption {
    private BooleanOption(Class<? extends CustomCodeStyleSettings> clazz,
                          @NotNull String fieldName,
                          @NotNull String title,
                          @Nullable String groupName,
                          @Nullable OptionAnchor anchor,
                          @Nullable String anchorFiledName) {
      super(clazz, fieldName, title, groupName, anchor, anchorFiledName);
    }

    @Override
    public Object getValue(CodeStyleSettings settings) {
      try {
        return field.getBoolean(getSettings(settings));
      }
      catch (IllegalAccessException ignore) {
        return null;
      }
    }

    @Override
    public void setValue(Object value, CodeStyleSettings settings) {
      try {
        field.setBoolean(getSettings(settings), ((Boolean)value).booleanValue());
      }
      catch (IllegalAccessException ignored) {
      }
    }
  }

  private class SelectionOption extends FieldOption {
    @NotNull final String[] options;
    @NotNull final int[] values;

    SelectionOption(Class<? extends CustomCodeStyleSettings> clazz,
                           @NotNull String fieldName,
                           @NotNull String title,
                           @Nullable String groupName,
                           @Nullable OptionAnchor anchor,
                           @Nullable String anchorFiledName,
                           @NotNull String[] options,
                           @NotNull int[] values) {
      super(clazz, fieldName, title, groupName, anchor, anchorFiledName);
      this.options = options;
      this.values = values;
    }

    @Override
    public Object getValue(CodeStyleSettings settings) {
      try {
        int value = field.getInt(getSettings(settings));
        for (int i = 0; i < values.length; i++) {
          if (values[i] == value) return options[i];
        }
        LOG.error("Invalid option value " + value + " for " + field.getName());
      }
      catch (IllegalAccessException ignore) {
      }
      return null;
    }

    @Override
    public void setValue(Object value, CodeStyleSettings settings) {
      try {
        for (int i = 0; i < values.length; i++) {
          if (options[i].equals(value)) {
            field.setInt(getSettings(settings), values[i]);
            return;
          }
        }
        LOG.error("Invalid option value " + value + " for " + field.getName());
      }
      catch (IllegalAccessException ignore) {
      }
    }
  }

  private class IntOption extends FieldOption {

    private final int myMinValue;
    private final int myMaxValue;
    private final int myDefaultValue;
    @Nullable private final Function<? super Integer, String> myDefaultValueRenderer;

    IntOption(Class<? extends CustomCodeStyleSettings> clazz,
                     @NotNull String fieldName,
                     @NotNull String title,
                     @Nullable String groupName, 
                     @Nullable OptionAnchor anchor, 
                     @Nullable String anchorFiledName,
                     int minValue,
                     int maxValue,
                     int defaultValue,
                     @Nullable Function<? super Integer, String> defaultValueRenderer) {
      super(clazz, fieldName, title, groupName, anchor, anchorFiledName);
      myMinValue = minValue;
      myMaxValue = maxValue;
      myDefaultValue = defaultValue;
      myDefaultValueRenderer = defaultValueRenderer;
    }

    @Override
    public Object getValue(CodeStyleSettings settings) {
      try {
        return field.getInt(getSettings(settings));
      }
      catch (IllegalAccessException e) {
        return null;
      }      
    }

    @Override
    public void setValue(Object value, CodeStyleSettings settings) {
      //noinspection EmptyCatchBlock
      try {
        if (value instanceof Integer) {
          field.setInt(getSettings(settings), ((Integer)value).intValue());
        }
        else {
          field.setInt(getSettings(settings), myDefaultValue);
        }
      }
      catch (IllegalAccessException e) {
      }
    }

    public int getMinValue() {
      return myMinValue;
    }

    public int getMaxValue() {
      return myMaxValue;
    }

    public int getDefaultValue() {
      return myDefaultValue;
    }

    public boolean isDefaultValue(Object value) {
      return value instanceof Integer && ((Integer)value).intValue() == myDefaultValue;
    }

    @Nullable
    public String getDefaultValueText() {
      return myDefaultValueRenderer != null ? myDefaultValueRenderer.apply(myDefaultValue) : null;
    }
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  public final ColumnInfo TITLE = new ColumnInfo("TITLE") {
    @Override
    public Object valueOf(Object o) {
      if (o instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)o;
        return node.getText();
      }
      return o.toString();
    }

    @Override
    public Class getColumnClass() {
      return TreeTableModel.class;
    }
  };

  @SuppressWarnings({"HardCodedStringLiteral"})
  public final ColumnInfo VALUE = new ColumnInfo("VALUE") {
    private final TableCellEditor myEditor = new MyValueEditor();
    private final TableCellRenderer myRenderer = new MyValueRenderer();

    @Override
    public Object valueOf(Object o) {
      if (o instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)o;
        return node.getValue();
      }

      return null;
    }

    @Override
    public TableCellRenderer getRenderer(Object o) {
      return myRenderer;
    }

    @Override
    public TableCellEditor getEditor(Object item) {
      return myEditor;
    }

    @Override
    public boolean isCellEditable(Object o) {
      return o instanceof MyTreeNode && ((MyTreeNode)o).isEnabled();
    }

    @Override
    public void setValue(Object o, Object o1) {
      MyTreeNode node = (MyTreeNode)o;
      node.setValue(o1);
    }
  };

  public final ColumnInfo[] COLUMNS = new ColumnInfo[]{TITLE, VALUE};

  protected static class MyTreeNode extends DefaultMutableTreeNode {
    private final Option myKey;
    private final String myText;
    private Object myValue;

    public MyTreeNode(Option key, String text, CodeStyleSettings settings) {
      myKey = key;
      myText = text;
      myValue = key.getValue(settings);
      setUserObject(myText);
    }

    public Option getKey() {
      return myKey;
    }

    public String getText() {
      return myText;
    }

    public Object getValue() {
      return myValue;
    }

    public void setValue(Object value) {
      myValue = value;
    }

    public void reset(CodeStyleSettings settings) {
      setValue(myKey.getValue(settings));
    }

    public boolean isModified(final CodeStyleSettings settings) {
      return myValue != null && !myValue.equals(myKey.getValue(settings));
    }

    public void apply(final CodeStyleSettings settings) {
      myKey.setValue(myValue, settings);
    }

    public boolean isEnabled() {
      return myKey.isEnabled();
    }
  }

  private class MyValueRenderer implements TableCellRenderer {
    private JTable myTable;
    private int myRow;
    private int myColumn;
    private final OptionsLabel myComboBox = new OptionsLabel();
    private final JCheckBox myCheckBox = new JBCheckBox();
    private final JPanel myEmptyLabel = new JPanel();
    private final JLabel myIntLabel = new JLabel();

    MyValueRenderer() {
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myComboBox);
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myCheckBox);
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myIntLabel);
    }

    @NotNull
    @Override
    public Component getTableCellRendererComponent(@NotNull JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      myTable = table;
      myRow = row;
      myColumn = column;
      boolean isEnabled = true;
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)((TreeTable)table).getTree().
        getPathForRow(row).getLastPathComponent();
      Option key = null;
      if (node instanceof MyTreeNode) {
        isEnabled = ((MyTreeNode)node).isEnabled();
        key = ((MyTreeNode)node).getKey();
      }
      if (!table.isEnabled()) {
        isEnabled = false;
      }

      Color background = table.getBackground();
      if (key != null && value != null) {
        JComponent customRenderer = getCustomValueRenderer(key.getOptionName(), value);
        if (customRenderer != null) {
          return customRenderer;
        }
      }
      if (value instanceof Boolean) {
        myCheckBox.setSelected(((Boolean)value).booleanValue());
        myCheckBox.setBackground(background);
        myCheckBox.setEnabled(isEnabled);
        return myCheckBox;
      }
      else if (value instanceof String) {
        myComboBox.setText((String)value);
        myComboBox.setBackground(background);
        myComboBox.setEnabled(isEnabled);
        return myComboBox;
      }
      else if (value instanceof Integer) {
        if (key instanceof IntOption && ((IntOption)key).isDefaultValue(value)) {
          myIntLabel.setText(((IntOption)key).getDefaultValueText());
        }
        else {
          myIntLabel.setText(value.toString());
        }
        return myIntLabel;
      }

      myEmptyLabel.setBackground(background);
      return myEmptyLabel;
    }

    protected class OptionsLabel extends JLabel {
      @Override
      public AccessibleContext getAccessibleContext() {
        if (accessibleContext == null) {
          accessibleContext = new AccessibleOptionsLabel();
        }
        return accessibleContext;
      }

      protected class AccessibleOptionsLabel extends AccessibleJLabel implements AccessibleAction {
        @Override
        public AccessibleRole getAccessibleRole() {
          return AccessibleRole.PUSH_BUTTON;
        }

        @Override
        public AccessibleAction getAccessibleAction() {
          return this;
        }

        @Override
        public int getAccessibleActionCount() {
          return 1;
        }

        @Override
        public String getAccessibleActionDescription(int i) {
          if (i == 0) {
            return UIManager.getString("AbstractButton.clickText");
          } else {
            return null;
          }
        }

        @Override
        public boolean doAccessibleAction(int i) {
          if (i == 0) {
            myTable.editCellAt(myRow, myColumn);
            return true;
          } else {
            return false;
          }
        }
      }
    }
  }


  @Nullable
  protected JComponent getCustomValueRenderer(@NotNull String optionName, @NotNull Object value) {
    return null;
  }

  /**
   * @author Konstantin Bulenkov
   */
  private class MyValueEditor extends AbstractTableCellEditor {
    public static final String STOP_CELL_EDIT_ACTION_KEY = "stopEdit";
    private final JCheckBox myBooleanEditor = new JBCheckBox();
    private final JBComboBoxTableCellEditorComponent myOptionsEditor = new JBComboBoxTableCellEditorComponent();
    private final IntegerField myIntOptionsEditor = new IntegerField();
    private JComponent myCurrentEditor = null;
    private MyTreeNode myCurrentNode = null;
    private final AbstractAction STOP_CELL_EDIT_ACTION = new AbstractAction() {
      @Override
      public void actionPerformed(ActionEvent e) {
        stopCellEditing();
      }
    };

    MyValueEditor() {
      final ActionListener itemChoosen = new ActionListener() {
        @Override
        public void actionPerformed(@NotNull ActionEvent e) {
          if (myCurrentNode != null) {
            myCurrentNode.setValue(getCellEditorValue());
            somethingChanged();
          }
        }
      };
      myBooleanEditor.addActionListener(itemChoosen);
      myOptionsEditor.addActionListener(itemChoosen);
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myBooleanEditor);
      UIUtil.applyStyle(UIUtil.ComponentStyle.SMALL, myOptionsEditor);
      UIUtil.applyStyle(UIUtil.ComponentStyle.MINI, myIntOptionsEditor);
    }

    @Override
    public Object getCellEditorValue() {
      if (myCurrentEditor == myOptionsEditor) {
        return myOptionsEditor.getEditorValue();
      }
      else if (myCurrentEditor == myBooleanEditor) {
        return myBooleanEditor.isSelected();
      }
      else if (myCurrentEditor == myIntOptionsEditor) {
        return myIntOptionsEditor.getValue();
      }
      else {
        Object value = getCustomNodeEditorValue(myCurrentEditor);
        if (value != null) return value;
      }

      return null;
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      final DefaultMutableTreeNode defaultNode = (DefaultMutableTreeNode)((TreeTable)table).getTree().
        getPathForRow(row).getLastPathComponent();
      myCurrentEditor = null;
      myCurrentNode = null;
      if (defaultNode instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)defaultNode;
        myCurrentNode = node;
        if (node.getKey() instanceof BooleanOption) {
          myCurrentEditor = myBooleanEditor;
          myBooleanEditor.setSelected(node.getValue() == Boolean.TRUE);
          myBooleanEditor.setEnabled(node.isEnabled());
        }
        else if (node.getKey() instanceof IntOption) {
          IntOption intOption = (IntOption)node.getKey();
          myCurrentEditor = myIntOptionsEditor;
          myIntOptionsEditor.setCanBeEmpty(true);
          myIntOptionsEditor.setMinValue(intOption.getMinValue());
          myIntOptionsEditor.setMaxValue(intOption.getMaxValue());
          myIntOptionsEditor.setDefaultValue(intOption.getDefaultValue());
          myIntOptionsEditor.setValue((Integer)node.getValue());
        }
        else {
          myCurrentEditor = getCustomNodeEditor(node);
        }
        if (myCurrentEditor == null) {
          myCurrentEditor = myOptionsEditor;
          myOptionsEditor.setCell(table, row, column);
          myOptionsEditor.setText(String.valueOf(node.getValue()));
          //noinspection ConfusingArgumentToVarargsMethod
          myOptionsEditor.setOptions(((SelectionOption)node.getKey()).options);
          myOptionsEditor.setDefaultValue(node.getValue());
        }
      }

      if (myCurrentEditor != null) {
        myCurrentEditor.setBackground(table.getBackground());
        if (myCurrentEditor instanceof JTextField) {
          myCurrentEditor.getInputMap().put(ENTER_KEY_STROKE, STOP_CELL_EDIT_ACTION_KEY);
          myCurrentEditor.getActionMap().put(STOP_CELL_EDIT_ACTION_KEY, STOP_CELL_EDIT_ACTION);
        }
      }
      return myCurrentEditor;
    }
  }

  @Nullable
  protected JComponent getCustomNodeEditor(@NotNull MyTreeNode node) {
    return null;
  }

  @Nullable
  protected Object getCustomNodeEditorValue(@NotNull JComponent customEditor) {
    return null;
  }

  @Override
  public void apply(CodeStyleSettings settings) throws ConfigurationException {
    TableCellEditor editor = myTreeTable.getCellEditor();
    if (editor != null && !editor.stopCellEditing()) {
      throw new ConfigurationException("Editing cannot be stopped");
    }
    TreeModel treeModel = myTreeTable.getTree().getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    applyNode(root, settings);
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    TableCellEditor editor = myTreeTable.getCellEditor();
    if (editor != null) {
      return true; // to allow stop editing in #apply
    }
    TreeModel treeModel = myTreeTable.getTree().getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    if (isModified(root, settings)) {
      return true;
    }
    return false;
  }

  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  @Override
  protected void resetImpl(final CodeStyleSettings settings) {
    TreeModel treeModel = myTreeTable.getTree().getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    resetNode(root, settings);
    ((DefaultTreeModel)treeModel).nodeChanged(root);
  }

  @NotNull
  @Override
  public Set<String> processListOptions() {
    Set<String> options = new HashSet<>();
    collectOptions(options, myOptions);
    collectOptions(options, myCustomOptions);
    return options;
  }

  private static void collectOptions(Set<? super String> optionNames, final List<? extends Option> optionList) {
    for (Option option : optionList) {
      if (option.groupName != null) {
        optionNames.add(option.groupName);
      }
      optionNames.add(option.title);
    }
  }

  private class MyTitleRenderer extends ColoredTreeCellRenderer {

    private final SpeedSearchHelper mySearchHelper;

    private MyTitleRenderer(SpeedSearchHelper helper) {
      mySearchHelper = helper;
    }

    @Override
    public void customizeCellRenderer(@NotNull JTree tree,
                                      Object value,
                                      boolean selected,
                                      boolean expanded,
                                      boolean leaf,
                                      int row,
                                      boolean hasFocus) {
      SimpleTextAttributes attributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
      String text;
      if (value instanceof MyTreeNode) {
        MyTreeNode node = (MyTreeNode)value;
        if (node.getKey().groupName == null) {
          attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
        }
        text = getRenamedTitle(node.getKey().getOptionName(), node.getText());
        setEnabled(node.isEnabled());
      }
      else {
        attributes = SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES;
        text = getRenamedTitle(value.toString(), value.toString());
        setEnabled(true);
      }
      mySearchHelper.setLabelText(this, text, attributes.getStyle(), attributes.getFgColor(), attributes.getBgColor());
    }
  }

  @Override
  public void highlightOptions(@NotNull String searchString) {
    mySearchHelper.find(searchString);
  }
}
