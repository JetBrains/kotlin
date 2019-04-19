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
package com.intellij.refactoring.move.moveFilesOrDirectories;

import com.intellij.ide.projectView.impl.NestingTreeStructureProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.util.ArrayUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

public class MoveRelatedFilesHandler extends MoveFilesOrDirectoriesHandler {
  @Override
  public boolean canMove(PsiElement[] elements, PsiElement targetContainer) {
    if (!super.canMove(elements, targetContainer)) return false;

    for (PsiElement element : elements) {
      if (element instanceof PsiFile &&
          ((PsiFile)element).getVirtualFile() != null &&
          !NestingTreeStructureProvider.getFilesShownAsChildrenInProjectView(element.getProject(),
                                                                             ((PsiFile)element).getVirtualFile()).isEmpty()) {
        return true;
      }
    }

    return false;
  }

  @Nullable
  @Override
  public PsiElement[] adjustForMove(@NotNull final Project project,
                                    @NotNull PsiElement[] sourceElements,
                                    @Nullable final PsiElement targetElement) {
    sourceElements = super.adjustForMove(project, sourceElements, targetElement);
    if (sourceElements == null) return null;

    final Set<PsiFile> relatedFilesToMove = new THashSet<>();

    for (PsiElement element : sourceElements) {
      if (!(element instanceof PsiFile)) continue;

      final VirtualFile file = ((PsiFile)element).getVirtualFile();
      if (file == null) continue;

      final Collection<NestingTreeStructureProvider.ChildFileInfo> relatedFileInfos =
        NestingTreeStructureProvider.getFilesShownAsChildrenInProjectView(element.getProject(), file);

      for (NestingTreeStructureProvider.ChildFileInfo info : relatedFileInfos) {
        final PsiFile psiFile = element.getManager().findFile(info.file);
        if (psiFile != null && !ArrayUtil.contains(psiFile, sourceElements)) {
          relatedFilesToMove.add(psiFile);
        }
      }
    }

    if (!relatedFilesToMove.isEmpty()) {
      final String message = relatedFilesToMove.size() == 1
                             ? RefactoringBundle.message("ask.to.move.related.file", relatedFilesToMove.iterator().next().getName())
                             : RefactoringBundle.message("ask.to.move.related.files",
                                                         StringUtil.join(relatedFilesToMove, PsiFile::getName, ", "));
      final int ok = ApplicationManager.getApplication().isUnitTestMode()
                     ? Messages.YES
                     : Messages.showYesNoDialog(project, message, RefactoringBundle.message("move.title"), Messages.getQuestionIcon());
      if (ok == Messages.YES) {
        final PsiElement[] result = new PsiElement[sourceElements.length + relatedFilesToMove.size()];

        System.arraycopy(sourceElements, 0, result, 0, sourceElements.length);

        final Iterator<PsiFile> iterator = relatedFilesToMove.iterator();
        for (int i = sourceElements.length; i < result.length; i++) {
          result[i] = iterator.next();
        }

        return result;
      }
    }

    return sourceElements;
  }
}
