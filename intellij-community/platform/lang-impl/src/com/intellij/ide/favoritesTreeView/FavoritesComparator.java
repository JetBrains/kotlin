
package com.intellij.ide.favoritesTreeView;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.GroupByTypeComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;

/**
 * @author Konstantin Bulenkov
 */
public class FavoritesComparator extends GroupByTypeComparator {
  public FavoritesComparator(ProjectView view, String id) {
    super(view, id);
  }

  @Override
  public int compare(NodeDescriptor d1, NodeDescriptor d2) {
    if (d1 instanceof FavoritesTreeNodeDescriptor && d2 instanceof FavoritesTreeNodeDescriptor) {
      d1 = ((FavoritesTreeNodeDescriptor)d1).getElement();
      d2 = ((FavoritesTreeNodeDescriptor)d2).getElement();
    }
    return super.compare(d1, d2);
  }
}