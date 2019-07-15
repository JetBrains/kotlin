// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.daemon.quickFix;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CreateDirectoryFix extends LocalQuickFixAndIntentionActionOnPsiElement {
  private static final int REFRESH_INTERVAL = 1000;

  private final String myNewFileName;
  @NotNull private final String myKey;
  private final SmartPsiElementPointer<PsiDirectory> myDirectory;
  private boolean myIsAvailable;
  private long myIsAvailableTimeStamp;

  // invoked from other module
  @SuppressWarnings("WeakerAccess")
  public CreateDirectoryFix(@NotNull String newFileName,
                            @NotNull PsiElement element,
                            @NotNull PsiDirectory directory,
                            @NotNull String key) {
    super(element);

    myDirectory = SmartPointerManager.getInstance(directory.getProject()).createSmartPsiElementPointer(directory);
    myNewFileName = newFileName;
    myKey = key;
    myIsAvailable = true;
    myIsAvailableTimeStamp = System.currentTimeMillis();
  }

  public CreateDirectoryFix(@NotNull String newFileName, @NotNull PsiElement element, @NotNull PsiDirectory directory) {
    this(newFileName, element, directory, "create.directory.text");
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
  public void invoke(@NotNull Project project,
                     @NotNull PsiFile file,
                     @Nullable Editor editor,
                     @NotNull PsiElement startElement,
                     @NotNull PsiElement endElement) {
    if (isAvailable(project, null, file)) {
      assert myDirectory.getElement() != null;

      invoke(myDirectory.getElement());
    }
  }

  @Override
  public void applyFix() {
    PsiDirectory directory = myDirectory.getElement();
    if (directory == null) return;
    invoke(directory);
  }

  @Override
  public boolean isAvailable(@NotNull Project project,
                             @NotNull PsiFile file,
                             @NotNull PsiElement startElement,
                             @NotNull PsiElement endElement) {
    PsiDirectory directory = myDirectory.getElement();
    if (directory == null) return false;

    long current = System.currentTimeMillis();

    if (ApplicationManager.getApplication().isUnitTestMode() || current - myIsAvailableTimeStamp > REFRESH_INTERVAL) {
      myIsAvailable &= directory.getVirtualFile().findChild(myNewFileName) == null;
      myIsAvailableTimeStamp = current;
    }

    return myIsAvailable;
  }

  private void invoke(PsiDirectory myDirectory) throws IncorrectOperationException {
    myIsAvailableTimeStamp = 0; // to revalidate applicability

    try {
      myDirectory.createSubdirectory(myNewFileName);
    }
    catch (IncorrectOperationException e) {
      myIsAvailable = false;
    }
  }
}
