/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.projectView.impl.nodes;

import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;

public class NestingTreeNode extends PsiFileNode {
  @NotNull private final Collection<? extends PsiFileNode> myChildNodes;

  public NestingTreeNode(@NotNull final PsiFileNode originalNode, @NotNull final Collection<? extends PsiFileNode> childNodes) {
    super(originalNode.getProject(), originalNode.getValue(), originalNode.getSettings());
    myChildNodes = childNodes;
  }

  @Override
  public boolean isAlwaysShowPlus() {
    return true;
  }

  @Override
  public boolean expandOnDoubleClick() {
    return false;
  }

  @Override
  public Collection<AbstractTreeNode> getChildrenImpl() {
    final ArrayList<AbstractTreeNode> result = new ArrayList<>(myChildNodes.size());
    for (PsiFileNode node : myChildNodes) {
      PsiFile value = node.getValue();
      if (value != null) {
        result.add(new PsiFileNode(node.getProject(), value, node.getSettings()));
      }
    }

    final Collection<AbstractTreeNode> superChildren = super.getChildrenImpl();
    if (superChildren != null) {
      result.addAll(superChildren);
    }

    return result;
  }

  @Override
  public boolean contains(@NotNull final VirtualFile file) {
    if (super.contains(file)) return true;

    for (PsiFileNode node : myChildNodes) {
      final PsiFile psiFile = node.getValue();
      if (psiFile != null && file.equals(psiFile.getVirtualFile())) {
        return true;
      }
    }

    return false;
  }
}
