// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.impl.ShowIntentionsPass;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ApplyIntentionAction extends AnAction {

  private final IntentionAction myAction;
  private final Editor myEditor;
  private final PsiFile myFile;

  public ApplyIntentionAction(final HighlightInfo.IntentionActionDescriptor descriptor, String text, Editor editor, PsiFile file) {
    this(descriptor.getAction(), text, editor, file);
  }

  public ApplyIntentionAction(final IntentionAction action, String text, Editor editor, PsiFile file) {
    super(text);
    getTemplatePresentation().setText(text, false);
    myAction = action;
    myEditor = editor;
    myFile = file;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    PsiDocumentManager.getInstance(myFile.getProject()).commitAllDocuments();
    ShowIntentionActionsHandler.chooseActionAndInvoke(myFile, myEditor, myAction, myAction.getText());
  }

  public String getName() {
    return ReadAction.compute(() -> myAction.getText());
  }

  @Nullable
  public static ApplyIntentionAction[] getAvailableIntentions(@NotNull Editor editor, @NotNull PsiFile file) {
    ShowIntentionsPass.IntentionsInfo info = ShowIntentionsPass.getActionsToShow(editor, file, false);
    if (info.isEmpty()) return null;

    final List<HighlightInfo.IntentionActionDescriptor> actions = new ArrayList<>();
    actions.addAll(info.errorFixesToShow);
    actions.addAll(info.inspectionFixesToShow);
    actions.addAll(info.intentionsToShow);

    final ApplyIntentionAction[] result = new ApplyIntentionAction[actions.size()];
    for (int i = 0; i < result.length; i++) {
      final HighlightInfo.IntentionActionDescriptor descriptor = actions.get(i);
      final String actionText = ReadAction.compute(() -> descriptor.getAction().getText());
      result[i] = new ApplyIntentionAction(descriptor, actionText, editor, file);
    }
    return result;
  }
}
