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

package com.intellij.codeInsight.folding.impl.actions;

import com.intellij.codeInsight.folding.CodeFoldingManager;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Caret;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.actionSystem.EditorAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExpandAllRegionsAction extends EditorAction {
  public ExpandAllRegionsAction() {
    super(new BaseFoldingHandler() {
      @Override
      public void doExecute(@NotNull final Editor editor, @Nullable Caret caret, DataContext dataContext) {
        Project project = editor.getProject();
        assert project != null;
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        CodeFoldingManager codeFoldingManager = CodeFoldingManager.getInstance(project);
        codeFoldingManager.updateFoldRegions(editor);

        final List<FoldRegion> regions = getFoldRegionsForSelection(editor, caret);
        editor.getFoldingModel().runBatchFoldingOperation(() -> {
          boolean anythingDone = false;
          for (FoldRegion region : regions) {
            // try to restore to default state at first
            Boolean collapsedByDefault = codeFoldingManager.isCollapsedByDefault(region);
            if (!region.isExpanded() && (collapsedByDefault == null || !collapsedByDefault)) {
              region.setExpanded(true);
              anythingDone = true;
            }
          }

          if (!anythingDone){
            for (FoldRegion region : regions) {
              region.setExpanded(true);
            }
          }

        });
      }
    });
  }

}
