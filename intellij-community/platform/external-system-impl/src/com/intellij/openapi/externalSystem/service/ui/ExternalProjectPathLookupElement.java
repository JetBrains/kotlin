// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.externalSystem.service.ui;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.FoldRegion;
import com.intellij.openapi.editor.FoldingModel;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

/**
 * @author Denis Zhdanov
 */
public class ExternalProjectPathLookupElement extends LookupElement {
  
  @NotNull private final String myProjectName;
  @NotNull private final String myProjectPath;

  public ExternalProjectPathLookupElement(@NotNull String projectName, @NotNull String projectPath) {
    myProjectName = projectName;
    myProjectPath = projectPath;
  }

  @NotNull
  @Override
  public String getLookupString() {
    return myProjectName;
  }

  @Override
  public void handleInsert(@NotNull InsertionContext context) {
    Editor editor = context.getEditor();
    final FoldingModel foldingModel = editor.getFoldingModel();
    foldingModel.runBatchFoldingOperation(() -> {
      FoldRegion[] regions = foldingModel.getAllFoldRegions();
      for (FoldRegion region : regions) {
        foldingModel.removeFoldRegion(region);
      }
    });
    
    final Document document = editor.getDocument();
    final int startOffset = context.getStartOffset();
    
    document.replaceString(startOffset, document.getTextLength(), myProjectPath);
    final Project project = context.getProject();
    PsiDocumentManager.getInstance(project).commitDocument(document);
    
    foldingModel.runBatchFoldingOperationDoNotCollapseCaret(() -> {
      FoldRegion region = foldingModel.addFoldRegion(startOffset, startOffset + myProjectPath.length(), myProjectName);
      if (region != null) {
        region.setExpanded(false);
      }
    });
  }
}
