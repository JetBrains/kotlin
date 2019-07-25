/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.refactoring.safeDelete;

import com.intellij.ide.projectView.impl.NestingTreeStructureProvider;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.refactoring.RefactoringSettings;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * According to {@link NestingTreeStructureProvider} some files in the Project View are shown as
 * children of another peer file. When going to delete such 'parent' file {@link RelatedFilesSafeDeleteProcessorDelegate}
 * suggests to delete child files as well. Example: when deleting foo.ts file user is suggested to delete generated foo.js and foo.js.map
 * files as well.
 */
public class RelatedFilesSafeDeleteProcessorDelegate implements SafeDeleteProcessorDelegate {
  @Override
  public boolean handlesElement(final PsiElement element) {
    return element instanceof PsiFile &&
           element.isValid() &&
           ((PsiFile)element).getVirtualFile() != null &&
           !NestingTreeStructureProvider.getFilesShownAsChildrenInProjectView(element.getProject(),
                                                                              ((PsiFile)element).getVirtualFile()).isEmpty();
  }

  @Override
  public Collection<PsiElement> getAdditionalElementsToDelete(@NotNull final PsiElement element,
                                                              @NotNull final Collection<PsiElement> allElementsToDelete,
                                                              final boolean askUser) {
    if (!askUser || !(element instanceof PsiFile)) return Collections.emptyList();

    final VirtualFile file = ((PsiFile)element).getVirtualFile();
    if (file == null) return Collections.emptyList();

    final Collection<NestingTreeStructureProvider.ChildFileInfo> relatedFileInfos =
      NestingTreeStructureProvider.getFilesShownAsChildrenInProjectView(element.getProject(), file);

    final Collection<PsiElement> psiFiles = new ArrayList<>(relatedFileInfos.size());
    for (NestingTreeStructureProvider.ChildFileInfo info : relatedFileInfos) {
      final PsiFile psiFile = element.getManager().findFile(info.file);
      if (psiFile != null && !allElementsToDelete.contains(psiFile)) {
        psiFiles.add(psiFile);
      }
    }

    if (!psiFiles.isEmpty()) {
      final String message = psiFiles.size() == 1
                             ? RefactoringBundle.message("ask.to.delete.related.file", ((PsiFile)psiFiles.iterator().next()).getName())
                             : RefactoringBundle.message("ask.to.delete.related.files",
                                                         StringUtil.join(psiFiles, (psiFile) -> ((PsiFile)psiFile).getName(), ", "));
      final int ok =
        Messages.showYesNoDialog(element.getProject(), message, RefactoringBundle.message("delete.title"), Messages.getQuestionIcon());
      if (ok == Messages.YES) {
        return psiFiles;
      }
    }

    return Collections.emptyList();
  }

  @Override
  public NonCodeUsageSearchInfo findUsages(@NotNull PsiElement element,
                                           @NotNull PsiElement[] allElementsToDelete,
                                           @NotNull List<UsageInfo> result) {
    return null;
  }

  @Override
  public Collection<? extends PsiElement> getElementsToSearch(@NotNull PsiElement element,
                                                              @NotNull Collection<PsiElement> allElementsToDelete) {
    return Collections.singleton(element);
  }

  @Override
  public Collection<String> findConflicts(@NotNull PsiElement element, @NotNull PsiElement[] allElementsToDelete) {
    return Collections.emptyList();
  }

  @Override
  public UsageInfo[] preprocessUsages(Project project, final UsageInfo[] usages) {
    return usages;
  }

  @Override
  public void prepareForDeletion(PsiElement element) throws IncorrectOperationException {
  }

  @Override
  public boolean isToSearchInComments(final PsiElement element) {
    return RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_COMMENTS;
  }

  @Override
  public boolean isToSearchForTextOccurrences(final PsiElement element) {
    return RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_NON_JAVA;
  }

  @Override
  public void setToSearchInComments(final PsiElement element, boolean enabled) {
    RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_COMMENTS = enabled;
  }

  @Override
  public void setToSearchForTextOccurrences(final PsiElement element, boolean enabled) {
    RefactoringSettings.getInstance().SAFE_DELETE_SEARCH_IN_NON_JAVA = enabled;
  }
}
