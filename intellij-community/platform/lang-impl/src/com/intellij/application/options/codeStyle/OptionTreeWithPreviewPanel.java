// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import com.intellij.psi.codeStyle.CustomCodeStyleSettings;
import com.intellij.ui.*;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;

/**
 * @author max
 */
public abstract class OptionTreeWithPreviewPanel extends CustomizableLanguageCodeStylePanel {
  private static final Logger LOG = Logger.getInstance(OptionTreeWithPreviewPanel.class);
  protected JTree myOptionsTree;
  protected final ArrayList<BooleanOptionKey> myKeys = new ArrayList<>();
  protected final JPanel myPanel = new JPanel(new GridBagLayout());

  private boolean myShowAllStandardOptions = false;
  private final Set<String> myAllowedOptions = new HashSet<>();
  protected MultiMap<String, CustomBooleanOptionInfo> myCustomOptions = new MultiMap<>();
  protected boolean isFirstUpdate = true;
  private final Map<String, String> myRenamedFields = new THashMap<>();
  private final Map<String, String> myRemappedGroups = new THashMap<>();

  private SpeedSearchHelper mySearchHelper;

  public OptionTreeWithPreviewPanel(CodeStyleSettings settings) {
    super(settings);
  }

  @Override
  protected void init() {
    super.init();

    initTables();

    myOptionsTree = createOptionsTree();

    JScrollPane scrollPane = new JBScrollPane(myOptionsTree) {
      @Override
      public Dimension getMinimumSize() {
        return super.getPreferredSize();
      }
    };
    myPanel.add(scrollPane,
                new GridBagConstraints(0, 0, 1, 1, 0, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                       JBUI.emptyInsets(), 0, 0));

    JPanel previewPanel = createPreviewPanel();

    myPanel.add(previewPanel,
                new GridBagConstraints(1, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                                       JBUI.emptyInsets(), 0, 0));

    installPreviewPanel(previewPanel);
    addPanelToWatch(myPanel);

    isFirstUpdate = false;
  }

  @Override
  public void showAllStandardOptions() {
    myShowAllStandardOptions = true;
    updateOptions(true);
  }

  @Override
  public void showStandardOptions(String... optionNames) {
    if (isFirstUpdate) {
      Collections.addAll(myAllowedOptions, optionNames);
    }
    updateOptions(false, optionNames);
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
                               @Nullable String groupName,
                               @Nullable OptionAnchor anchor,
                               @Nullable String anchorFieldName,
                               Object... options) {
    if (isFirstUpdate) {
      myCustomOptions.putValue(groupName, new CustomBooleanOptionInfo(settingsClass, fieldName, title, groupName, anchor, anchorFieldName));
    }
    enableOption(fieldName);
  }

  @Override
  public void renameStandardOption(String fieldName, String newTitle) {
    if (isFirstUpdate) {
      myRenamedFields.put(fieldName, newTitle);
    }
  }

  protected void updateOptions(boolean showAllStandardOptions, String... allowedOptions) {
    for (BooleanOptionKey key : myKeys) {
      String fieldName = key.field.getName();
      if (key instanceof CustomBooleanOptionKey) {
        key.setEnabled(false);
      }
      else if (showAllStandardOptions) {
        key.setEnabled(true);
      }
      else {
        key.setEnabled(false);
        for (String optionName : allowedOptions) {
          if (fieldName.equals(optionName)) {
            key.setEnabled(true);
            break;
          }
        }
      }
    }
  }

  protected void enableOption(String optionName) {
    for (BooleanOptionKey key : myKeys) {
      if (key.field.getName().equals(optionName)) {
        key.setEnabled(true);
      }
    }
  }

  protected JTree createOptionsTree() {
    DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
    String groupName = "";
    DefaultMutableTreeNode groupNode = null;

    List<BooleanOptionKey> result = sortOptions(orderByGroup(myKeys));

    for (BooleanOptionKey key : result) {
      String newGroupName = key.groupName;
      if (!newGroupName.equals(groupName) || groupNode == null) {
        groupName = newGroupName;
        groupNode = new DefaultMutableTreeNode(newGroupName);
        rootNode.add(groupNode);
      }
      if (isOptionVisible(key)) {
        groupNode.add(new MyToggleTreeNode(key, key.title));
      }
    }

    DefaultTreeModel model = new DefaultTreeModel(rootNode);

    final Tree optionsTree = new Tree(model);
    TreeSpeedSearch speedSearch = new TreeSpeedSearch(
      optionsTree,
      path -> {
        final Object lastPathComponent = path.getLastPathComponent();
        return lastPathComponent instanceof MyToggleTreeNode ? ((MyToggleTreeNode)lastPathComponent).getText() :
               lastPathComponent.toString();
      },
      true);
    mySearchHelper = new SpeedSearchHelper(speedSearch);
    speedSearch.setComparator(new SpeedSearchComparator(false));
    TreeUtil.installActions(optionsTree);
    optionsTree.setRootVisible(false);
    optionsTree.setShowsRootHandles(true);


    optionsTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (!optionsTree.isEnabled()) return;
        if (e.getKeyCode() == KeyEvent.VK_SPACE) {
          TreePath treePath = optionsTree.getLeadSelectionPath();
          selectCheckbox(treePath);
          e.consume();
        }
      }
    });

    new ClickListener() {
      @Override
      public boolean onClick(@NotNull MouseEvent e, int clickCount) {
        if (!optionsTree.isEnabled()) return false;
        TreePath treePath = optionsTree.getPathForLocation(e.getX(), e.getY());
        selectCheckbox(treePath);
        return true;
      }
    }.installOn(optionsTree);

    int row = 0;
    while (row < optionsTree.getRowCount()) {
      optionsTree.expandRow(row);
      row++;
    }

    optionsTree.setCellRenderer(new MyTreeCellRenderer(mySearchHelper));
    optionsTree.setBackground(UIUtil.getPanelBackground());
    optionsTree.setBorder(JBUI.Borders.emptyRight(10));

    return optionsTree;
  }

  private List<BooleanOptionKey> orderByGroup(final List<? extends BooleanOptionKey> options) {
    final List<String> groupOrder = getGroupOrder(options);
    List<BooleanOptionKey> result = new ArrayList<>(options.size());
    result.addAll(options);
    Collections.sort(result, (key1, key2) -> {
      String group1 = key1.groupName;
      String group2 = key2.groupName;
      if (group1 == null) {
        return group2 == null ? 0 : 1;
      }
      if (group2 == null) {
        return -1;
      }
      int index1 = groupOrder.indexOf(group1);
      int index2 = groupOrder.indexOf(group2);
      if (index1 == -1 || index2 == -1) return group1.compareToIgnoreCase(group2);
      return Integer.compare(index1, index2);
    });
    return result;
  }

  protected List<String> getGroupOrder(List<? extends BooleanOptionKey> options) {
    List<String> groupOrder = new ArrayList<>();
    for (BooleanOptionKey each : options) {
      if (each.groupName != null && !groupOrder.contains(each.groupName)) {
        groupOrder.add(each.groupName);
      }
    }
    return groupOrder;
  }

  private void selectCheckbox(TreePath treePath) {
    if (treePath == null) {
      return;
    }
    Object o = treePath.getLastPathComponent();
    if (o instanceof MyToggleTreeNode) {
      MyToggleTreeNode node = (MyToggleTreeNode)o;
      if (!node.isEnabled()) return;
      node.setSelected(!node.isSelected());
      int row = myOptionsTree.getRowForPath(treePath);
      myOptionsTree.repaint(myOptionsTree.getRowBounds(row));
      //updatePreview();
      somethingChanged();
    }
  }

  protected abstract void initTables();

  @Override
  protected void resetImpl(final CodeStyleSettings settings) {
    TreeModel treeModel = myOptionsTree.getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    resetNode(root, settings);
    ((DefaultTreeModel)treeModel).nodeChanged(root);
  }

  private void resetNode(TreeNode node, final CodeStyleSettings settings) {
    if (node instanceof MyToggleTreeNode) {
      resetMyTreeNode((MyToggleTreeNode)node, settings);
      return;
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      resetNode(child, settings);
    }
  }

  private void resetMyTreeNode(MyToggleTreeNode childNode, final CodeStyleSettings settings) {
    try {
      BooleanOptionKey key = (BooleanOptionKey)childNode.getKey();
      childNode.setSelected(key.getValue(settings));
      childNode.setEnabled(key.isEnabled());
    }
    catch (IllegalArgumentException | IllegalAccessException e) {
      LOG.error(e);
    }
  }

  @Override
  public void apply(CodeStyleSettings settings) {
    TreeModel treeModel = myOptionsTree.getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    applyNode(root, settings);
  }

  private static void applyNode(TreeNode node, final CodeStyleSettings settings) {
    if (node instanceof MyToggleTreeNode) {
      applyToggleNode((MyToggleTreeNode)node, settings);
      return;
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      applyNode(child, settings);
    }
  }

  private static void applyToggleNode(MyToggleTreeNode childNode, final CodeStyleSettings settings) {
    BooleanOptionKey key = (BooleanOptionKey)childNode.getKey();
    key.setValue(settings, childNode.isSelected());
  }

  @Override
  public boolean isModified(CodeStyleSettings settings) {
    TreeModel treeModel = myOptionsTree.getModel();
    TreeNode root = (TreeNode)treeModel.getRoot();
    if (isModified(root, settings)) {
      return true;
    }
    return false;
  }

  private static boolean isModified(TreeNode node, final CodeStyleSettings settings) {
    if (node instanceof MyToggleTreeNode) {
      if (isToggleNodeModified((MyToggleTreeNode)node, settings)) {
        return true;
      }
    }
    for (int j = 0; j < node.getChildCount(); j++) {
      TreeNode child = node.getChildAt(j);
      if (isModified(child, settings)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isToggleNodeModified(MyToggleTreeNode childNode, final CodeStyleSettings settings) {
    try {
      BooleanOptionKey key = (BooleanOptionKey)childNode.getKey();
      return childNode.isSelected() != key.getValue(settings);
    }
    catch (IllegalArgumentException | IllegalAccessException e) {
      LOG.error(e);
    }
    return false;
  }

  protected void initBooleanField(@NonNls String fieldName, String title, String groupName) {
    if (myShowAllStandardOptions || myAllowedOptions.contains(fieldName)) {
      doInitBooleanField(fieldName, title, groupName);
    }
  }

  private void doInitBooleanField(@NonNls String fieldName, String title, String groupName) {
    try {
      Class<?> styleSettingsClass = CommonCodeStyleSettings.class;
      Field field = styleSettingsClass.getField(fieldName);
      String actualGroupName = getRemappedGroup(fieldName, groupName);

      BooleanOptionKey key = new BooleanOptionKey(fieldName, 
                                                  getRenamedTitle(actualGroupName, actualGroupName),
                                                  getRenamedTitle(fieldName, title), field);
      myKeys.add(key);
    }
    catch (NoSuchFieldException | SecurityException e) {
      LOG.error(e);
    }
  }

  protected void initCustomOptions(String groupName) {
    for (CustomBooleanOptionInfo option : myCustomOptions.get(groupName)) {
      try {
        Field field = option.settingClass.getField(option.fieldName);
        myKeys.add(new CustomBooleanOptionKey<>(option.fieldName,
                                                getRenamedTitle(groupName, groupName),
                                                getRenamedTitle(option.fieldName, option.title),
                                                option.anchor, option.anchorFieldName,
                                                option.settingClass, field));
      }
      catch (NoSuchFieldException | SecurityException e) {
        LOG.error(e);
      }
    }
  }

  private String getRenamedTitle(String fieldName, String defaultTitle) {
    String renamed = myRenamedFields.get(fieldName);
    return renamed == null ? defaultTitle : renamed;
  }

  protected static class MyTreeCellRenderer implements TreeCellRenderer {
    private final SimpleColoredComponent myLabel;
    private final JCheckBox              myCheckBox;
    private final JPanel                 myCheckBoxPanel;
    private final SpeedSearchHelper      mySearchStringProvider;

    public MyTreeCellRenderer() {
      this(new SpeedSearchHelper());
    }

    public MyTreeCellRenderer(@NotNull SpeedSearchHelper searchStringProvider) {
      myCheckBox = new JCheckBox();
      myCheckBox.setMargin(JBUI.emptyInsets());
      myLabel = new SimpleColoredComponent();
      myCheckBoxPanel = new JPanel();
      myCheckBoxPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
      myCheckBoxPanel.add(myCheckBox);
      myCheckBoxPanel.add(myLabel);
      mySearchStringProvider = searchStringProvider;
    }

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean isSelected, boolean expanded,
                                                  boolean leaf, int row, boolean hasFocus) {
      Color background = isSelected ? UIUtil.getTreeSelectionBackground(hasFocus) : tree.getBackground();
      Color foreground = isSelected ? UIUtil.getTreeSelectionForeground(hasFocus) : tree.getForeground();
      if (value instanceof MyToggleTreeNode) {
        MyToggleTreeNode treeNode = (MyToggleTreeNode)value;

        JToggleButton button = myCheckBox;
        button.setSelected(treeNode.isSelected);
        button.setForeground(foreground);
        button.setBackground(background);
        button.setVisible(true);
        button.setEnabled(tree.isEnabled() && treeNode.isEnabled());

        mySearchStringProvider.setLabelText(myLabel, treeNode.getText(), SimpleTextAttributes.STYLE_PLAIN, foreground, background);
      }
      else {
        myCheckBox.setVisible(false);
        mySearchStringProvider.setLabelText(myLabel, value.toString(), SimpleTextAttributes.STYLE_BOLD, foreground, background);

        myLabel.setEnabled(tree.isEnabled());
      }
      myCheckBoxPanel.setForeground(foreground);
      myCheckBoxPanel.setBackground(background);
      myLabel.setOpaque(true);
      return myCheckBoxPanel;
    }


  }

  private class BooleanOptionKey extends OrderedOption {
    final String groupName;
    String title;
    final Field field;
    private boolean enabled = true;

    BooleanOptionKey(String fieldName, String groupName, String title, Field field) {
      this(fieldName, groupName, title, null, null, field);
    }

    BooleanOptionKey(String fieldName,
                            String groupName,
                            String title,
                            @Nullable OptionAnchor anchor,
                            @Nullable String anchorFiledName,
                            Field field) {
      super(fieldName, anchor, anchorFiledName);
      this.groupName = groupName;
      this.title = title;
      this.field = field;
    }

    public void setValue(CodeStyleSettings settings, Boolean aBoolean) {
      try {
        CommonCodeStyleSettings commonSettings = settings.getCommonSettings(getDefaultLanguage());
        field.set(commonSettings, aBoolean);
      }
      catch (IllegalAccessException e) {
        LOG.error(e);
      }
    }

    public boolean getValue(CodeStyleSettings settings) throws IllegalAccessException {
      CommonCodeStyleSettings commonSettings = settings.getCommonSettings(getDefaultLanguage());
      return field.getBoolean(commonSettings);
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isEnabled() {
      return this.enabled;
    }
  }

  private static class CustomBooleanOptionInfo {
    @NotNull final Class<? extends CustomCodeStyleSettings> settingClass;
    @NotNull final String fieldName;
    @NotNull final String title;
    @Nullable final String groupName;
    @Nullable final OptionAnchor anchor;
    @Nullable final String anchorFieldName;

    private CustomBooleanOptionInfo(@NotNull Class<? extends CustomCodeStyleSettings> settingClass,
                                    @NotNull String fieldName,
                                    @NotNull String title,
                                    @Nullable String groupName,
                                    @Nullable OptionAnchor anchor,
                                    @Nullable String anchorFieldName) {
      this.settingClass = settingClass;
      this.fieldName = fieldName;
      this.title = title;
      this.groupName = groupName;
      this.anchor = anchor;
      this.anchorFieldName = anchorFieldName;
    }
  }

  private class CustomBooleanOptionKey<T extends CustomCodeStyleSettings> extends BooleanOptionKey {
    private final Class<T> mySettingsClass;

    CustomBooleanOptionKey(String fieldName,
                                  String groupName,
                                  String title,
                                  OptionAnchor anchor,
                                  String anchorFieldName,
                                  Class<T> settingsClass,
                                  Field field) {
      super(fieldName, groupName, title, anchor, anchorFieldName, field);
      mySettingsClass = settingsClass;
    }

    @Override
    public void setValue(CodeStyleSettings settings, Boolean aBoolean) {
      final CustomCodeStyleSettings customSettings = settings.getCustomSettings(mySettingsClass);
      try {
        field.set(customSettings, aBoolean);
      }
      catch (IllegalAccessException e) {
        LOG.error(e);
      }
    }

    @Override
    public boolean getValue(CodeStyleSettings settings) throws IllegalAccessException {
      final CustomCodeStyleSettings customSettings = settings.getCustomSettings(mySettingsClass);
      return field.getBoolean(customSettings);
    }
  }

  private static class MyToggleTreeNode extends DefaultMutableTreeNode {
    private final Object myKey;
    private final String myText;
    private boolean isSelected;
    private boolean isEnabled = true;

    MyToggleTreeNode(Object key, String text) {
      myKey = key;
      myText = text;
    }

    public Object getKey() {
      return myKey;
    }

    public String getText() {
      return myText;
    }

    public void setSelected(boolean val) {
      isSelected = val;
    }

    public boolean isSelected() {
      return isSelected;
    }

    public void setEnabled(boolean val) {
      isEnabled = val;
    }

    public boolean isEnabled() {
      return isEnabled;
    }
  }

  @Override
  public JComponent getPanel() {
    return myPanel;
  }

  @NotNull
  @Override
  public Set<String> processListOptions() {
    Set<String> result = new HashSet<>();
    for (BooleanOptionKey key : myKeys) {
      result.add(key.title);
      if (key.groupName != null) {
        result.add(key.groupName);
      }
    }
    result.addAll(myRenamedFields.values());
    for (String groupName : myCustomOptions.keySet()) {
      result.add(groupName);
      for (CustomBooleanOptionInfo trinity : myCustomOptions.get(groupName)) {
        result.add(trinity.title);
      }
    }
    return result;
  }

  protected boolean shouldHideOptions() {
    return false;
  }

  private boolean isOptionVisible(BooleanOptionKey key) {
    if (!shouldHideOptions()) return true;
    if (myShowAllStandardOptions || myAllowedOptions.contains(key.getOptionName())) return true;
    for (CustomBooleanOptionInfo customOption : myCustomOptions.get(key.groupName)) {
      if (customOption.fieldName.equals(key.getOptionName())) return true;
    }
    return false;
  }

  @Override
  public void moveStandardOption(String fieldName, String newGroup) {
    myRemappedGroups.put(fieldName, newGroup);
  }

  private String getRemappedGroup(String fieldName, String defaultName) {
    return myRemappedGroups.getOrDefault(fieldName, defaultName);
  }

  @Override
  public void highlightOptions(@NotNull String searchString) {
    mySearchHelper.find(searchString);
  }

}
