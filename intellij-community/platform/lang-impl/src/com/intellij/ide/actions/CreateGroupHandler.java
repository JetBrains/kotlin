// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.InputValidatorEx;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

abstract class CreateGroupHandler implements InputValidatorEx {

  @NotNull protected final Project myProject;
  @NotNull protected final PsiDirectory myDirectory;

  @Nullable protected PsiFileSystemItem createdElement;
  @Nullable protected String errorText;

  CreateGroupHandler(@NotNull Project project, @NotNull PsiDirectory directory) {
    myProject = project;
    myDirectory = directory;
  }

  @Nullable
  @Override
  public String getErrorText(String inputString) {
    return errorText;
  }

  @Nullable
  PsiFileSystemItem getCreatedElement() {
    return createdElement;
  }

  @NotNull
  abstract String getInitialText();
}
