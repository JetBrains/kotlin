// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.tools;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.CompoundScheme;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.*;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.PlatformIcons;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public abstract class BaseToolsPanel<T extends Tool> extends JPanel {
  enum Direction {
    UP {
      @Override
      public boolean isAvailable(final int index, final int childCount) {
        return index != 0;
      }

      @Override
      public int newIndex(final int index) {
        return index - 1;
      }
    },
    DOWN {
      @Override
      public boolean isAvailable(final int index, final int childCount) {
        return index < childCount - 1;
      }

      @Override
      public int newIndex(final int index) {
        return index + 1;
      }
    };

    public abstract boolean isAvailable(final int index, final int childCount);

    public abstract int newIndex(final int index);
  }

  private final CheckboxTree myTree;

  private final AnActionButton myAddButton;
  private final AnActionButton myCopyButton;
  private final AnActionButton myEditButton;
  private final AnActionButton myMoveUpButton;
  private final AnActionButton myMoveDownButton;
  private final AnActionButton myRemoveButton;
  private boolean myIsModified = false;

  private final CompoundScheme.MutatorHelper<ToolsGroup<T>, T> mutatorHelper = new CompoundScheme.MutatorHelper<>();

  protected BaseToolsPanel() {
    myTree = new CheckboxTree(
      getTreeCellRenderer(),
      new CheckedTreeNode(null)) {
      @Override
      protected void onDoubleClick(final CheckedTreeNode node) {
        editSelected();
      }

      @Override
      protected void onNodeStateChanged(final CheckedTreeNode node) {
        myIsModified = true;
      }
    };

    myTree.setRootVisible(false);
    myTree.getEmptyText().setText(ToolsBundle.message("tools.not.configured"));
    myTree.setSelectionModel(new DefaultTreeSelectionModel());
    myTree.getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

    setLayout(new BorderLayout());
    add(ToolbarDecorator.createDecorator(myTree).setAddAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        ToolEditorDialog dlg = createToolEditorDialog(ToolsBundle.message("tools.add.title"));
        Tool tool = new Tool();
        tool.setUseConsole(true);
        tool.setFilesSynchronizedAfterRun(true);
        tool.setShownInMainMenu(true);
        tool.setShownInEditor(true);
        tool.setShownInProjectViews(true);
        tool.setShownInSearchResultsPopup(true);
        tool.setEnabled(true);
        dlg.setData(tool, getGroups());
        if (dlg.showAndGet()) {
          insertNewTool(dlg.getData(), true);
        }
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myTree, true));
      }
    }).setRemoveAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        removeSelected();
      }
    }).setEditAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        editSelected();
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myTree, true));
      }
    }).setMoveUpAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        moveNode(Direction.UP);
        myIsModified = true;
      }
    }).setMoveDownAction(new AnActionButtonRunnable() {
      @Override
      public void run(AnActionButton button) {
        moveNode(Direction.DOWN);
        myIsModified = true;
      }
    }).addExtraAction(myCopyButton = new AnActionButton(ToolsBundle.message("tools.copy.button"), PlatformIcons.COPY_ICON) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        Tool originalTool = getSelectedTool();

        if (originalTool != null) {
          ToolEditorDialog dlg = createToolEditorDialog(ToolsBundle.message("tools.copy.title"));
          Tool toolCopy = new Tool();
          toolCopy.copyFrom(originalTool);
          dlg.setData(toolCopy, getGroups());
          if (dlg.showAndGet()) {
            insertNewTool(dlg.getData(), true);
          }
          IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myTree, true));
        }
      }
    }).createPanel(), BorderLayout.CENTER);

    myAddButton = ToolbarDecorator.findAddButton(this);
    myEditButton = ToolbarDecorator.findEditButton(this);
    myRemoveButton = ToolbarDecorator.findRemoveButton(this);
    myMoveUpButton = ToolbarDecorator.findUpButton(this);
    myMoveDownButton = ToolbarDecorator.findDownButton(this);

    //TODO check edit and delete

    myTree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
      @Override
      public void valueChanged(TreeSelectionEvent e) {
        update();
      }
    });
    setMinimumSize(myTree.getEmptyText().getPreferredSize());
  }

  public void reset() {
    mutatorHelper.clear();
    for (ToolsGroup<T> group : getToolManager().getGroups()) {
      insertNewGroup(mutatorHelper.copy(group));
    }

    if ((getTreeRoot()).getChildCount() > 0) {
      myTree.setSelectionInterval(0, 0);
    }
    else {
      myTree.getSelectionModel().clearSelection();
    }
    (getModel()).nodeStructureChanged(null);

    TreeUtil.expand(myTree, 5);

    myIsModified = false;

    update();
  }

  protected abstract BaseToolManager<T> getToolManager();

  protected CheckboxTree.CheckboxTreeCellRenderer getTreeCellRenderer() {
    return new MyCheckboxTreeCellRenderer();
  }

  protected static class MyCheckboxTreeCellRenderer extends CheckboxTree.CheckboxTreeCellRenderer {
    @Override
    public void customizeRenderer(final JTree tree,
                                  final Object value,
                                  final boolean selected,
                                  final boolean expanded,
                                  final boolean leaf,
                                  final int row,
                                  final boolean hasFocus) {
      if (!(value instanceof CheckedTreeNode)) return;
      Object object = ((CheckedTreeNode)value).getUserObject();

      if (object instanceof ToolsGroup) {
        final String groupName = ((ToolsGroup)object).getName();
        if (groupName != null) {
          getTextRenderer().append(groupName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
        else {
          getTextRenderer().append("[unnamed group]", SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
        }
      }
      else if (object instanceof Tool) {
        getTextRenderer().append(((Tool)object).getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
      }
    }
  }


  @NotNull
  private CheckedTreeNode insertNewGroup(@NotNull ToolsGroup<T> groupCopy) {
    CheckedTreeNode groupNode = new CheckedTreeNode(groupCopy);
    getTreeRoot().add(groupNode);
    for (T tool : groupCopy.getElements()) {
      insertNewTool(groupNode, tool);
    }
    return groupNode;
  }

  private CheckedTreeNode insertNewTool(@NotNull CheckedTreeNode groupNode, @NotNull Tool toolCopy) {
    CheckedTreeNode toolNode = new CheckedTreeNode(toolCopy);
    toolNode.setChecked(toolCopy.isEnabled());
    ((ToolsGroup)groupNode.getUserObject()).addElement(toolCopy);
    groupNode.add(toolNode);
    nodeWasInserted(toolNode);
    return toolNode;
  }

  private CheckedTreeNode getTreeRoot() {
    return (CheckedTreeNode)myTree.getModel().getRoot();
  }

  public void apply() {
    getToolManager().setTools(mutatorHelper.apply(getGroupList()));
    myIsModified = false;
  }

  @NotNull
  private List<ToolsGroup<T>> getGroupList() {
    MutableTreeNode root = (MutableTreeNode)myTree.getModel().getRoot();
    List<ToolsGroup<T>> result = new ArrayList<>(root.getChildCount());
    for (int i = 0; i < root.getChildCount(); i++) {
      final CheckedTreeNode node = (CheckedTreeNode)root.getChildAt(i);
      for (int j = 0; j < node.getChildCount(); j++) {
        final CheckedTreeNode toolNode = (CheckedTreeNode)node.getChildAt(j);
        ((Tool)toolNode.getUserObject()).setEnabled(toolNode.isChecked());
      }

      //noinspection unchecked
      result.add((ToolsGroup)node.getUserObject());
    }
    return result;
  }

  public boolean isModified() {
    return myIsModified;
  }

  private void moveNode(final Direction direction) {
    CheckedTreeNode node = getSelectedNode();
    if (node != null) {
      if (isMovingAvailable(node, direction)) {
        moveNode(node, direction);
        if (node.getUserObject() instanceof Tool) {
          ToolsGroup group = (ToolsGroup)(((CheckedTreeNode)node.getParent()).getUserObject());
          Tool tool = (Tool)node.getUserObject();
          moveElementInsideGroup(tool, group, direction);
        }
        TreePath path = new TreePath(node.getPath());
        myTree.getSelectionModel().setSelectionPath(path);
        myTree.expandPath(path);
        IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myTree, true));
      }
    }
  }

  private static void moveElementInsideGroup(final Tool tool, final ToolsGroup group, Direction dir) {
    if (dir == Direction.UP) {
      group.moveElementUp(tool);
    }
    else {
      group.moveElementDown(tool);
    }
  }

  private void moveNode(final CheckedTreeNode toolNode, Direction dir) {
    CheckedTreeNode parentNode = (CheckedTreeNode)toolNode.getParent();
    int index = parentNode.getIndex(toolNode);
    removeNodeFromParent(toolNode);
    int newIndex = dir.newIndex(index);
    parentNode.insert(toolNode, newIndex);
    getModel().nodesWereInserted(parentNode, new int[]{newIndex});
  }

  private static boolean isMovingAvailable(final CheckedTreeNode toolNode, Direction dir) {
    TreeNode parent = toolNode.getParent();
    int index = parent.getIndex(toolNode);
    return dir.isAvailable(index, parent.getChildCount());
  }

  private void insertNewTool(@NotNull Tool newTool, boolean setSelection) {
    CheckedTreeNode groupNode = findGroupNode(newTool.getGroup());
    if (groupNode == null) {
      groupNode = insertNewGroup(new ToolsGroup(newTool.getGroup()));
      nodeWasInserted(groupNode);
    }
    CheckedTreeNode tool = insertNewTool(groupNode, newTool);
    if (setSelection) {
      TreePath treePath = new TreePath(tool.getPath());
      myTree.expandPath(treePath);
      myTree.getSelectionModel().setSelectionPath(treePath);
    }
    myIsModified = true;
  }

  private void nodeWasInserted(final CheckedTreeNode groupNode) {
    (getModel()).nodesWereInserted(groupNode.getParent(), new int[]{groupNode.getParent().getChildCount() - 1});
  }

  private DefaultTreeModel getModel() {
    return (DefaultTreeModel)myTree.getModel();
  }

  private CheckedTreeNode findGroupNode(final String group) {
    for (int i = 0; i < getTreeRoot().getChildCount(); i++) {
      CheckedTreeNode node = (CheckedTreeNode)getTreeRoot().getChildAt(i);
      ToolsGroup g = (ToolsGroup)node.getUserObject();
      if (Comparing.equal(group, g.getName())) return node;
    }

    return null;
  }

  @Nullable
  public Tool getSelectedTool() {
    CheckedTreeNode node = getSelectedToolNode();
    if (node == null) return null;
    return node.getUserObject() instanceof Tool ? (Tool)node.getUserObject() : null;
  }

  @Nullable
  private ToolsGroup getSelectedToolGroup() {
    CheckedTreeNode node = getSelectedToolNode();
    if (node == null) return null;
    return node.getUserObject() instanceof ToolsGroup ? (ToolsGroup)node.getUserObject() : null;
  }

  private void update() {
    CheckedTreeNode node = getSelectedToolNode();
    Tool selectedTool = getSelectedTool();
    ToolsGroup selectedGroup = getSelectedToolGroup();

    if (selectedTool != null) {
      myAddButton.setEnabled(true);
      myCopyButton.setEnabled(true);
      myEditButton.setEnabled(true);
      myMoveDownButton.setEnabled(isMovingAvailable(node, Direction.DOWN));
      myMoveUpButton.setEnabled(isMovingAvailable(node, Direction.UP));
      myRemoveButton.setEnabled(true);
    }
    else if (selectedGroup != null) {
      myAddButton.setEnabled(true);
      myCopyButton.setEnabled(false);
      myEditButton.setEnabled(false);
      myMoveDownButton.setEnabled(isMovingAvailable(node, Direction.DOWN));
      myMoveUpButton.setEnabled(isMovingAvailable(node, Direction.UP));
      myRemoveButton.setEnabled(true);
    }
    else {
      myAddButton.setEnabled(true);
      myCopyButton.setEnabled(false);
      myEditButton.setEnabled(false);
      myMoveDownButton.setEnabled(false);
      myMoveUpButton.setEnabled(false);
      myRemoveButton.setEnabled(false);
    }

    (getModel()).nodeStructureChanged(null);

    myTree.repaint();
  }

  private void removeSelected() {
    CheckedTreeNode node = getSelectedToolNode();
    if (node != null) {
      int result = Messages.showYesNoDialog(
        this,
        ToolsBundle.message("tools.delete.confirmation"),
        "Delete Tool",
        Messages.getWarningIcon()
      );
      if (result != Messages.YES) {
        return;
      }
      myIsModified = true;
      if (node.getUserObject() instanceof Tool) {
        Tool tool = (Tool)node.getUserObject();
        CheckedTreeNode parentNode = (CheckedTreeNode)node.getParent();
        ((ToolsGroup)parentNode.getUserObject()).removeElement(tool);
        removeNodeFromParent(node);
        if (parentNode.getChildCount() == 0) {
          removeNodeFromParent(parentNode);
        }
      }
      else if (node.getUserObject() instanceof ToolsGroup) {
        removeNodeFromParent(node);
      }
      update();
      IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(myTree, true));
    }
  }

  private void removeNodeFromParent(DefaultMutableTreeNode node) {
    TreeNode parent = node.getParent();
    int idx = parent.getIndex(node);
    node.removeFromParent();

    (getModel()).nodesWereRemoved(parent, new int[]{idx}, new TreeNode[]{node});
  }


  private void editSelected() {
    CheckedTreeNode node = getSelectedToolNode();
    if (node != null && node.getUserObject() instanceof Tool) {
      Tool selected = (Tool)node.getUserObject();
      if (selected != null) {
        String oldGroupName = selected.getGroup();
        ToolEditorDialog dlg = createToolEditorDialog(ToolsBundle.message("tools.edit.title"));
        dlg.setData(selected, getGroups());
        if (dlg.showAndGet()) {
          selected.copyFrom(dlg.getData());
          String newGroupName = selected.getGroup();
          if (!Comparing.equal(oldGroupName, newGroupName)) {
            CheckedTreeNode oldGroupNode = (CheckedTreeNode)node.getParent();
            removeNodeFromParent(node);
            ((ToolsGroup)oldGroupNode.getUserObject()).removeElement(selected);
            if (oldGroupNode.getChildCount() == 0) {
              removeNodeFromParent(oldGroupNode);
            }

            insertNewTool(selected, true);
          }
          else {
            (getModel()).nodeChanged(node);
          }
          myIsModified = true;
          update();
        }
      }
    }
  }

  protected ToolEditorDialog createToolEditorDialog(String title) {
    return new ToolEditorDialog(this, title);
  }

  private CheckedTreeNode getSelectedToolNode() {
    TreePath selectionPath = myTree.getSelectionPath();
    if (selectionPath != null) {
      return (CheckedTreeNode)selectionPath.getLastPathComponent();
    }
    return null;
  }

  private CheckedTreeNode getSelectedNode() {
    TreePath selectionPath = myTree.getSelectionPath();
    if (selectionPath != null) {
      return (CheckedTreeNode)selectionPath.getLastPathComponent();
    }
    return null;
  }

  private String[] getGroups() {
    List<String> result = new ArrayList<>();
    for (ToolsGroup group : getGroupList()) {
      result.add(group.getName());
    }
    return ArrayUtilRt.toStringArray(result);
  }

  void addSelectionListener(TreeSelectionListener listener) {
    myTree.getSelectionModel().addTreeSelectionListener(listener);
  }

  @Nullable
  Tool getSingleSelectedTool() {
    final TreePath[] selectionPaths = myTree.getSelectionPaths();
    if (selectionPaths == null || selectionPaths.length != 1) {
      return null;
    }
    Object toolOrToolGroup = ((CheckedTreeNode)selectionPaths[0].getLastPathComponent()).getUserObject();
    if (toolOrToolGroup instanceof Tool) {
      return (Tool)toolOrToolGroup;
    }
    return null;
  }

  public void selectTool(final String actionId) {
    Object root = myTree.getModel().getRoot();
    if (!(root instanceof CheckedTreeNode)) {
      return;
    }
    final List<CheckedTreeNode> nodes = new ArrayList<>();
    new Object() {
      public void collect(CheckedTreeNode node) {
        if (node.isLeaf()) {
          Object userObject = node.getUserObject();
          if (userObject instanceof Tool && actionId.equals(((Tool)userObject).getActionId())) {
            nodes.add(node);
          }
        }
        else {
          for (int i = 0; i < node.getChildCount(); i++) {
            final TreeNode child = node.getChildAt(i);
            if (child instanceof CheckedTreeNode) {
              collect((CheckedTreeNode)child);
            }
          }
        }
      }
    }.collect((CheckedTreeNode)root);
    if (nodes.isEmpty()) {
      return;
    }
    myTree.getSelectionModel().setSelectionPath(new TreePath(nodes.get(0).getPath()));
  }
}
