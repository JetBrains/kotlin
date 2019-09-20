// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Quick fix that creates a new directory in one of the target directories. Automatically creates all intermediate directories of
 * {@link TargetDirectory#getPathToCreate()} and {@link NewFileLocation#getSubPath()}. If there are multiple target directories it shows
 * a popup where users can select desired target directory.
 */
public class CreateDirectoryPathFix extends AbstractCreateFileFix {
  // invoked from other module
  @SuppressWarnings("WeakerAccess")
  public CreateDirectoryPathFix(@NotNull PsiElement psiElement,
                                @NotNull NewFileLocation newFileLocation,
                                @NotNull String fixLocaleKey) {
    super(psiElement, newFileLocation, fixLocaleKey);

    myIsAvailable = true;
    myIsAvailableTimeStamp = System.currentTimeMillis();
  }

  public CreateDirectoryPathFix(@NotNull PsiElement psiElement,
                                @NotNull NewFileLocation newFileLocation) {
    this(psiElement, newFileLocation, "create.directory.text");
  }

  @Override
  @NotNull
  public String getText() {
    return CodeInsightBundle.message(myKey, myNewFileName);
  }

  @Override
  @NotNull
  public String getFamilyName() {
    return CodeInsightBundle.message("create.file.family");
  }

  @Nullable
  @Override
  public PsiElement getElementToMakeWritable(@NotNull PsiFile file) {
    return null;
  }

  @Override
  protected void apply(@NotNull Project project, @NotNull PsiDirectory targetDirectory, @Nullable Editor editor)
    throws IncorrectOperationException {

    targetDirectory.createSubdirectory(myNewFileName);
  }
}
