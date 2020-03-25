// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.projectView.impl;

import com.intellij.ide.dnd.aware.DnDAwareTree;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.VfsPresentationUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDirectoryContainer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.DirtyUI;
import com.intellij.ui.popup.HintUpdateSupply;
import com.intellij.ui.tabs.FileColorManagerImpl;
import com.intellij.util.ObjectUtils;
import com.intellij.util.ui.tree.TreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.*;

/**
 * @author Konstantin Bulenkov
 */
public class ProjectViewTree extends DnDAwareTree {
  private static final Logger LOG = Logger.getInstance(ProjectViewTree.class);

  protected ProjectViewTree(Project project, TreeModel model) {
    this(model);
  }

  public ProjectViewTree(TreeModel model) {
    super((TreeModel)null);
    setLargeModel(true);
    setModel(model);
    setCellRenderer(createCellRenderer());
    HintUpdateSupply.installDataContextHintUpdateSupply(this);
  }

  /**
   * @return custom renderer for tree nodes
   */
  @NotNull
  protected TreeCellRenderer createCellRenderer() {
    return new ProjectViewRenderer();
  }

  /**
   * @deprecated Not every tree employs {@link DefaultMutableTreeNode} so
   * use {@link #getSelectionPaths()} or {@link TreeUtil#getSelectedPathIfOne(JTree)} directly.
   */
  @Deprecated
  public DefaultMutableTreeNode getSelectedNode() {
    TreePath path = TreeUtil.getSelectedPathIfOne(this);
    return path == null ? null : ObjectUtils.tryCast(path.getLastPathComponent(), DefaultMutableTreeNode.class);
  }

  @Override
  public final int getToggleClickCount() {
    int count = super.getToggleClickCount();
    TreePath path = getSelectionPath();
    if (path != null) {
      NodeDescriptor<?> descriptor = TreeUtil.getLastUserObject(NodeDescriptor.class, path);
      if (descriptor != null && !descriptor.expandOnDoubleClick()) {
        LOG.debug("getToggleClickCount: -1 for ", descriptor.getClass().getName());
        return -1;
      }
    }
    return count;
  }

  @Override
  public void setToggleClickCount(int count) {
    if (count != 2) LOG.info(new IllegalStateException("setToggleClickCount: unexpected count = " + count));
    super.setToggleClickCount(count);
  }

  @Override
  public boolean isFileColorsEnabled() {
    return isFileColorsEnabledFor(this);
  }

  public static boolean isFileColorsEnabledFor(JTree tree) {
    boolean enabled = FileColorManagerImpl._isEnabled() && FileColorManagerImpl._isEnabledForProjectView();
    boolean opaque = tree.isOpaque();
    if (enabled && opaque) {
      tree.setOpaque(false);
    }
    else if (!enabled && !opaque) {
      tree.setOpaque(true);
    }
    return enabled;
  }

  @DirtyUI
  @Nullable
  @Override
  public Color getFileColorFor(Object object) {
    if (object instanceof DefaultMutableTreeNode) {
      DefaultMutableTreeNode node = (DefaultMutableTreeNode)object;
      object = node.getUserObject();
    }
    if (object instanceof AbstractTreeNode) {
      AbstractTreeNode<?> node = (AbstractTreeNode<?>)object;
      Object value = node.getValue();
      if (value instanceof PsiElement) {
        return getColorForElement((PsiElement)value);
      }
    }
    if (object instanceof ProjectViewNode) {
      ProjectViewNode<?> node = (ProjectViewNode<?>)object;
      VirtualFile file = node.getVirtualFile();
      if (file != null) {
        Project project = node.getProject();
        if (project != null && !project.isDisposed()) {
          Color color = VfsPresentationUtil.getFileBackgroundColor(project, file);
          if (color != null) return color;
        }
      }
    }
    return null;
  }

  @Nullable
  public static Color getColorForElement(@Nullable PsiElement psi) {
    Color color = null;
    if (psi != null) {
      if (!psi.isValid()) return null;

      Project project = psi.getProject();
      final VirtualFile file = PsiUtilCore.getVirtualFile(psi);

      if (file != null) {
        color = VfsPresentationUtil.getFileBackgroundColor(project, file);
      }
      else if (psi instanceof PsiDirectory) {
        color = VfsPresentationUtil.getFileBackgroundColor(project, ((PsiDirectory)psi).getVirtualFile());
      }
      else if (psi instanceof PsiDirectoryContainer) {
        final PsiDirectory[] dirs = ((PsiDirectoryContainer)psi).getDirectories();
        for (PsiDirectory dir : dirs) {
          Color c = VfsPresentationUtil.getFileBackgroundColor(project, dir.getVirtualFile());
          if (c != null && color == null) {
            color = c;
          }
          else if (c != null) {
            color = null;
            break;
          }
        }
      }
    }
    return color;
  }
}
