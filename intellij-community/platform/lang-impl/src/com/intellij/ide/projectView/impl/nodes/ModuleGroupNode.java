// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.IdeBundle;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.actions.MoveModulesToGroupAction;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.impl.ModuleGroup;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleGrouper;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.util.PlatformIcons;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public abstract class ModuleGroupNode extends ProjectViewNode<ModuleGroup> implements DropTargetNode {
  public ModuleGroupNode(final Project project, @NotNull ModuleGroup value, final ViewSettings viewSettings) {
    super(project, value, viewSettings);
  }

  @NotNull
  protected abstract AbstractTreeNode createModuleNode(@NotNull Module module) throws
                                                                      InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException;
  @NotNull
  protected abstract ModuleGroupNode createModuleGroupNode(@NotNull ModuleGroup moduleGroup);

  @Override
  @NotNull
  public Collection<AbstractTreeNode<?>> getChildren() {
    final Collection<ModuleGroup> childGroups = getValue().childGroups(getProject());
    final List<AbstractTreeNode<?>> result = new ArrayList<>();
    for (final ModuleGroup childGroup : childGroups) {
      result.add(createModuleGroupNode(childGroup));
    }
    Collection<Module> modules = getValue().modulesInGroup(getProject());
    try {
      for (Module module : modules) {
        result.add(createModuleNode(module));
      }
    }
    catch (ReflectiveOperationException e) {
      LOG.error(e);
    }

    return result;
  }

  @NotNull
  @Override
  public Collection<VirtualFile> getRoots() {
    Collection<AbstractTreeNode<?>> children = getChildren();
    Set<VirtualFile> result = new HashSet<>();
    for (AbstractTreeNode each : children) {
      if (each instanceof ProjectViewNode) {
        result.addAll(((ProjectViewNode)each).getRoots());
      }
    }

    return result;
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    List<Module> modules = getModulesByFile(file);
    if (modules.isEmpty() && file.getFileSystem() instanceof ArchiveFileSystem) {
      VirtualFile archiveFile = ((ArchiveFileSystem)file.getFileSystem()).getLocalByEntry(file);
      if (archiveFile != null) modules = getModulesByFile(archiveFile);
    }
    List<String> thisGroupPath = getValue().getGroupPathList();
    ModuleGrouper grouper = ModuleGrouper.instanceFor(getProject());
    for (Module module : modules) {
      if (ContainerUtil.startsWith(grouper.getGroupPath(module), thisGroupPath)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public boolean validate() {
    return getValue() != null;
  }

  @NotNull
  protected abstract List<Module> getModulesByFile(@NotNull VirtualFile file);

  @Override
  public void update(@NotNull PresentationData presentation) {
    presentation.setPresentableText(getPresentableName());
    presentation.setIcon(PlatformIcons.CLOSED_MODULE_GROUP_ICON);
  }

  @NotNull
  private String getPresentableName() {
    return StringUtil.join(getRelativeGroupPath(), ".");
  }

  private List<String> getRelativeGroupPath() {
    AbstractTreeNode parent = getParent();
    List<String> thisPath = getValue().getGroupPathList();
    if (parent instanceof ModuleGroupNode) {
      List<String> parentPath = ((ModuleGroupNode)parent).getValue().getGroupPathList();
      if (ContainerUtil.startsWith(thisPath, parentPath)) {
        return thisPath.subList(parentPath.size(), thisPath.size());
      }
    }
    return thisPath;
  }

  @Override
  public String getTestPresentation() {
    return "Group: " + getPresentableName();
  }

  @Override
  public String getToolTip() {
    return IdeBundle.message("tooltip.module.group");
  }

  @Override
  public int getWeight() {
    return 0;
  }

  @Override
  public int getTypeSortWeight(final boolean sortByType) {
    return 1;
  }

  @Override
  public boolean canDrop(TreeNode @NotNull [] sourceNodes) {
    final List<Module> modules = extractModules(sourceNodes);
    return !modules.isEmpty();
  }

  @Override
  public void drop(TreeNode @NotNull [] sourceNodes, @NotNull DataContext dataContext) {
    final List<Module> modules = extractModules(sourceNodes);
    MoveModulesToGroupAction.doMove(modules.toArray(Module.EMPTY_ARRAY), getValue(), null);
  }

  @Override
  public void dropExternalFiles(PsiFileSystemItem[] sourceFileArray, DataContext dataContext) {
    // Do nothing, N/A
  }

  private static List<Module> extractModules(TreeNode[] sourceNodes) {
    final List<Module> modules = new ArrayList<>();
    for (TreeNode sourceNode : sourceNodes) {
      if (sourceNode instanceof DefaultMutableTreeNode) {
        final Object userObject = AbstractProjectViewPane.extractValueFromNode(sourceNode);
        if (userObject instanceof Module) {
          modules.add((Module) userObject);
        }
      }
    }
    return modules;
  }
}
