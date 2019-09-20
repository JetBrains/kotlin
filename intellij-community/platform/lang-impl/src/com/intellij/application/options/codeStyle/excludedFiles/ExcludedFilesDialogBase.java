// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.application.options.codeStyle.excludedFiles;

import com.intellij.formatting.fileSet.FileSetDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;

public abstract class ExcludedFilesDialogBase extends DialogWrapper {

  protected ExcludedFilesDialogBase(@Nullable Project project) {
    super(project);
  }

  protected ExcludedFilesDialogBase(boolean canBeParent) {
    super(canBeParent);
  }

  @Nullable
  public abstract FileSetDescriptor getDescriptor();
}
