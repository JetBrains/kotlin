/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonShortcuts;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.keymap.KeymapUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ComponentPopupBuilder;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.Consumer;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TaskDefaultFavoriteListProvider extends AbstractFavoritesListProvider {
  public static final String CURRENT_TASK = "Current task";

  public TaskDefaultFavoriteListProvider(Project project) {
    super(project, CURRENT_TASK);
  }

  //@Override
  //public String getListName(Project project) {
  //  return CURRENT_TASK;
  //}

  //@Override
  //public boolean canBeRemoved() {
  //  return false;
  //}
  //
  //@Override
  //public boolean isTreeLike() {
  //  return false;
  //}

  @Override
  public FavoritesListNode createFavoriteListNode(Project project) {
    return null;
  }

  @Override
  public int getWeight() {
    return TASKS_WEIGHT;
  }

  //@Override
  //public Comparator<FavoritesTreeNodeDescriptor> getNodeDescriptorComparator() {
  //  return new Comparator<FavoritesTreeNodeDescriptor>() {
  //    @Override
  //    public int compare(FavoritesTreeNodeDescriptor o1, FavoritesTreeNodeDescriptor o2) {
  //      return o1.getIndex() - o2.getIndex();
  //    }
  //  };
  //}

  //@Override
  //public Operation createCustomOperation(OperationType operationType) {
  //  switch (operationType) {
  //    case ADD:return getCustomAddOperation();
  //    case EDIT: return getCustomEditOperation();
  //    default:return null;
  //  }
  //}
  //
  // private Operation getCustomAddOperation() {
  //  return new Operation() {
  //    @Override
  //    public boolean willHandle(final Project project, @NotNull Set<Object> selectedObjects) {//todo
  //      //final int count = tree.getSelectionCount();
  //      //if (count != 1) {
  //      //  return false;
  //      //}
  //      //final TreePath path = tree.getSelectionPath();
  //      //if (path.getPathCount() > 2) return true;
  //      return false;
  //    }
  //
  //    @Override
  //    public String getCustomName() {
  //      return "New Note";
  //    }
  //
  //    @Override
  //    public void handle(final Project project, @NotNull Set<Object> selectedObjects) {//todo
  //final Object component = tree.getSelectionPath().getLastPathComponent();
  //if (component instanceof DefaultMutableTreeNode) {
  //  final Object uo = ((DefaultMutableTreeNode)component).getUserObject();
  //  if (uo instanceof FavoritesTreeNodeDescriptor) {
  //    final FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
  //
  //    final AbstractTreeNode treeNode = ((FavoritesTreeNodeDescriptor)uo).getElement();
  //    final NoteNode node = new NoteNode("Test text", false);
  //    final NoteProjectNode noteNode = new NoteProjectNode(project, node, favoritesManager.getViewSettings());
  //    final Consumer<String> after = new Consumer<String>() {
  //      @Override
  //      public void consume(String text) {
  //        node.setText(text);
  //        // above it
  //        final AbstractTreeNode parent = treeNode.getParent();
  //        noteNode.setParent(parent);
  //        if (parent instanceof ProjectViewNodeWithChildrenList) {
  //          // add through manager
  //          //((ProjectViewNodeWithChildrenList)parent).addChildBefore(noteNode, treeNode);
  //          final List<AbstractTreeNode> pathToSelected = FavoritesTreeUtil.getLogicalPathToSelected(tree);
  //          final List<AbstractTreeNode> elements;
  //          AbstractTreeNode sibling;
  //          if (pathToSelected.isEmpty()) {
  //            elements = pathToSelected;
  //            sibling = null;
  //          }
  //          else {
  //            elements = pathToSelected.subList(0, pathToSelected.size() - 1);
  //            sibling = pathToSelected.get(pathToSelected.size() - 1);
  //          }
  //          favoritesManager.addRoot(CURRENT_TASK, elements, noteNode, sibling);
  //        } else if (parent instanceof FavoritesListNode) {
  //          favoritesManager.addRoot(CURRENT_TASK, Collections.<AbstractTreeNode>emptyList(), noteNode, treeNode);
  //        }
  //      }
  //    };
  //    showNotePopup(project, tree, after, "");
  //  }
  //}
  //}
  //};
  //}

  // ! containing self
  public static List<AbstractTreeNode> getPathToUsualNode(final AbstractTreeNode treeNode) {
    final List<AbstractTreeNode> result = new ArrayList<>();
    AbstractTreeNode current = treeNode;
    while (current != null && (!(current instanceof FavoritesRootNode))) {
      result.add(current);
      current = current.getParent();
    }
    Collections.reverse(result);
    return result;
  }

  public static List<AbstractTreeNode> getPathToUsualNode(final AbstractTreeNode treeNode, final Tree tree) {
    final AbstractTreeNode parent = treeNode.getParent();
    if (parent instanceof ProjectViewNodeWithChildrenList) {
      final List<AbstractTreeNode> pathToSelected = FavoritesTreeUtil.getLogicalPathToSelected(tree);
      if (pathToSelected.isEmpty()) {
        return pathToSelected;
      }
      else {
        return pathToSelected.subList(0, pathToSelected.size() - 1);
      }
    }
    return Collections.emptyList();
  }

  private void showNotePopup(Project project,
                             final DnDAwareTree tree,
                             final Consumer<? super String> after, final String initText) {
    final JTextArea textArea = new JTextArea(3, 50);
    textArea.setFont(UIUtil.getTreeFont());
    textArea.setText(initText);
    final JBScrollPane pane = new JBScrollPane(textArea);
    final ComponentPopupBuilder builder = JBPopupFactory.getInstance().createComponentPopupBuilder(pane, textArea)
      .setCancelOnClickOutside(true)
      .setAdText(KeymapUtil.getShortcutsText(CommonShortcuts.CTRL_ENTER.getShortcuts()) + " to finish")
      .setTitle("Comment")
      .setMovable(true)
      .setRequestFocus(true).setResizable(true).setMayBeParent(true);
    final JBPopup popup = builder.createPopup();
    final JComponent content = popup.getContent();
    final AnAction action = new AnAction() {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        popup.closeOk(e.getInputEvent());
        unregisterCustomShortcutSet(content);
        after.consume(textArea.getText());
      }
    };
    action.registerCustomShortcutSet(CommonShortcuts.CTRL_ENTER, content);
    ApplicationManager.getApplication().invokeLater(() -> popup.showInCenterOf(tree), ModalityState.NON_MODAL, project.getDisposed());
  }

  //private Operation getCustomEditOperation() {
  //  return new Operation() {
  //    @Override
  //    public boolean willHandle(final Project project, @NotNull Set<Object> selectedObjects) {//todo
  //final int count = tree.getSelectionCount();
  //if (count != 1) {
  //  return false;
  //}
  //final TreePath path = tree.getSelectionPath();
  //if (path.getPathCount() < 2) return false;
  //// todo temporarily
  //if (path.getLastPathComponent() instanceof DefaultMutableTreeNode) {
  //  final Object uo = ((DefaultMutableTreeNode)path.getLastPathComponent()).getUserObject();
  //  if (uo instanceof FavoritesTreeNodeDescriptor) {
  //    return ((FavoritesTreeNodeDescriptor)uo).getElement() instanceof NoteProjectNode;
  //  }
  //}
  //return false;
  //}

  //@Override
  //public String getCustomName() {
  //  return "Edit Note";
  //}

  //@Override
  //public void handle(Project project, @NotNull Set<Object> selectedObjects) {//todo
  //final Object component = tree.getSelectionPath().getLastPathComponent();
  //if (component instanceof DefaultMutableTreeNode) {
  //  final Object uo = ((DefaultMutableTreeNode)component).getUserObject();
  //  if (uo instanceof FavoritesTreeNodeDescriptor) {
  //    final FavoritesManager favoritesManager = FavoritesManager.getInstance(project);
  //
  //    final AbstractTreeNode treeNode = ((FavoritesTreeNodeDescriptor)uo).getElement();
  //
  //    if (treeNode instanceof NoteProjectNode) {
  //      showNotePopup(project, tree, new Consumer<String>() {
  //        @Override
  //        public void consume(String s) {
  //          ((NoteProjectNode)treeNode).getValue().setText(s);
  //          favoritesManager.editRoot(CURRENT_TASK, FavoritesTreeUtil.getLogicalIndexPathTo(tree.getSelectionPath()), treeNode);
  //          favoritesManager.fireListeners(CURRENT_TASK);
  //        }
  //      }, ((NoteProjectNode)treeNode).getValue().getText());
  //    }
  //  }
  //}
  //}
  //};
  //}

  //@Override
  //public TreeCellRenderer getTreeCellRenderer() {
  //  return new MyRenderer();
  //}
  //
  //private static class MyRenderer implements TreeCellRenderer {
  //  private AbstractTreeNode myNode;
  //
  //  private final MultilineTreeCellRenderer myMultilineTreeCellRenderer = new MultilineTreeCellRenderer() {
  //    @Override
  //    protected void initComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
  //      if (myNode instanceof NoteProjectNode) {
  //        setForeground(UIUtil.getListSelectionBackground());
  //        final NoteNode note = ((NoteProjectNode)myNode).getValue();
  //        final String[] lines = StringUtil.splitByLines(note.getText());
  //        setText(lines, null);
  //      }
  //    }
  //  };
  //
  //  private final MultiLineLabel myLabel = new MultiLineLabel();
  //
  //  @Override
  //  public Component getTreeCellRendererComponent(JTree tree,
  //                                                Object value,
  //                                                boolean selected,
  //                                                boolean expanded,
  //                                                boolean leaf,
  //                                                int row,
  //                                                boolean hasFocus) {
  //    if (value instanceof DefaultMutableTreeNode) {
  //      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)value;
  //      //only favorites roots to explain
  //      final Object userObject = node.getUserObject();
  //      if (userObject instanceof FavoritesTreeNodeDescriptor) {
  //        final FavoritesTreeNodeDescriptor favoritesTreeNodeDescriptor = (FavoritesTreeNodeDescriptor)userObject;
  //        AbstractTreeNode treeNode = favoritesTreeNodeDescriptor.getElement();
  //        if (treeNode instanceof NoteProjectNode) {
  //          myNode = treeNode;
  //          myLabel.setText(((NoteProjectNode)myNode).getValue().getText());
  //          //myLabel.setBackground(selected ? );
  //          myMultilineTreeCellRenderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
  //          return myMultilineTreeCellRenderer;
  //          //return myLabel;
  //        }
  //      }
  //    }
  //    return null;
  //  }
  //}
}
