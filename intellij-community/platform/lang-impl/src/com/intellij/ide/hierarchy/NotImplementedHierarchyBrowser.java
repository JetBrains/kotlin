// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.hierarchy;

import com.intellij.psi.PsiElement;
import com.intellij.ui.content.Content;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class NotImplementedHierarchyBrowser implements HierarchyBrowser {
  private final PsiElement myTarget;

  public NotImplementedHierarchyBrowser(@NotNull PsiElement target) {myTarget = target;}

  @Override
  public JComponent getComponent() {
    return new JLabel("Not implemented. Target: " + myTarget);
  }

  @Override
  public void setContent(Content content) {
  }
}
