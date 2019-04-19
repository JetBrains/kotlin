// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.projectView.impl;

import com.intellij.ide.IdeView;
import com.intellij.ide.util.DirectoryChooserUtil;
import com.intellij.ide.util.EditorHelper;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static com.intellij.psi.util.PsiUtilCore.getVirtualFile;

public final class IdeViewForProjectViewPane implements IdeView {
  private final Supplier<? extends AbstractProjectViewPane> supplier;

  public IdeViewForProjectViewPane(Supplier<? extends AbstractProjectViewPane> supplier) {
    this.supplier = supplier;
  }

  @Nullable
  @Override
  public PsiDirectory getOrChooseDirectory() {
    return DirectoryChooserUtil.getOrChooseDirectory(this);
  }

  @NotNull
  @Override
  public PsiDirectory[] getDirectories() {
    AbstractProjectViewPane pane = supplier.get();
    return pane == null ? PsiDirectory.EMPTY_ARRAY : pane.getSelectedDirectories();
  }

  @Override
  public void selectElement(PsiElement element) {
    AbstractProjectViewPane pane = supplier.get();
    if (pane == null || element == null) return;
    VirtualFile file = getVirtualFile(element);
    boolean requestFocus = element instanceof PsiDirectory;
    pane.select(element, file, requestFocus);
    if (!requestFocus && null == EditorHelper.openInEditor(element, false, true)) {
      pane.select(element, file, true);
    }
  }
}
