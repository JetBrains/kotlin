// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class CreateDirectoryHandler extends CreateGroupHandler {

  @NotNull private final CreateDirectoryOrPackageHandler myDelegate;

  CreateDirectoryHandler(@NotNull Project project, @NotNull PsiDirectory directory) {
    super(project, directory);
    myDelegate = new CreateDirectoryOrPackageHandler(project, directory, true, "\\/");
  }

  @NotNull
  @Override
  String getInitialText() {
    return "";
  }

  @Override
  public boolean checkInput(String inputString) {
    return myDelegate.checkInput(inputString);
  }

  @Override
  public boolean canClose(String inputString) {
    return myDelegate.canClose(inputString);
  }

  @Nullable
  @Override
  PsiFileSystemItem getCreatedElement() {
    return myDelegate.getCreatedElement();
  }

  @Nullable
  @Override
  public String getErrorText(String inputString) {
    return myDelegate.getErrorText(inputString);
  }
}
