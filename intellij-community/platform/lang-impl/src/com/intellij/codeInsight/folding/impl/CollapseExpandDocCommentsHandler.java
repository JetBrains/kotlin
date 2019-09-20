/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.codeInsight.folding.impl;

import com.intellij.codeInsight.CodeInsightActionHandler;
import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiDocCommentBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CollapseExpandDocCommentsHandler implements CodeInsightActionHandler {

  private static final Key<Boolean> DOC_COMMENT_MARK = Key.create("explicit.fold.region.doc.comment.mark");

  public static void setDocCommentMark(@NotNull FoldRegion region, boolean value) {
    region.putUserData(DOC_COMMENT_MARK, value);
  }

  private final boolean myExpand;

  public CollapseExpandDocCommentsHandler(boolean isExpand) {
    myExpand = isExpand;
  }

  @Override
  public void invoke(@NotNull Project project, @NotNull final Editor editor, @NotNull PsiFile file){
    CodeFoldingManager foldingManager = CodeFoldingManager.getInstance(project);
    foldingManager.updateFoldRegions(editor);
    final FoldRegion[] allFoldRegions = editor.getFoldingModel().getAllFoldRegions();
    Runnable processor = () -> {
      for (FoldRegion region : allFoldRegions) {
        PsiElement element = EditorFoldingInfo.get(editor).getPsiElement(region);
        if (element instanceof PsiDocCommentBase || Boolean.TRUE.equals(region.getUserData(DOC_COMMENT_MARK))) {
          region.setExpanded(myExpand);
        }
      }
    };
    editor.getFoldingModel().runBatchFoldingOperation(processor);
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@NotNull PsiFile currentFile) {
    return null;
  }
}
