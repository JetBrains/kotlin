// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.refactoring.rename;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.refactoring.RefactoringSettings;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author yole
 */
public class RenamePsiFileProcessor extends RenamePsiElementProcessor {
  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    return element instanceof PsiFileSystemItem;
  }

  @NotNull
  @Override
  public RenameDialog createRenameDialog(@NotNull Project project, @NotNull final PsiElement element, PsiElement nameSuggestionContext, Editor editor) {
    return new PsiFileRenameDialog(project, element, nameSuggestionContext, editor);
  }

  private static boolean getSearchForReferences(PsiElement element) {
    return element instanceof PsiFile
      ? RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_FILE
      : RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_DIRECTORY;
  }

  @NotNull
  @Override
  public Collection<PsiReference> findReferences(@NotNull PsiElement element,
                                                 @NotNull SearchScope searchScope,
                                                 boolean searchInCommentsAndStrings) {
    if (!getSearchForReferences(element)) {
      return Collections.emptyList();
    }
    return super.findReferences(element, searchScope, searchInCommentsAndStrings);
  }

  public static class PsiFileRenameDialog extends RenameWithOptionalReferencesDialog {
    public PsiFileRenameDialog(Project project, PsiElement element, PsiElement nameSuggestionContext, Editor editor) {
      super(project, element, nameSuggestionContext, editor);
    }

    @Override
    protected boolean getSearchForReferences() {
      return RenamePsiFileProcessor.getSearchForReferences(getPsiElement());
    }

    @Override
    protected void setSearchForReferences(boolean value) {
      if (getPsiElement() instanceof PsiFile) {
        RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_FILE = value;
      }
      else {
        RefactoringSettings.getInstance().RENAME_SEARCH_FOR_REFERENCES_FOR_DIRECTORY = value;
      }
    }
  }
}
