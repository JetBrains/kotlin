// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.tree.TreePath;
import java.util.List;


/**
 * Implement this extension to customise selection process in the project view.
 */
public abstract class ProjectViewPaneSelectionHelper {
  private static final ExtensionPointName<ProjectViewPaneSelectionHelper> EP_NAME =
    ExtensionPointName.create("com.intellij.projectViewPaneSelectionHelper");

  /**
   * @param selectionDescriptor information about target elements and potential {@link TreePath tree paths} for selection found by {@link AbstractProjectViewPane#createVisitor(PsiElement, VirtualFile, List)}  node visitor}
   * @return collection of tree paths to select in tree or null if this helper can't handle {@link SelectionDescriptor}
   * @see AbstractTreeNode#canRepresent
   */
  @Nullable
  protected abstract List<? extends TreePath> computeAdjustedPaths(@NotNull SelectionDescriptor selectionDescriptor);

  /**
   * @param selectionDescriptor information about target elements and potential {@link TreePath tree paths} for selection found by {@link AbstractProjectViewPane#createVisitor(PsiElement, VirtualFile, List)}  node visitor}
   * @return list of {@link TreePath tree paths} to select, computed from {@code selectionDescriptor} with first suitable selection helper
   * Returns {@link SelectionDescriptor#originalTreePaths original paths} by default
   */
  @NotNull
  static List<? extends TreePath> getAdjustedPaths(@NotNull SelectionDescriptor selectionDescriptor) {
    for (ProjectViewPaneSelectionHelper helper : EP_NAME.getExtensionList()) {
      List<? extends TreePath> adjustedPaths = helper.computeAdjustedPaths(selectionDescriptor);
      if (adjustedPaths != null) {
        return adjustedPaths;
      }
    }
    return selectionDescriptor.originalTreePaths;
  }

  public static class SelectionDescriptor {
    @Nullable
    public final PsiElement targetPsiElement;
    @Nullable
    public final VirtualFile targetVirtualFile;
    @NotNull
    public final List<TreePath> originalTreePaths;

    public SelectionDescriptor(@Nullable PsiElement targetPsiElement,
                               @Nullable VirtualFile targetVirtualFile,
                               @NotNull List<TreePath> originalTreePaths) {
      this.targetPsiElement = targetPsiElement;
      this.targetVirtualFile = targetVirtualFile;
      this.originalTreePaths = ContainerUtil.immutableList(originalTreePaths);
    }
  }
}
