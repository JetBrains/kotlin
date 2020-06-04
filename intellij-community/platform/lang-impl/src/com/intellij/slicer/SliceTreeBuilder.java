// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.slicer;

import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AlphaComparator;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.util.ProgressIndicatorBase;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.Comparator;

public class SliceTreeBuilder extends AbstractTreeBuilder {
  public final boolean splitByLeafExpressions;
  public final boolean dataFlowToThis;
  public volatile boolean analysisInProgress;

  public static final Comparator<NodeDescriptor<?>> SLICE_NODE_COMPARATOR = (o1, o2) -> {
    if (!(o1 instanceof SliceNode) || !(o2 instanceof SliceNode)) {
      return AlphaComparator.INSTANCE.compare(o1, o2);
    }
    SliceNode node1 = (SliceNode)o1;
    SliceNode node2 = (SliceNode)o2;
    SliceUsage usage1 = node1.getValue();
    SliceUsage usage2 = node2.getValue();

    PsiElement element1 = usage1.getElement();
    PsiElement element2 = usage2.getElement();

    PsiFile file1 = element1 == null ? null : element1.getContainingFile();
    PsiFile file2 = element2 == null ? null : element2.getContainingFile();

    if (file1 == null) return file2 == null ? 0 : 1;
    if (file2 == null) return -1;

    if (file1 == file2) {
      return element1.getTextOffset() - element2.getTextOffset();
    }

    return Comparing.compare(file1.getName(), file2.getName());
  };

  SliceTreeBuilder(@NotNull JTree tree,
                   @NotNull Project project,
                   boolean dataFlowToThis,
                   @NotNull SliceNode rootNode,
                   boolean splitByLeafExpressions) {
    super(tree, (DefaultTreeModel)tree.getModel(), new SliceTreeStructure(project, rootNode), SLICE_NODE_COMPARATOR, false);
    this.dataFlowToThis = dataFlowToThis;
    this.splitByLeafExpressions = splitByLeafExpressions;
    initRootNode();
  }

  SliceNode getRootSliceNode() {
    return (SliceNode)getTreeStructure().getRootElement();
  }

  @Override
  protected boolean isAutoExpandNode(NodeDescriptor nodeDescriptor) {
    return false;
  }

  void switchToGroupedByLeavesNodes() {
    SliceLanguageSupportProvider provider = getRootSliceNode().getProvider();
    if(provider == null){
      return;
    }
    analysisInProgress = true;
    provider.startAnalyzeLeafValues(getTreeStructure(), () -> analysisInProgress = false);
  }


  public void switchToLeafNulls() {
    SliceLanguageSupportProvider provider = getRootSliceNode().getProvider();
    if(provider == null){
      return;
    }
    analysisInProgress = true;
    provider.startAnalyzeLeafValues(getTreeStructure(), () -> analysisInProgress = false);

    analysisInProgress = true;
    provider.startAnalyzeNullness(getTreeStructure(), () -> analysisInProgress = false);
  }

  @Nullable
  @Override
  protected ProgressIndicator createProgressIndicator() {
    return new ProgressIndicatorBase(true);
  }
}
