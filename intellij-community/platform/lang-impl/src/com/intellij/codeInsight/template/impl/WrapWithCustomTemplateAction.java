// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

public class WrapWithCustomTemplateAction extends AnAction {
  private final CustomLiveTemplate myTemplate;
  private final Editor myEditor;
  private final PsiFile myFile;

  public WrapWithCustomTemplateAction(CustomLiveTemplate template,
                                      final Editor editor,
                                      final PsiFile file,
                                      final Set<Character> usedMnemonicsSet) {
    super(InvokeTemplateAction.extractMnemonic(template.getTitle(), usedMnemonicsSet));
    myTemplate = template;
    myFile = file;
    myEditor = editor;
  }


  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    perform();
  }

  public void perform() {
    final Document document = myEditor.getDocument();
    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null) {
      ReadonlyStatusHandler.getInstance(myFile.getProject()).ensureFilesWritable(Collections.singletonList(file));
    }

    String selection = myEditor.getSelectionModel().getSelectedText(true);

    if (selection != null) {
      selection = selection.trim();
      PsiDocumentManager.getInstance(myFile.getProject()).commitAllDocuments();
      myTemplate.wrap(selection, new CustomTemplateCallback(myEditor, myFile));
    }
  }
}
