/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
public class FavoritesRootNode extends AbstractTreeNode<String> {
  private List<AbstractTreeNode> myFavoritesRoots;

  public FavoritesRootNode(Project project) {
    super(project, "");
  }

  @Override
  @NotNull
  public Collection<AbstractTreeNode> getChildren() {
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
