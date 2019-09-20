// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.hints;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ParameterHintsPassFactory implements TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
  protected static final Key<Long> PSI_MODIFICATION_STAMP = Key.create("psi.modification.stamp");

  @Override
  public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar, @NotNull Project project) {
    registrar.registerTextEditorHighlightingPass(this, null, null, false, -1);
  }

  @Nullable
  @Override
  public TextEditorHighlightingPass createHighlightingPass(@NotNull PsiFile file, @NotNull Editor editor) {
    if (editor.isOneLineMode()) return null;
    long currentStamp = getCurrentModificationStamp(file);
    Long savedStamp = editor.getUserData(PSI_MODIFICATION_STAMP);
    if (savedStamp != null && savedStamp == currentStamp) return null;
    Language language = file.getLanguage();
    InlayParameterHintsProvider provider = InlayParameterHintsExtension.INSTANCE.forLanguage(language);
    if (provider == null) return null;
    return new ParameterHintsPass(file, editor, MethodInfoBlacklistFilter.forLanguage(language), false);
  }

  public static long getCurrentModificationStamp(@NotNull PsiFile file) {
    return file.getManager().getModificationTracker().getModificationCount();
  }

  public static void forceHintsUpdateOnNextPass() {
    for (Editor editor : EditorFactory.getInstance().getAllEditors()) {
      forceHintsUpdateOnNextPass(editor);
    }
  }

  public static void forceHintsUpdateOnNextPass(@NotNull Editor editor) {
    editor.putUserData(PSI_MODIFICATION_STAMP, null);
  }

  protected static void putCurrentPsiModificationStamp(@NotNull Editor editor, @NotNull PsiFile file) {
    editor.putUserData(PSI_MODIFICATION_STAMP, getCurrentModificationStamp(file));
  }
}