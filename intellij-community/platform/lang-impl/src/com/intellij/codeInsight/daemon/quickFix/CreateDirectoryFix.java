// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CreateDirectoryFix extends AbstractCreateFileFix {
  // invoked from other module
  @SuppressWarnings("WeakerAccess")
  public CreateDirectoryFix(@NotNull PsiElement psiElement,
                            @NotNull List<TargetDirectory> directories,
                            @NotNull String[] subPath,
                            @NotNull String newDirectoryName,
                            @NotNull String fixLocaleKey) {
    super(psiElement, newDirectoryName, directories, subPath, fixLocaleKey);

    myIsAvailable = true;
    myIsAvailableTimeStamp = System.currentTimeMillis();
  }

  public CreateDirectoryFix(@NotNull PsiElement psiElement,
                            @NotNull List<TargetDirectory> directories,
                            @NotNull String[] subPath,
                            @NotNull String newDirectoryName) {
    this(psiElement, directories, subPath, newDirectoryName, "create.directory.text");
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
  protected void apply(@NotNull Project project, TargetDirectory directory) throws IncorrectOperationException {
    myIsAvailableTimeStamp = 0; // to revalidate applicability

    PsiDirectory currentDirectory = directory.getDirectory();
    if (currentDirectory == null) {
      return;
    }

    try {
      for (String pathPart : directory.getPathToCreate()) {
        currentDirectory = findOrCreateSubdirectory(currentDirectory, pathPart);
      }
      for (String pathPart : mySubPath) {
        currentDirectory = findOrCreateSubdirectory(currentDirectory, pathPart);
      }
      currentDirectory.createSubdirectory(myNewFileName);
    }
    catch (IncorrectOperationException e) {
      myIsAvailable = false;
    }
  }
}
