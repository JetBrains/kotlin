/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.usageView.UsageViewUtil;
import org.jetbrains.annotations.NotNull;

class MoveFilesOrDirectoriesViewDescriptor implements UsageViewDescriptor {
  private final PsiElement[] myElementsToMove;
  private String myProcessedElementsHeader;
  private final String myCodeReferencesText;

  MoveFilesOrDirectoriesViewDescriptor(PsiElement[] elementsToMove, PsiDirectory newParent) {
    myElementsToMove = elementsToMove;
    if (elementsToMove.length == 1) {
      myProcessedElementsHeader = StringUtil.capitalize(RefactoringBundle.message("move.single.element.elements.header",
                                                                                  UsageViewUtil.getType(elementsToMove[0]),
                                                                                  newParent.getVirtualFile().getPresentableUrl()));
      myCodeReferencesText = RefactoringBundle.message("references.in.code.to.0.1",
                                                       UsageViewUtil.getType(elementsToMove[0]), UsageViewUtil.getLongName(elementsToMove[0]));
    }
    else {
      if (elementsToMove[0] instanceof PsiFile) {
        myProcessedElementsHeader =
          StringUtil.capitalize(RefactoringBundle.message("move.files.elements.header", newParent.getVirtualFile().getPresentableUrl()));
      }
      else if (elementsToMove[0] instanceof PsiDirectory){
        myProcessedElementsHeader = StringUtil
          .capitalize(RefactoringBundle.message("move.directories.elements.header", newParent.getVirtualFile().getPresentableUrl()));
      }
      myCodeReferencesText = RefactoringBundle.message("references.found.in.code");
    }
  }

  @Override
  @NotNull
  public PsiElement[] getElements() {
    return myElementsToMove;
  }

  @Override
  public String getProcessedElementsHeader() {
    return myProcessedElementsHeader;
  }

  @NotNull
  @Override
  public String getCodeReferencesText(int usagesCount, int filesCount) {
    return myCodeReferencesText + UsageViewBundle.getReferencesString(usagesCount, filesCount);
  }

  @Override
  public String getCommentReferencesText(int usagesCount, int filesCount) {
    return RefactoringBundle.message("comments.elements.header",
                                     UsageViewBundle.getOccurencesString(usagesCount, filesCount));
  }

}
