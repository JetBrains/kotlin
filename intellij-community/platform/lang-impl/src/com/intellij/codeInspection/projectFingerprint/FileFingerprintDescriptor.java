// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInspection.projectFingerprint;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ex.ProblemDescriptorImpl;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

public class FileFingerprintDescriptor extends ProblemDescriptorImpl {
  private final FileFingerprint myFileFingerprint;

  public FileFingerprintDescriptor(@NotNull PsiFile psiFile, @NotNull FileFingerprint fingerprint) {
    super(psiFile, psiFile, "Fingerprint", LocalQuickFix.EMPTY_ARRAY, ProblemHighlightType.GENERIC_ERROR_OR_WARNING, false, null, null,
          false);
    myFileFingerprint = fingerprint;
  }

  @NotNull
  public FileFingerprint getFileFingerprint() {
    return myFileFingerprint;
  }
}
