// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.codeInsight.navigation;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import gnu.trove.THashSet;
import gnu.trove.TIntArrayList;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public final class MethodUpDownUtil {
  private MethodUpDownUtil() {
  }

  public static int[] getNavigationOffsets(PsiFile file, final int caretOffset) {
    for (MethodNavigationOffsetProvider provider : MethodNavigationOffsetProvider.EP_NAME.getExtensionList()) {
      int[] offsets = provider.getMethodNavigationOffsets(file, caretOffset);
      if (offsets != null && offsets.length > 0) {
        return offsets;
      }
    }

    Collection<PsiElement> array = new THashSet<>();
    addNavigationElements(array, file);
    return offsetsFromElements(array);
  }

  public static int[] offsetsFromElements(final Collection<? extends PsiElement> array) {
    TIntArrayList offsets = new TIntArrayList(array.size());
    for (PsiElement element : array) {
      int offset = element.getTextOffset();
      assert offset >= 0 : element + " ("+element.getClass()+"); offset: " + offset;
      offsets.add(offset);
    }
    offsets.sort();
    return offsets.toNativeArray();
  }

  private static void addNavigationElements(Collection<? super PsiElement> array, PsiFile element) {
    StructureViewBuilder structureViewBuilder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(element);
    if (structureViewBuilder instanceof TreeBasedStructureViewBuilder) {
      TreeBasedStructureViewBuilder builder = (TreeBasedStructureViewBuilder) structureViewBuilder;
      StructureViewModel model = builder.createStructureViewModel(null);
      try {
        addStructureViewElements(model.getRoot(), array, element);
      }
      finally {
        Disposer.dispose(model);
      }
    }
  }

  private static void addStructureViewElements(final TreeElement parent, final Collection<? super PsiElement> array, @NotNull PsiFile file) {
    for(TreeElement treeElement: parent.getChildren()) {
      Object value = ((StructureViewTreeElement)treeElement).getValue();
      if (value instanceof PsiElement) {
        PsiElement element = (PsiElement)value;
        if (array.contains(element) || !file.equals(element.getContainingFile())) continue;
        array.add(element);
      }
      addStructureViewElements(treeElement, array, file);
    }
  }
}