// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInspection.ui;

import com.intellij.codeHighlighting.HighlightDisplayLevel;
import com.intellij.codeInspection.reference.RefEntity;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.util.AtomicClearableLazyValue;
import com.intellij.openapi.util.RecursionGuard;
import com.intellij.util.containers.BidirectionalMap;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.WeakInterner;
import gnu.trove.TObjectHashingStrategy;
import gnu.trove.TObjectIntHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeNode;
import java.util.*;

/**
 * @author max
 */
public abstract class InspectionTreeNode implements TreeNode {
  private static final WeakInterner<LevelAndCount[]> LEVEL_AND_COUNT_INTERNER = new WeakInterner<>(new TObjectHashingStrategy<LevelAndCount[]>() {
    @Override
    public int computeHashCode(LevelAndCount[] object) {
      return Arrays.hashCode(object);
    }

    @Override
    public boolean equals(LevelAndCount[] o1, LevelAndCount[] o2) {
      return Arrays.equals(o1, o2);
    }
  });

  protected final ProblemLevels myProblemLevels = new ProblemLevels();
  @NotNull
  final Children myChildren = new Children();
  final InspectionTreeNode myParent;

  protected InspectionTreeNode(InspectionTreeNode parent) {
    myParent = parent;
  }

  protected boolean doesNeedInternProblemLevels() {
    return false;
  }

  @Nullable
  public Icon getIcon(boolean expanded) {
    return null;
  }

  @NotNull
  LevelAndCount[] getProblemLevels() {
    if (!isProblemCountCacheValid()) {
      dropProblemCountCaches();
    }
    return myProblemLevels.getValue();
  }

  void dropProblemCountCaches() {
    InspectionTreeNode current = this;
    while (current != null && getParent() != null) {
      current.myProblemLevels.drop();
      current = current.getParent();
    }
  }

  protected boolean isProblemCountCacheValid() {
    return true;
  }

  protected void visitProblemSeverities(@NotNull TObjectIntHashMap<HighlightDisplayLevel> counter) {
    for (InspectionTreeNode child : getChildren()) {
      for (LevelAndCount levelAndCount : child.getProblemLevels()) {
        if (!counter.adjustValue(levelAndCount.getLevel(), levelAndCount.getCount())) {
          counter.put(levelAndCount.getLevel(), levelAndCount.getCount());
        }
      }
    }
  }

  public boolean isValid() {
    return true;
  }

  public boolean isExcluded() {
    List<? extends InspectionTreeNode> children = getChildren();
    for (InspectionTreeNode child : children) {
      if (!child.isExcluded()) {
        return false;
      }
    }

    return !children.isEmpty() ;
  }

  public boolean appearsBold() {
    return false;
  }

  @Nullable
  public String getTailText() {
    return null;
  }

  public void excludeElement() {
    for (InspectionTreeNode child : getChildren()) {
      child.excludeElement();
    }
    dropProblemCountCaches();
  }

  public void amnestyElement() {
    for (InspectionTreeNode child : getChildren()) {
      child.amnestyElement();
    }
    dropProblemCountCaches();
  }

  public RefEntity getContainingFileLocalEntity() {
    RefEntity current = null;
    for (InspectionTreeNode child : getChildren()) {
      final RefEntity entity = child.getContainingFileLocalEntity();
      if (entity == null || current != null) {
        return null;
      }
      current = entity;
    }
    return current;
  }

  @Override
  public boolean isLeaf() {
    return getChildren().isEmpty();
  }

  public abstract String getPresentableText();

  @NotNull
  public List<? extends InspectionTreeNode> getChildren() {
    return ContainerUtil.immutableList(myChildren.myChildren);
  }

  @Override
  public InspectionTreeNode getParent() {
    return myParent;
  }

  @Override
  public int getChildCount() {
    return getChildren().size();
  }

  @Override
  public InspectionTreeNode getChildAt(int idx) {
    return getChildren().get(idx);
  }

  @Override
  public int getIndex(TreeNode node) {
    return Collections.binarySearch(getChildren(), (InspectionTreeNode)node, InspectionResultsViewComparator.INSTANCE);
  }

  @Override
  public boolean getAllowsChildren() {
    return true;
  }

  @Override
  public Enumeration children() {
    return Collections.enumeration(getChildren());
  }

  @Override
  public String toString() {
    return getPresentableText();
  }

  void uiRequested() {

  }

  static class Children {
    private static final InspectionTreeNode[] EMPTY_ARRAY = new InspectionTreeNode[0];

    volatile InspectionTreeNode[] myChildren = EMPTY_ARRAY;
    final BidirectionalMap<Object, InspectionTreeNode> myUserObject2Node = new BidirectionalMap<>();

    void clear() {
      myChildren = EMPTY_ARRAY;
      myUserObject2Node.clear();
    }
  }

  class ProblemLevels {
    private volatile LevelAndCount[] myLevels;

    @NotNull
    private LevelAndCount[] compute() {
      TObjectIntHashMap<HighlightDisplayLevel> counter = new TObjectIntHashMap<>();
      visitProblemSeverities(counter);
      LevelAndCount[] arr = new LevelAndCount[counter.size()];
      final int[] i = {0};
      counter.forEachEntry((l, c) -> {
        arr[i[0]++] = new LevelAndCount(l, c);
        return true;
      });
      Arrays.sort(arr, Comparator.<LevelAndCount, HighlightSeverity>comparing(levelAndCount -> levelAndCount.getLevel().getSeverity())
        .reversed());
      return doesNeedInternProblemLevels() ? LEVEL_AND_COUNT_INTERNER.intern(arr) : arr;
    }

    @NotNull
    public LevelAndCount[] getValue() {
      LevelAndCount[] result = myLevels;
      if (result == null) {
        //noinspection SynchronizeOnThis
        synchronized (this) {
          result = myLevels;
          if (result == null) {
            myLevels = result = compute();
          }
        }
      }
      return result;
    }

    public void drop() {
      myLevels = null;
    }
  }
}
