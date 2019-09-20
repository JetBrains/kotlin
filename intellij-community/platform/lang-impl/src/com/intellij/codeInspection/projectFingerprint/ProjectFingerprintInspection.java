// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.projectFingerprint;

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ProjectFingerprintInspection extends LocalInspectionTool {
  @Nullable
  @Override
  public ProblemDescriptor[] checkFile(@NotNull PsiFile file, @NotNull InspectionManager manager, boolean isOnTheFly) {
    if (!ApplicationManager.getApplication().isHeadlessEnvironment()) return null;
    Document document = PsiDocumentManager.getInstance(file.getProject()).getDocument(file);
    if (document == null || file.getViewProvider().getBaseLanguage() != file.getLanguage()) return null;
    return new FileFingerprintDescriptor[]{createDescriptor(document, file)};
  }

  @NotNull
  private static FileFingerprintDescriptor createDescriptor(@NotNull Document document, @NotNull PsiFile file) {
    return new FileFingerprintDescriptor(file, new FileFingerprint(file, document));
  }
}
