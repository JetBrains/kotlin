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

package com.intellij.codeInsight.navigation;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ScrollType;
import com.intellij.openapi.fileEditor.ex.IdeDocumentHistory;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MethodDownHandler implements CodeInsightActionHandler {
  @Override
  public void invoke(@NotNull Project project, @NotNull Editor editor, @NotNull PsiFile file) {
    LookupManager.getInstance(project).hideActiveLookup();

    int caretOffset = editor.getCaretModel().getOffset();
    int caretLine = editor.getCaretModel().getLogicalPosition().line;
    int[] offsets = MethodUpDownUtil.getNavigationOffsets(file, caretOffset);
    for (int offset : offsets) {
      if (offset > caretOffset) {
        int line = editor.offsetToLogicalPosition(offset).line;
        if (line > caretLine) {
          editor.getCaretModel().removeSecondaryCarets();
          editor.getCaretModel().moveToOffset(offset);
          editor.getSelectionModel().removeSelection();
          editor.getScrollingModel().scrollToCaret(ScrollType.CENTER_DOWN);
          IdeDocumentHistory.getInstance(project).includeCurrentCommandAsNavigation();
          break;
        }
      }
    }
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@NotNull PsiFile currentFile) {
    return null;
  }
}
