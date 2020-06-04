// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Comparator;

import static com.intellij.openapi.util.text.StringUtil.naturalCompare;

public class GroupByTypeComparator implements Comparator<NodeDescriptor<?>> {
  private ProjectView myProjectView;
  private String myPaneId;
  private boolean myForceSortByType;

  public GroupByTypeComparator(@Nullable ProjectView projectView, final String paneId) {
    myProjectView = projectView;
    myPaneId = paneId;
  }

  public GroupByTypeComparator(final boolean forceSortByType) {
    myForceSortByType = forceSortByType;
  }

  @Override
  public int compare(NodeDescriptor descriptor1, NodeDescriptor descriptor2) {
    if (!isSortByType() && descriptor1 instanceof ProjectViewNode && ((ProjectViewNode) descriptor1).isSortByFirstChild()) {
      Collection<? extends AbstractTreeNode<?>> children = ((ProjectViewNode<?>)descriptor1).getChildren();
      if (!children.isEmpty()) {
        descriptor1 = children.iterator().next();
        descriptor1.update();
      }
    }
    if (!isSortByType() && descriptor2 instanceof ProjectViewNode && ((ProjectViewNode) descriptor2).isSortByFirstChild()) {
      Collection<? extends AbstractTreeNode<?>> children = ((ProjectViewNode<?>)descriptor2).getChildren();
      if (!children.isEmpty()) {
        descriptor2 = children.iterator().next();
        descriptor2.update();
      }
    }

    if (descriptor1 instanceof ProjectViewNode && descriptor2 instanceof ProjectViewNode) {
      ProjectViewNode<?> node1 = (ProjectViewNode<?>)descriptor1;
      ProjectViewNode<?> node2 = (ProjectViewNode<?>)descriptor2;

      if (isManualOrder()) {
        Comparable key1 = node1.getManualOrderKey();
        Comparable key2 = node2.getManualOrderKey();
        int result = compare(key1, key2);
        if (result != 0) return result;
      }

      if (isFoldersAlwaysOnTop()) {
        int typeWeight1 = node1.getTypeSortWeight(isSortByType());
        int typeWeight2 = node2.getTypeSortWeight(isSortByType());
        if (typeWeight1 != 0 && typeWeight2 == 0) {
          return -1;
        }
        if (typeWeight1 == 0 && typeWeight2 != 0) {
          return 1;
        }
        if (typeWeight1 != 0 && typeWeight2 != typeWeight1) {
          return typeWeight1 - typeWeight2;
        }
      }

      if (isSortByType()) {
        final Comparable typeSortKey1 = node1.getTypeSortKey();
        final Comparable typeSortKey2 = node2.getTypeSortKey();
        int result = compare(typeSortKey1, typeSortKey2);
        if (result != 0) return result;
      }
      else {
        final Comparable typeSortKey1 = node1.getSortKey();
        final Comparable typeSortKey2 = node2.getSortKey();
        if (typeSortKey1 != null && typeSortKey2 != null) {
          int result = compare(typeSortKey1, typeSortKey2);
          if (result != 0) return result;
        }
      }

      if (isAbbreviateQualifiedNames()) {
        String key1 = node1.getQualifiedNameSortKey();
        String key2 = node2.getQualifiedNameSortKey();
        if (key1 != null && key2 != null) {
          return naturalCompare(key1, key2);
        }
      }
    }
    if (descriptor1 == null) return -1;
    if (descriptor2 == null) return 1;
    return AlphaComparator.INSTANCE.compare(descriptor1, descriptor2);
  }

  protected boolean isManualOrder() {
    if (myProjectView != null) {
      return myProjectView.isManualOrder(myPaneId);
    }
    return true;
  }

  protected boolean isSortByType() {
    if (myProjectView != null) {
      return myProjectView.isSortByType(myPaneId);
    }
    return myForceSortByType;
  }

  protected boolean isAbbreviateQualifiedNames() {
    return myProjectView != null && myProjectView.isAbbreviatePackageNames(myPaneId);
  }

  protected boolean isFoldersAlwaysOnTop() {
    return myProjectView == null || myProjectView.isFoldersAlwaysOnTop(myPaneId);
  }

  private static int compare(Comparable key1, Comparable key2) {
    if (key1 == null && key2 == null) return 0;
    if (key1 == null) return 1;
    if (key2 == null) return -1;
    if (key1 instanceof String && key2 instanceof String) {
      return naturalCompare((String)key1, (String)key2);
    }
    try {
      //noinspection unchecked
      return key1.compareTo(key2);
    }
    catch (ClassCastException ignored) {
      // if custom nodes provide comparable keys of different types,
      // let's try to compare class names instead to avoid broken trees
      return key1.getClass().getName().compareTo(key2.getClass().getName());
    }
  }
}
