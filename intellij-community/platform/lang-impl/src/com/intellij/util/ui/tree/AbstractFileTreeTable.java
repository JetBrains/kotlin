// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.util.ui.tree;

import com.intellij.icons.AllIcons;
import com.intellij.ide.CommonActionsManager;
import com.intellij.ide.DefaultTreeExpander;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.ui.SimpleColoredComponent;
import com.intellij.ui.TableUtil;
import com.intellij.ui.TreeTableSpeedSearch;
import com.intellij.ui.speedSearch.SpeedSearchUtil;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.ui.treeStructure.treetable.TreeTableCellRenderer;
import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import com.intellij.util.IconUtil;
import com.intellij.util.PlatformIcons;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.util.*;
import java.util.List;

public class AbstractFileTreeTable<T> extends TreeTable {
  private final MyModel<T> myModel;
  private final Project myProject;

  public AbstractFileTreeTable(@NotNull Project project,
                               @NotNull Class<T> valueClass,
                               @NotNull String valueTitle,
                               @NotNull VirtualFileFilter filter,
                               boolean showProjectNode) {
    this(project, valueClass, valueTitle, filter, showProjectNode, true);
  }

  /**
   * Due to historical reasons, passed filter does not perform all jobs - fileIndex.isInContent is checked in addition.
   * Flag showContentFilesOnly allows you to disable such behavior.
   */
  public AbstractFileTreeTable(@NotNull Project project,
                               @NotNull Class<T> valueClass,
                               @NotNull String valueTitle,
                               @NotNull VirtualFileFilter filter,
                               boolean showProjectNode,
                               boolean showContentFilesOnly) {
    super(new MyModel<>(project, valueClass, valueTitle, showContentFilesOnly ? new ProjectContentFileFilter(project, filter) : filter));
    myProject = project;

    //noinspection unchecked
    myModel = (MyModel)getTableModel();
    myModel.setTreeTable(this);

    new TreeTableSpeedSearch(this, o -> {
      final DefaultMutableTreeNode node = (DefaultMutableTreeNode)o.getLastPathComponent();
      final Object userObject = node.getUserObject();
      if (userObject == null) {
        return getProjectNodeText();
      }
      if (userObject instanceof VirtualFile) {
        return ((VirtualFile)userObject).getName();
      }
      return node.toString();
    });
    final DefaultTreeExpander treeExpander = new DefaultTreeExpander(getTree());
    CommonActionsManager.getInstance().createExpandAllAction(treeExpander, this);
    CommonActionsManager.getInstance().createCollapseAllAction(treeExpander, this);

    getTree().setShowsRootHandles(true);
    getTree().setRootVisible(showProjectNode);
    final ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    getTree().setCellRenderer(new DefaultTreeCellRenderer() {
      private final SimpleColoredComponent myComponent = new SimpleColoredComponent();
      @Override
      public Component getTreeCellRendererComponent(JTree tree,
                                                    Object value,
                                                    boolean sel,
                                                    boolean expanded,
                                                    boolean leaf,
                                                    int row,
                                                    boolean hasFocus) {
        myComponent.clear();
        if (value instanceof ProjectRootNode) {
          myComponent.append(getProjectNodeText());
          myComponent.setIcon(AllIcons.Nodes.Project);
        }
        else {
          FileNode fileNode = (FileNode)value;
          VirtualFile file = fileNode.getObject();
          myComponent.append(fileNode.getParent() instanceof FileNode ? file.getName() : file.getPresentableUrl());
          Icon icon = file.isDirectory()
                      ? fileIndex.isExcluded(file) ? AllIcons.Modules.ExcludeRoot
                                                   : PlatformIcons.FOLDER_ICON : IconUtil.getIcon(file, 0, null);
          myComponent.setIcon(icon);
        }
        SpeedSearchUtil.applySpeedSearchHighlighting(AbstractFileTreeTable.this, myComponent, false, selected);
        return myComponent;
      }
    });
    getTableHeader().setReorderingAllowed(false);

    setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    setPreferredScrollableViewportSize(new Dimension(300, getRowHeight() * 10));

    getColumnModel().getColumn(0).setPreferredWidth(280);
    getColumnModel().getColumn(1).setPreferredWidth(60);
  }

  protected boolean isNullObject(final T value) {
    return false;
  }

  private static String getProjectNodeText() {
    return "Project";
  }

  public Project getProject() {
    return myProject;
  }

  public TableColumn getValueColumn() {
    return getColumnModel().getColumn(1);
  }

  protected boolean isValueEditableForFile(final VirtualFile virtualFile) {
    return true;
  }

  public static void press(final Container comboComponent) {
    if (comboComponent instanceof JButton) {
      ((JButton)comboComponent).doClick();
    }
    else {
      for (int i = 0; i < comboComponent.getComponentCount(); i++) {
        Component child = comboComponent.getComponent(i);
        if (child instanceof Container) {
          press((Container)child);
        }
      }
    }
  }

  public boolean clearSubdirectoriesOnDemandOrCancel(final VirtualFile parent, final String message, final String title) {
    Map<VirtualFile, T> mappings = myModel.myCurrentMapping;
    Map<VirtualFile, T> subdirectoryMappings = new THashMap<>();
    for (VirtualFile file : mappings.keySet()) {
      if (file != null && (parent == null || VfsUtilCore.isAncestor(parent, file, true))) {
        subdirectoryMappings.put(file, mappings.get(file));
      }
    }
    if (subdirectoryMappings.isEmpty()) {
      return true;
    }
    int ret = Messages.showYesNoCancelDialog(myProject, message, title, "Override", "Do Not Override", "Cancel",
                                             Messages.getWarningIcon());
    if (ret == Messages.YES) {
      for (VirtualFile file : subdirectoryMappings.keySet()) {
        myModel.setValueAt(null, new DefaultMutableTreeNode(file), 1);
      }
    }
    return ret != Messages.CANCEL;
  }

  @NotNull
  public Map<VirtualFile, T> getValues() {
    return myModel.getValues();
  }

  @Override
  public TreeTableCellRenderer createTableRenderer(TreeTableModel treeTableModel) {
    TreeTableCellRenderer tableRenderer = super.createTableRenderer(treeTableModel);
    tableRenderer.setRootVisible(false);
    tableRenderer.setShowsRootHandles(true);
    return tableRenderer;
  }

  public void reset(@NotNull Map<VirtualFile, T> mappings) {
    myModel.reset(mappings);
    myModel.nodeChanged((TreeNode)myModel.getRoot());
    getTree().setModel(null);
    getTree().setModel(myModel);
    TreeUtil.expandRootChildIfOnlyOne(getTree());
  }

  public void select(@Nullable final VirtualFile toSelect) {
    if (toSelect != null) {
      select(toSelect, (TreeNode)myModel.getRoot());
    }
  }

  private void select(@NotNull VirtualFile toSelect, final TreeNode root) {
    for (int i = 0; i < root.getChildCount(); i++) {
      TreeNode child = root.getChildAt(i);
      VirtualFile file = ((FileNode)child).getObject();
      if (VfsUtilCore.isAncestor(file, toSelect, false)) {
        if (Comparing.equal(file, toSelect)) {
          TreeUtil.selectNode(getTree(), child);
          getSelectionModel().clearSelection();
          addSelectedPath(TreeUtil.getPathFromRoot(child));
          TableUtil.scrollSelectionToVisible(this);
        }
        else {
          select(toSelect, child);
        }
        return;
      }
    }
  }

  private static class MyModel<T> extends DefaultTreeModel implements TreeTableModel {
    private final Map<VirtualFile, T> myCurrentMapping = new HashMap<>();
    private final Class<T> myValueClass;
    private final String myValueTitle;
    private AbstractFileTreeTable<T> myTreeTable;

    private MyModel(@NotNull Project project, @NotNull Class<T> valueClass, @NotNull String valueTitle, @NotNull VirtualFileFilter filter) {
      super(new ProjectRootNode(project, filter));
      myValueClass = valueClass;
      myValueTitle = valueTitle;
    }

    private Map<VirtualFile, T> getValues() {
      return new HashMap<>(myCurrentMapping);
    }

    @Override
    public void setTree(JTree tree) {
    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(final int column) {
      switch (column) {
        case 0:
          return "File/Directory";
        case 1:
          return myValueTitle;
        default:
          throw new RuntimeException("invalid column " + column);
      }
    }

    @Override
    public Class getColumnClass(final int column) {
      switch (column) {
        case 0:
          return TreeTableModel.class;
        case 1:
          return myValueClass;
        default:
          throw new RuntimeException("invalid column " + column);
      }
    }

    @Override
    public Object getValueAt(final Object node, final int column) {
      Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
      if (userObject instanceof Project) {
        switch (column) {
          case 0:
            return userObject;
          case 1:
            return myCurrentMapping.get(null);
        }
      }
      VirtualFile file = (VirtualFile)userObject;
      switch (column) {
        case 0:
          return file;
        case 1:
          return myCurrentMapping.get(file);
        default:
          throw new RuntimeException("invalid column " + column);
      }
    }

    @Override
    public boolean isCellEditable(final Object node, final int column) {
      switch (column) {
        case 0:
          return false;
        case 1:
          final Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
          return !(userObject instanceof VirtualFile || userObject == null) || myTreeTable.isValueEditableForFile((VirtualFile)userObject);
        default:
          throw new RuntimeException("invalid column " + column);
      }
    }

    @Override
    public void setValueAt(final Object aValue, final Object node, final int column) {
      final Object userObject = ((DefaultMutableTreeNode)node).getUserObject();
      if (userObject instanceof Project) {
        return;
      }

      final VirtualFile file = (VirtualFile)userObject;
      @SuppressWarnings("unchecked")
      T t = (T)aValue;
      if (t == null || myTreeTable.isNullObject(t)) {
        myCurrentMapping.remove(file);
      }
      else {
        myCurrentMapping.put(file, t);
      }
      fireTreeNodesChanged(this, new Object[]{getRoot()}, null, null);
    }

    public void reset(@NotNull Map<VirtualFile, T> mappings) {
      myCurrentMapping.clear();
      myCurrentMapping.putAll(mappings);
      ((ProjectRootNode)getRoot()).clearCachedChildren();
    }

    void setTreeTable(final AbstractFileTreeTable<T> treeTable) {
      myTreeTable = treeTable;
    }
  }

  public static class ProjectRootNode extends ConvenientNode<Project> {
    private final VirtualFileFilter myFilter;

    public ProjectRootNode(@NotNull Project project) {
      this(project, VirtualFileFilter.ALL);
    }

    public ProjectRootNode(@NotNull Project project, @NotNull VirtualFileFilter filter) {
      super(project);
      myFilter = filter;
    }

    @Override
    protected void appendChildrenTo(@NotNull final Collection<? super ConvenientNode> children) {
      Project project = getObject();
      VirtualFile[] roots = ProjectRootManager.getInstance(project).getContentRoots();

      NextRoot:
      for (VirtualFile root : roots) {
        for (VirtualFile candidate : roots) {
          if (VfsUtilCore.isAncestor(candidate, root, true)) continue NextRoot;
        }
        if (myFilter.accept(root)) {
          children.add(new FileNode(root, project, myFilter));
        }
      }
    }
  }

  public abstract static class ConvenientNode<T> extends DefaultMutableTreeNode {
    private final T myObject;

    private ConvenientNode(T object) {
      myObject = object;
    }

    public T getObject() {
      return myObject;
    }

    protected abstract void appendChildrenTo(@NotNull Collection<? super ConvenientNode> children);

    @Override
    public int getChildCount() {
      init();
      return super.getChildCount();
    }

    @Override
    public TreeNode getChildAt(final int childIndex) {
      init();
      return super.getChildAt(childIndex);
    }

    @Override
    public Enumeration children() {
      init();
      return super.children();
    }

    private void init() {
      if (getUserObject() == null) {
        setUserObject(myObject);
        final List<ConvenientNode> children = new ArrayList<>();
        appendChildrenTo(children);
        Collections.sort(children, (node1, node2) -> {
          Object o1 = node1.getObject();
          Object o2 = node2.getObject();
          if (o1 == o2) return 0;
          if (o1 instanceof Project) return -1;
          if (o2 instanceof Project) return 1;
          VirtualFile file1 = (VirtualFile)o1;
          VirtualFile file2 = (VirtualFile)o2;
          if (file1.isDirectory() != file2.isDirectory()) {
            return file1.isDirectory() ? -1 : 1;
          }
          return file1.getName().compareTo(file2.getName());
        });
        int i = 0;
        for (ConvenientNode child : children) {
          insert(child, i++);
        }
      }
    }

    public void clearCachedChildren() {
      if (children != null) {
        for (Object child : children) {
          //noinspection unchecked
          ((ConvenientNode<T>)child).clearCachedChildren();
        }
      }
      removeAllChildren();
      setUserObject(null);
    }
  }

  public static class FileNode extends ConvenientNode<VirtualFile> {
    private final Project myProject;
    private final VirtualFileFilter myFilter;

    public FileNode(@NotNull VirtualFile file, @NotNull final Project project) {
      this(file, project, VirtualFileFilter.ALL);
    }

    public FileNode(@NotNull VirtualFile file, @NotNull final Project project, @NotNull VirtualFileFilter filter) {
      super(file);
      myProject = project;
      myFilter = filter;
    }

    @Override
    protected void appendChildrenTo(@NotNull final Collection<? super ConvenientNode> children) {
      for (VirtualFile child : getObject().getChildren()) {
        if (myFilter.accept(child)) {
          children.add(new FileNode(child, myProject, myFilter));
        }
      }
    }
  }
}
