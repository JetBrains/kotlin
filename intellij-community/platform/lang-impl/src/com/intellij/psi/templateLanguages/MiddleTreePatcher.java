// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.psi.templateLanguages;

import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.TreeElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Inserts the {@link OuterLanguageElement} so that it isn't a first child of the parent (unless it's the very first element in the file).
 */
public class MiddleTreePatcher implements TreePatcher {
  @Override
  public void insert(@NotNull CompositeElement parent, TreeElement anchorBefore, @NotNull OuterLanguageElement toInsert) {
    anchorBefore = findTopmostAnchor(anchorBefore);
    if (anchorBefore != null) {
      anchorBefore.rawInsertBeforeMe((TreeElement)toInsert);
    }
    else {
      parent.rawAddChildren((TreeElement)toInsert);
    }
  }

  private static TreeElement findTopmostAnchor(@Nullable TreeElement anchorBefore) {
    while (anchorBefore != null && anchorBefore.getTreePrev() == null) {
      CompositeElement parent = anchorBefore.getTreeParent();
      if (parent.getTreeParent() == null) break;
      anchorBefore = parent;
    }
    return anchorBefore;
  }

}
