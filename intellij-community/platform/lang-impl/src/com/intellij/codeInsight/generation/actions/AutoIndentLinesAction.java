/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package com.intellij.codeInsight.generation.actions;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.actions.BaseCodeInsightAction;
import com.intellij.codeInsight.generation.AutoIndentLinesHandler;
import com.intellij.lang.LanguageFormatting;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.util.DocumentUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AutoIndentLinesAction extends BaseCodeInsightAction implements DumbAware {
  @Nullable
  @Override
  protected Editor getEditor(@NotNull DataContext dataContext, @NotNull Project project, boolean forUpdate) {
    Editor editor = getBaseEditor(dataContext, project);
    if (editor == null) return null;
    Document document = editor.getDocument();
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
    PsiFile psiFile = documentManager.getCachedPsiFile(document);
    if (psiFile == null) return editor;
    if (!forUpdate) documentManager.commitAllDocuments();
    int startLineOffset = DocumentUtil.getLineStartOffset(editor.getSelectionModel().getSelectionStart(), document);
    return InjectedLanguageUtil.getEditorForInjectedLanguageNoCommit(editor, psiFile, startLineOffset);
  }

  @NotNull
  @Override
  protected CodeInsightActionHandler getHandler() {
    return new AutoIndentLinesHandler();
  }

  @Override
  protected boolean isValidForFile(@NotNull Project project, @NotNull Editor editor, @NotNull final PsiFile file) {
    final FileType fileType = file.getFileType();
    return fileType instanceof LanguageFileType &&
           LanguageFormatting.INSTANCE.forContext(((LanguageFileType)fileType).getLanguage(), file) != null;
  }
}