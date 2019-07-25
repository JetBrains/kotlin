// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.arrangement.action;

import com.intellij.application.options.CodeStyle;
import com.intellij.codeInsight.actions.RearrangeCodeProcessor;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.arrangement.Rearranger;
import org.jetbrains.annotations.NotNull;

/**
 * Arranges content at the target file(s).
 * 
 * @author Denis Zhdanov
 */
public class RearrangeCodeAction extends AnAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    PsiFile file = e.getData(CommonDataKeys.PSI_FILE);
    boolean enabled = file != null && Rearranger.EXTENSION.forLanguage(file.getLanguage()) != null && CodeStyle.isFormattingEnabled(file);
    e.getPresentation().setEnabled(enabled);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null) {
      return;
    }
    
    final Editor editor = e.getData(CommonDataKeys.EDITOR);
    if (editor == null) {
      return;
    }
    
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    Document document = editor.getDocument();
    documentManager.commitDocument(document);
    
    final PsiFile file = documentManager.getPsiFile(document);
    if (file == null) {
      return;
    }

    SelectionModel model = editor.getSelectionModel();
    if (model.hasSelection()) {
      new RearrangeCodeProcessor(file, model).run();
    }
    else {
      new RearrangeCodeProcessor(file).run();
    }
  }
}
