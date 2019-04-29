// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.changeSignature.inplace;

import com.intellij.codeHighlighting.TextEditorHighlightingPass;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactory;
import com.intellij.codeHighlighting.TextEditorHighlightingPassFactoryRegistrar;
import com.intellij.codeHighlighting.TextEditorHighlightingPassRegistrar;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.HighlightInfoType;
import com.intellij.codeInsight.daemon.impl.UpdateHighlightersUtil;
import com.intellij.codeInsight.daemon.impl.quickfix.QuickFixAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collection;
import java.util.Collections;

final class ChangeSignaturePassFactory implements TextEditorHighlightingPassFactory, TextEditorHighlightingPassFactoryRegistrar {
  @Override
  public void registerHighlightingPassFactory(@NotNull TextEditorHighlightingPassRegistrar registrar, @NotNull Project project) {
    registrar.registerTextEditorHighlightingPass(this, null, null, true, -1);
  }

  @Override
  public TextEditorHighlightingPass createHighlightingPass(@NotNull final PsiFile file, @NotNull final Editor editor) {
    LanguageChangeSignatureDetector<ChangeInfo> detector =
      LanguageChangeSignatureDetectors.INSTANCE.forLanguage(file.getLanguage());
    if (detector == null) return null;

    return new ChangeSignaturePass(file.getProject(), file, editor);
  }

  private static class ChangeSignaturePass extends TextEditorHighlightingPass {
    @NonNls private static final String SIGNATURE_SHOULD_BE_POSSIBLY_CHANGED = "Signature change was detected";
    private final Project myProject;
    private final PsiFile myFile;
    private final Editor myEditor;

    ChangeSignaturePass(Project project, PsiFile file, Editor editor) {
      super(project, editor.getDocument(), true);
      myProject = project;
      myFile = file;
      myEditor = editor;
    }

    @Override
    public void doCollectInformation(@NotNull ProgressIndicator progress) {}

    @Override
    public void doApplyInformationToEditor() {
      HighlightInfo info = null;
      final InplaceChangeSignature currentRefactoring = InplaceChangeSignature.getCurrentRefactoring(myEditor);
      if (currentRefactoring != null) {
        final ChangeInfo changeInfo = currentRefactoring.getStableChange();
        final PsiElement element = changeInfo.getMethod();
        int offset = myEditor.getCaretModel().getOffset();
        if (element == null || !element.isValid()) return;
        final TextRange elementTextRange = element.getTextRange();
        if (elementTextRange == null || !elementTextRange.contains(offset)) return;
        final LanguageChangeSignatureDetector<ChangeInfo> detector = LanguageChangeSignatureDetectors.INSTANCE.forLanguage(changeInfo.getLanguage());
        TextRange range = detector.getHighlightingRange(changeInfo);
        TextAttributes attributes = new TextAttributes(null, null,
                                                       myEditor.getColorsScheme().getAttributes(CodeInsightColors.WEAK_WARNING_ATTRIBUTES)
                                                         .getEffectColor(),
                                                       null, Font.PLAIN);
        HighlightInfo.Builder builder = HighlightInfo.newHighlightInfo(HighlightInfoType.INFORMATION).range(range);
        builder.textAttributes(attributes);
        builder.descriptionAndTooltip(SIGNATURE_SHOULD_BE_POSSIBLY_CHANGED);
        info = builder.createUnconditionally();
        QuickFixAction.registerQuickFixAction(info, new ApplyChangeSignatureAction(currentRefactoring.getInitialName()));
      }
      Collection<HighlightInfo> infos = info != null ? Collections.singletonList(info) : Collections.emptyList();
      UpdateHighlightersUtil.setHighlightersToEditor(myProject, myDocument, 0, myFile.getTextLength(), infos, getColorsScheme(), getId());
    }
  }
}
