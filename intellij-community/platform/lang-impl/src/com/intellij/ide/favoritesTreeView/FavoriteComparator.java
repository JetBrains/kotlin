
// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.projectView.impl.GroupByTypeComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;

/**
 * @author Konstantin Bulenkov
 */
class FavoriteComparator extends GroupByTypeComparator {
  FavoriteComparator() {
    super(null, FavoritesViewTreeBuilder.ID);
  }

  @Override
  public int compare(NodeDescriptor d1, NodeDescriptor d2) {
    if (d1 instanceof FavoriteTreeNodeDescriptor && d2 instanceof FavoriteTreeNodeDescriptor) {
      d1 = ((FavoriteTreeNodeDescriptor)d1).getElement();
      d2 = ((FavoriteTreeNodeDescriptor)d2).getElement();
    }
    return super.compare(d1, d2);
  }
}