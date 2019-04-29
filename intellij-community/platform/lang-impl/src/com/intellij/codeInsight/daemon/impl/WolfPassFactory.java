// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeHighlighting.*;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class WolfPassFactory implements TextEditorHighlightingPassFactory {
  private long myPsiModificationCount;

  static final class MyRegistrar implements TextEditorHighlightingPassFactoryRegistrar {
    @Override
    public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar, @NotNull Project project) {
      registrar.registerTextEditorHighlightingPass(new WolfPassFactory(), new int[]{Pass.UPDATE_ALL}, new int[]{Pass.LOCAL_INSPECTIONS}, false, Pass.WOLF);
    }
  }

  @Override
  @Nullable
  public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull final Editor editor) {
    Project project = file.getProject();
    final long psiModificationCount = PsiManager.getInstance(project).getModificationTracker().getModificationCount();
    if (psiModificationCount == myPsiModificationCount) {
      return null; //optimization
    }
    return new WolfHighlightingPass(project, editor.getDocument(), file){
      @Override
      protected void applyInformationWithProgress() {
        super.applyInformationWithProgress();
        myPsiModificationCount = psiModificationCount;
      }
    };
  }
}
