// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.ReadonlyStatusHandler;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Set;

/**
 * @author peter
 */
public class InvokeTemplateAction extends AnAction {
  private final TemplateImpl myTemplate;
  private final Editor myEditor;
  private final Project myProject;

  public InvokeTemplateAction(TemplateImpl template, Editor editor, Project project, Set<Character> usedMnemonicsSet) {
    super(extractMnemonic(template.getKey(), usedMnemonicsSet) +
          (StringUtil.isEmptyOrSpaces(template.getDescription()) ? "" : ". " + template.getDescription()));
    myTemplate = template;
    myProject = project;
    myEditor = editor;
  }

  public static String extractMnemonic(String caption, Set<? super Character> usedMnemonics) {
    if (StringUtil.isEmpty(caption)) return "";

    for (int i = 0; i < caption.length(); i++) {
      char c = caption.charAt(i);
      if (usedMnemonics.add(Character.toUpperCase(c))) {
        return caption.substring(0, i) + UIUtil.MNEMONIC + caption.substring(i);
      }
    }

    return caption + " ";
  }

  public TemplateImpl getTemplate() {
    return myTemplate;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    perform();
  }

  public void perform() {
    final Document document = myEditor.getDocument();
    final VirtualFile file = FileDocumentManager.getInstance().getFile(document);
    if (file != null) {
      ReadonlyStatusHandler.getInstance(myProject).ensureFilesWritable(Collections.singletonList(file));
    }

    CommandProcessor.getInstance().executeCommand(myProject, () -> myEditor.getCaretModel().runForEachCaret(__ -> {
      // adjust the selection so that it starts with a non-whitespace character (to make sure that the template is inserted
      // at a meaningful position rather than at indent 0)
      if (myEditor.getSelectionModel().hasSelection() && myTemplate.isToReformat()) {
        int offset = myEditor.getSelectionModel().getSelectionStart();
        int selectionEnd = myEditor.getSelectionModel().getSelectionEnd();
        int lineEnd = document.getLineEndOffset(document.getLineNumber(offset));
        while (offset < lineEnd && offset < selectionEnd &&
               (document.getCharsSequence().charAt(offset) == ' ' || document.getCharsSequence().charAt(offset) == '\t')) {
          offset++;
        }
        // avoid extra line break after $SELECTION$ in case when selection ends with a complete line
        if (selectionEnd == document.getLineStartOffset(document.getLineNumber(selectionEnd))) {
          selectionEnd--;
        }
        if (offset < lineEnd && offset < selectionEnd) {  // found non-WS character in first line of selection
          myEditor.getSelectionModel().setSelection(offset, selectionEnd);
        }
      }
      String selectionString = myEditor.getSelectionModel().getSelectedText();
      TemplateManager.getInstance(myProject).startTemplate(myEditor, selectionString, myTemplate);
    }), "Wrap with template", "Wrap with template " + myTemplate.getKey());
  }
}
