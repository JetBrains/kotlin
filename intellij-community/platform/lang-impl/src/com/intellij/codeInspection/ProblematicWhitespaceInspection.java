/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInspection;

import com.intellij.application.options.CodeStyle;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorSettings;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.LanguageFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.codeStyle.CodeStyleSettings;
import com.intellij.psi.codeStyle.CommonCodeStyleSettings;
import org.jetbrains.annotations.NotNull;

/**
 * @author Bas Leijdekkers
 */
public class ProblematicWhitespaceInspection extends LocalInspectionTool {

  private static class ShowWhitespaceFix implements LocalQuickFix {

    @NotNull
    @Override
    public String getFamilyName() {
      return InspectionsBundle.message("problematic.whitespace.show.whitespaces.quickfix");
    }

    @Override
    public boolean startInWriteAction() {
      return false;
    }

    @Override
    public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
      final FileEditorManager editorManager = FileEditorManager.getInstance(project);
      final Editor editor = editorManager.getSelectedTextEditor();
      if (editor == null) {
        return;
      }
      final EditorSettings settings = editor.getSettings();
      settings.setWhitespacesShown(!settings.isWhitespacesShown());
      editor.getComponent().repaint();
    }
  }

  @NotNull
  @Override
  public PsiElementVisitor buildVisitor(@NotNull ProblemsHolder holder, boolean isOnTheFly) {
    return new ProblematicWhitespaceVisitor(holder, isOnTheFly);
  }

  private class ProblematicWhitespaceVisitor extends PsiElementVisitor {

    private final ProblemsHolder myHolder;
    private final boolean myIsOnTheFly;

    ProblematicWhitespaceVisitor(ProblemsHolder holder, boolean isOnTheFly) {
      myHolder = holder;
      myIsOnTheFly = isOnTheFly;
    }

    @Override
    public void visitFile(PsiFile file) {
      super.visitFile(file);
      final FileType fileType = file.getFileType();
      if (!(fileType instanceof LanguageFileType)) {
        return;
      }
      final CodeStyleSettings settings = CodeStyle.getSettings(file);
      final CommonCodeStyleSettings.IndentOptions indentOptions = settings.getIndentOptions(fileType);
      final boolean useTabs = indentOptions.USE_TAB_CHARACTER;
      final boolean smartTabs = indentOptions.SMART_TABS;
      final Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
      if (document == null) {
        return;
      }
      final int lineCount = document.getLineCount();
      int previousLineIndent = 0;
      for (int i = 0; i < lineCount; i++) {
        final int startOffset = document.getLineStartOffset(i);
        final int endOffset = document.getLineEndOffset(i);
        final String line = document.getText(new TextRange(startOffset, endOffset));
        boolean spaceSeen = false;
        for (int j = 0, length = line.length(); j < length; j++) {
          final char c = line.charAt(j);
          if (c == '\t') {
            if (useTabs) {
              if (smartTabs && spaceSeen) {
                if (registerError(file, startOffset, true)) {
                  return;
                }
              }
            }
            else {
              if (registerError(file, startOffset, false)) {
                return;
              }
            }
          }
          else if (c == ' ') {
            if (useTabs) {
              if (!smartTabs) {
                if (registerError(file, startOffset, true)) {
                  return;
                }
              }
              else if (!spaceSeen) {
                final int currentIndent = Math.max(0, j);
                if (currentIndent < previousLineIndent) {
                  if (registerError(file, startOffset, true)) {
                    return;
                  }
                }
                previousLineIndent = currentIndent;
              }
            }
            spaceSeen = true;
          }
          else {
            if (!spaceSeen) {
              previousLineIndent = Math.max(0, j);
            }
            break;
          }
        }
      }
    }

    private boolean registerError(PsiFile file, int startOffset, boolean tab) {
      final PsiElement element = file.findElementAt(startOffset);
      if (element != null && isSuppressedFor(element)) {
        return false;
      }
      final String description = tab
                                 ? InspectionsBundle.message("problematic.whitespace.spaces.problem.descriptor", file.getName())
                                 : InspectionsBundle.message("problematic.whitespace.tabs.problem.descriptor", file.getName());
      if (myIsOnTheFly) {
        myHolder.registerProblem(file, description, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, new ShowWhitespaceFix());
      }
      else {
        myHolder.registerProblem(file, description, ProblemHighlightType.GENERIC_ERROR_OR_WARNING);
      }
      return true;
    }
  }
}
