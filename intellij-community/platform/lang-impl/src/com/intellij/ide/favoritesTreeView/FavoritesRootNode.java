// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public final class FavoritesRootNode extends AbstractTreeNode<String> {
  private List<AbstractTreeNode<?>> myFavoritesRoots;

  public FavoritesRootNode(Project project) {
    super(project, "");
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode<?>> getChildren() {
    if (myFavoritesRoots == null) {
      myFavoritesRoots = new ArrayList<>(FavoritesManager.getInstance(myProject).createRootNodes());
    }
    return myFavoritesRoots;
  }

  public void rootsChanged() {
    myFavoritesRoots = null;
  }

  @Override
  public void update(@NotNull final PresentationData presentation) {
  }
}
