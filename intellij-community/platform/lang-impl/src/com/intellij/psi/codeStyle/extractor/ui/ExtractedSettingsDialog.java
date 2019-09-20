// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.codeStyle.extractor.ui;

import com.intellij.openapi.application.ApplicationBundle;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.codeStyle.LanguageCodeStyleSettingsProvider;
import com.intellij.psi.codeStyle.extractor.values.Value;
import com.intellij.psi.codeStyle.presentation.CodeStyleSettingPresentation;
import com.intellij.ui.SpeedSearchComparator;
import com.intellij.ui.TreeTableSpeedSearch;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.panels.HorizontalLayout;
import com.intellij.ui.treeStructure.treetable.ListTreeTableModel;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableCellRenderer;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.AbstractTableCellEditor;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import java.util.*;

/**
 * @author Roman.Shein
 */
public class ExtractedSettingsDialog extends DialogWrapper {
  protected CodeStyleSettingsNameProvider myNameProvider;
  protected List<Value> myValues;
  protected DefaultMutableTreeNode myRoot;

  public ExtractedSettingsDialog(@Nullable Project project,
                                 @NotNull CodeStyleSettingsNameProvider nameProvider,
                                 @NotNull List<Value> values) {
    super(project, false);
    myNameProvider = nameProvider;
    myValues = values;
    setModal(true);
    init();
    setTitle("Extracted Code Style Settings");
  }

  @Nullable
  @Override
  protected JComponent createCenterPanel() {
    JComponent result = buildExtractedSettingsTree();
    return result;
  }

  public boolean valueIsSelectedInTree(@NotNull Value value) {
    if (myRoot == null) return false;
    return valueIsSelectedInTree(myRoot, value);
  }

  protected boolean valueIsSelectedInTree(@NotNull TreeNode startNode, @NotNull Value value) {
    for (Enumeration children = startNode.children(); children.hasMoreElements();) {
      Object child = children.nextElement();
      if (child instanceof SettingsTreeNode) {
        SettingsTreeNode settingsChild = (SettingsTreeNode) child;
        if (settingsChild.accepted && value.equals(settingsChild.myValue)) {
          return true;
        }
        if (valueIsSelectedInTree(settingsChild, value)) return true;
      } else if (child instanceof TreeNode) {
        if (valueIsSelectedInTree((TreeNode) child, value)) return true;
      }
    }
    return false;
  }

  public static class SettingsTreeNode extends DefaultMutableTreeNode {
    protected CodeStyleSettingPresentation myRepresentation;
    protected boolean accepted = true;
    protected final String valueString;
    protected final boolean isGroupNode;
    protected final String customTitle;
    protected Value myValue;

    public SettingsTreeNode(String valueString, CodeStyleSettingPresentation representation, boolean isGroupNode, Value value) {
      this(valueString, representation, isGroupNode, null, value);
    }

    public SettingsTreeNode(String valueString, CodeStyleSettingPresentation representation, boolean isGroupNode, String customTitle,
                            Value value) {
      this.valueString = valueString;
      this.myRepresentation = representation;
      this.isGroupNode = isGroupNode;
      this.customTitle = customTitle;
      this.myValue = value;
    }

    public SettingsTreeNode(String title) {
      this(title, null, true, null);
    }

    public boolean isGroupOrTypeNode() {
      return isGroupNode;
    }

    @NotNull
    public String getTitle() {
      return customTitle != null ? customTitle : (myRepresentation == null ? valueString : myRepresentation.getUiName());
    }

    @Nullable
    public String getValueString() {
      return myRepresentation == null ? null : valueString;
    }
  }

  protected static ColumnInfo getTitleColumnInfo() {
    return new ColumnInfo("TITLE") {
      @Nullable
      @Override
      public Object valueOf(Object o) {
        if (o instanceof SettingsTreeNode) {
          return ((SettingsTreeNode) o).getTitle();
        } else {
          return o.toString();
        }
      }

      @Override
      public Class getColumnClass() {
        return TreeTableModel.class;
      }
    };
  }

  protected static class ValueRenderer implements TableCellRenderer {
    private final JLabel myLabel = new JLabel();
    private final JCheckBox myCheckBox = new JCheckBox();
    private final JPanel myPanel = new JPanel(new HorizontalLayout(0));
    {
      myPanel.add(myLabel);
      myPanel.add(myCheckBox);
    }

    @NotNull
    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
      if (table instanceof TreeTable) {
        table.setEnabled(true);
        DefaultMutableTreeNode valueNode = (DefaultMutableTreeNode)((TreeTable) table).getTree().getPathForRow(row).getLastPathComponent();
        if (valueNode instanceof SettingsTreeNode) {
          SettingsTreeNode settingsNode = (SettingsTreeNode) valueNode;
          myLabel.setText(settingsNode.getValueString());
          myCheckBox.setEnabled(true);
          myCheckBox.setSelected(settingsNode.accepted);
        } else {
          myLabel.setBackground(table.getBackground());
          myCheckBox.setEnabled(false);
        }
      }
      return myPanel;
    }
  }

  protected static class ValueEditor extends AbstractTableCellEditor {

    private final JLabel myLabel = new JLabel();
    private final JCheckBox myCheckBox = new JCheckBox();
    private final JPanel myPanel = new JPanel(new HorizontalLayout(0));
    {
      myPanel.add(myLabel);
      myPanel.add(myCheckBox);
    }

    private SettingsTreeNode myCurrentNode;
    private TreeTable myCurrentTree;
    final ActionListener itemChoiceListener = new ActionListener() {
      @Override
      public void actionPerformed(@NotNull ActionEvent e) {
        if (myCurrentNode != null) {
          boolean wasChanged = myCurrentNode.accepted != myCheckBox.isSelected();
          myCurrentNode.accepted = myCheckBox.isSelected();
          if (wasChanged) {
            updateAncestorsUi(myCurrentNode.accepted, myCurrentNode);
            updateChildrenUi(myCurrentNode);
          }
          if (myCurrentTree != null) {
            myCurrentTree.repaint();
          }
        }
      }
    };

    protected void updateAncestorsUi(boolean accepted, SettingsTreeNode node) {
      TreeNode parent = node.getParent();
      if (parent instanceof SettingsTreeNode) {
        SettingsTreeNode settingsParent = (SettingsTreeNode) parent;
        settingsParent.accepted = false;
        if (!accepted) {
          //propagate disabled settings upwards
          updateAncestorsUi(false, settingsParent);
        } else {
          for (Enumeration children = parent.children(); children.hasMoreElements(); ) {
            Object child = children.nextElement();
            if ((child instanceof SettingsTreeNode) && !((SettingsTreeNode) child).accepted) return;
          }
          settingsParent.accepted = true;
          updateAncestorsUi(true, settingsParent);
        }
      }
    }

    protected void updateChildrenUi(SettingsTreeNode node) {
      for (Enumeration children = node.children(); children.hasMoreElements(); ) {
        Object child = children.nextElement();
        if (child instanceof SettingsTreeNode) {
          SettingsTreeNode settingsChild = (SettingsTreeNode) child;
          settingsChild.accepted = node.accepted;
          updateChildrenUi(settingsChild);
        }
      }
    }

    public ValueEditor() {
      myCheckBox.addActionListener(itemChoiceListener);
    }

    @Override
    public Object getCellEditorValue() {
      return myCheckBox.isSelected();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
      DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) ((TreeTable) table).getTree().getPathForRow(row).getLastPathComponent();
      if (treeNode instanceof SettingsTreeNode) {
        myCurrentTree = (TreeTable) table;
        myCurrentNode = (SettingsTreeNode) treeNode;
        myLabel.setText(myCurrentNode.getValueString());
        myCheckBox.setSelected(myCurrentNode.accepted);
      }
      return myPanel;
    }
  }

  private static final ValueRenderer myValueRenderer = new ValueRenderer();
  private static final ValueEditor myValueEditor = new ValueEditor();

  protected static ColumnInfo getValueColumnInfo() {
    return new ColumnInfo("VALUE") {
      @Nullable
      @Override
      public Object valueOf(Object o) {
        if (o instanceof SettingsTreeNode) {
          return ((SettingsTreeNode) o).getValueString();
        } else {
          return null;
        }
      }

      @Override
      public TableCellRenderer getRenderer(Object o) {
        return myValueRenderer;
      }

      @Override
      public TableCellEditor getEditor(Object o) {
        return myValueEditor;
      }

      @Override
      public boolean isCellEditable(Object o) {
        return o instanceof SettingsTreeNode;
      }
    };
  }

  protected JComponent buildExtractedSettingsTree() {

    Collection<Value> unusedValues = new HashSet<>(myValues);
    myRoot = new DefaultMutableTreeNode();
    for (Map.Entry<LanguageCodeStyleSettingsProvider.SettingsType,
      Map<CodeStyleSettingPresentation.SettingsGroup, List<CodeStyleSettingPresentation>>> typeEntry : myNameProvider.mySettings.entrySet()) {
      DefaultMutableTreeNode settingsNode = null;
      for (Map.Entry<CodeStyleSettingPresentation.SettingsGroup, List<CodeStyleSettingPresentation>> groupEntry: typeEntry.getValue().entrySet()) {
        CodeStyleSettingPresentation.SettingsGroup group = groupEntry.getKey();
        List<CodeStyleSettingPresentation> representations = groupEntry.getValue();
        List<CodeStyleSettingPresentation> children = ContainerUtil.emptyList();
        DefaultMutableTreeNode groupNode = null;
        if (group.name == null && !representations.isEmpty()) {
          //there is a setting with name coinciding with group name
          if (representations.size() > 1) {
            children = representations.subList(1, representations.size());
          }
          CodeStyleSettingPresentation headRep = representations.get(0);
          Value myValue = CodeStyleSettingsNameProvider.getValue(headRep, myValues);
          if (myValue == null) {
            //value was not found (was not selected)
            groupNode = new SettingsTreeNode(headRep.getUiName());
          } else {
            groupNode = new SettingsTreeNode(headRep.getUiName());
            groupNode.add(new SettingsTreeNode(headRep.getValueUiName(myValue.value), headRep, true, myValue));
            unusedValues.remove(myValue);
          }
        } else {
          children = representations;
        }
        for (CodeStyleSettingPresentation representation: children) {
          Value myValue = CodeStyleSettingsNameProvider.getValue(representation, myValues);
          if (myValue != null) {
            if (groupNode == null) {
              groupNode = new SettingsTreeNode(group.name);
            }
            groupNode.add(new SettingsTreeNode(representation.getValueUiName(myValue.value), representation, false, myValue));
            unusedValues.remove(myValue);
          }
        }
        if (groupNode != null && !groupNode.isLeaf()) {
          if (settingsNode == null) {
            settingsNode = new SettingsTreeNode(CodeStyleSettingsNameProvider.getSettingsTypeName(typeEntry.getKey()));
          }
          settingsNode.add(groupNode);
        }
      }
      if (settingsNode != null) {
        myRoot.add(settingsNode);
      }
    }

    //TODO: for now, settings without UI presentation are not displayed. Do something about it.
    //unusedValues = ContainerUtil.filter(unusedValues, new Condition<Value>(){
    //  @Override
    //  public boolean value(Value value) {
    //    return value.state == Value.STATE.SELECTED;
    //  }
    //});
    //
    //DefaultMutableTreeNode unnamedNode = null;
    //for (Value value: unusedValues) {
    //  if (unnamedNode == null) {
    //    unnamedNode = new SettingsTreeNode("Settings without UI representation");
    //  }
    //  unnamedNode.add(new SettingsTreeNode(value.value.toString(), null, false, value.name, value));
    //}
    //
    //if (unnamedNode != null) {
    //  myRoot.add(unnamedNode);
    //}

    final ColumnInfo[] COLUMNS = new ColumnInfo[]{getTitleColumnInfo(), getValueColumnInfo()};

    ListTreeTableModel model = new ListTreeTableModel(myRoot, COLUMNS);
    final TreeTable treeTable = new TreeTable(model) {
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

        TableCellRenderer renderer = COLUMNS[column].getRenderer(node);
        return renderer == null ? super.getCellRenderer(row, column) : renderer;
      }

      @Override
      public TableCellEditor getCellEditor(int row, int column) {
        TreePath treePath = getTree().getPathForRow(row);
        if (treePath == null) return super.getCellEditor(row, column);

        Object node = treePath.getLastPathComponent();
        TableCellEditor editor = COLUMNS[column].getEditor(node);
        return editor == null ? super.getCellEditor(row, column) : editor;
      }
    };
    new TreeTableSpeedSearch(treeTable).setComparator(new SpeedSearchComparator(false));

    treeTable.setRootVisible(false);

    final JTree tree = treeTable.getTree();
    tree.setCellRenderer(myTitleRenderer);
    tree.setShowsRootHandles(true);
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

    final Dimension valueSize = new JLabel(ApplicationBundle.message("option.table.sizing.text")).getPreferredSize();
    treeTable.setPreferredScrollableViewportSize(new Dimension(maxWidth + valueSize.width + 10, 20));
    treeTable.setBackground(UIUtil.getPanelBackground());
    treeTable.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));

    final Dimension screenSize = treeTable.getToolkit().getScreenSize();
    JBScrollPane scroller = new JBScrollPane(treeTable) {
      @Override
      public Dimension getMinimumSize() {
        return super.getPreferredSize();
      }
    };
    final Dimension preferredSize = new Dimension(Math.min(screenSize.width / 2, treeTable.getPreferredSize().width),
                                                  Math.min(screenSize.height / 2, treeTable.getPreferredSize().height));
    getRootPane().setPreferredSize(preferredSize);
    return scroller;
  }

  final TreeCellRenderer myTitleRenderer = new CellRenderer();

  public static class CellRenderer implements TreeCellRenderer {

    private final JLabel myLabel = new JLabel();

    @NotNull
    @Override
    public Component getTreeCellRendererComponent(JTree tree,
                                                  Object value,
                                                  boolean selected,
                                                  boolean expanded,
                                                  boolean leaf,
                                                  int row,
                                                  boolean hasFocus) {
      if (value instanceof SettingsTreeNode) {
        SettingsTreeNode node = (SettingsTreeNode) value;
        myLabel.setText(node.getTitle());
        myLabel.setFont(node.isGroupOrTypeNode() ? myLabel.getFont().deriveFont(Font.BOLD) : myLabel.getFont().deriveFont(Font.PLAIN));
      } else {
        myLabel.setText(value.toString());
        myLabel.setFont(myLabel.getFont().deriveFont(Font.BOLD));
      }

      Color foreground = selected ? UIUtil.getTableSelectionForeground() : UIUtil.getTableForeground();
      myLabel.setForeground(foreground);

      return myLabel;
    }
  }
}
