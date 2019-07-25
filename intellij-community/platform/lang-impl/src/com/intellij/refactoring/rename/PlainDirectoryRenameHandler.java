// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename;

import com.intellij.ide.projectView.impl.RenameModuleHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;

import java.util.Arrays;

/**
 * Explicit handler for renaming directories which don't correspond to packages. It duplicates behavior of the default {@link PsiElementRenameHandler}.
 * The explicit handler is needed to suggest choosing what user wants to rename if the rename refactoring is invoked on a directory which
 * has an additional {@link RenameHandler} (e.g. {@link RenameModuleHandler}).
 */
public class PlainDirectoryRenameHandler extends DirectoryRenameHandlerBase {
  @Override
  protected boolean isSuitableDirectory(PsiDirectory directory) {
    return Arrays.stream(EP_NAME.getExtensions()).noneMatch(extension -> extension instanceof DirectoryAsPackageRenameHandlerBase<?> &&
                                                                         ((DirectoryAsPackageRenameHandlerBase)extension).getPackage(directory) != null);
  }

  @Override
  protected void doRename(PsiElement element, Project project, PsiElement nameSuggestionContext, Editor editor) {
    PsiElementRenameHandler.rename(element, project, nameSuggestionContext, editor);
  }
}
