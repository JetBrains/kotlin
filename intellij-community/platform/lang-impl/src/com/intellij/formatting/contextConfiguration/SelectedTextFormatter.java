/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.formatting.contextConfiguration;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleManager;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import org.jetbrains.annotations.NotNull;

public class SelectedTextFormatter {
  private final Project myProject;
  private final Editor myEditor;
  private final PsiFile myFile;

  private final String myTextBefore;
  private final RangeMarker mySelectionRangeMarker;


  public SelectedTextFormatter(Project project, Editor editor, PsiFile file) {
    myProject = project;
    myEditor = editor;
    myFile = file;

    myTextBefore = myEditor.getSelectionModel().getSelectedText();
    mySelectionRangeMarker = myEditor.getDocument().createRangeMarker(myEditor.getSelectionModel().getSelectionStart(),
                                                                      myEditor.getSelectionModel().getSelectionEnd());
  }

  public void restoreSelectedText() {
    final Document document = myEditor.getDocument();
    if (!mySelectionRangeMarker.isValid()) return;
    final int start = mySelectionRangeMarker.getStartOffset();
    final int end = mySelectionRangeMarker.getEndOffset();

    WriteCommandAction.writeCommandAction(myProject)
                      .withName("Configure code style on selected fragment: restore text before")
                      .run(() -> document.replaceString(start, end, myTextBefore));

    myEditor.getSelectionModel().setSelection(start, start + myTextBefore.length());
  }

  void reformatSelectedText(@NotNull CodeStyleSettings reformatSettings) {
    final SelectionModel model = myEditor.getSelectionModel();
    if (model.hasSelection()) {
      CodeStyle.doWithTemporarySettings(myProject, reformatSettings, () -> reformatRange(myFile, getSelectedRange()));
    }
  }

  void reformatWholeFile() {
    reformatRange(myFile, myFile.getTextRange());
  }

  private static void reformatRange(final @NotNull PsiFile file, final @NotNull TextRange range) {
    final Project project = file.getProject();
    CommandProcessor.getInstance().executeCommand(project, () -> ApplicationManager.getApplication().runWriteAction(() -> CodeStyleManager.getInstance(project).reformatText(file, range.getStartOffset(), range.getEndOffset())), "Reformat", null);
  }

  @NotNull
  TextRange getSelectedRange() {
    SelectionModel model = myEditor.getSelectionModel();
    int start = model.getSelectionStart();
    int end = model.getSelectionEnd();
    return TextRange.create(start, end);
  }
}