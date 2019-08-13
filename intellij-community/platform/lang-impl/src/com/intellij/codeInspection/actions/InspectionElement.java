// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.actions;

import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.FakePsiElement;
import com.intellij.psi.impl.source.DummyHolder;
import com.intellij.psi.impl.source.DummyHolderFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class InspectionElement extends FakePsiElement {
  public static final InspectionElement[] EMPTY_ARRAY = new InspectionElement[0];
  @NotNull private final InspectionToolWrapper myWrapper;
  @NotNull private final PsiManager myPsiManager;
  @NotNull private final DummyHolder myDummyHolder;

  public InspectionElement(@NotNull InspectionToolWrapper wrapper, @NotNull PsiManager psiManager) {
    myWrapper = wrapper;
    myPsiManager = psiManager;
    myDummyHolder = DummyHolderFactory.createHolder(myPsiManager, null);
  }

  @NotNull
  public InspectionToolWrapper getToolWrapper() {
    return myWrapper;
  }

  @Override
  public PsiElement getParent() {
    return myDummyHolder;
  }

  @Override
  public ItemPresentation getPresentation() {
    return new ItemPresentation() {
      @Override
      public String getPresentableText() {
        return myWrapper.getDisplayName();
      }

      @Nullable
      @Override
      public String getLocationString() {
        return null;
      }

      @Nullable
      @Override
      public Icon getIcon(boolean unused) {
        return null;
      }
    };
  }

  @Override
  public PsiManager getManager() {
    return myPsiManager;
  }
}
