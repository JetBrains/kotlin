// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.openapi.editor.HectorComponentPanel;
import com.intellij.openapi.editor.HectorComponentPanelsProvider;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.include.FileIncludeManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class FileIncludeContextHectorProvider implements HectorComponentPanelsProvider {
  @Override
  @Nullable
  public HectorComponentPanel createConfigurable(@NotNull final PsiFile file) {
    if (DumbService.getInstance(file.getProject()).isDumb()) {
      return null;
    }

    FileIncludeManager includeManager = FileIncludeManager.getManager(file.getProject());
    if (includeManager.getIncludingFiles(file.getVirtualFile(), false).length > 0) {
      return new FileIncludeContextHectorPanel(file, includeManager);
    }
    return null;
  }
}
