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

package com.intellij.refactoring.rename;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.refactoring.RefactoringBundle;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usageView.UsageViewDescriptor;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.ArrayUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Set;

public class RenameViewDescriptor implements UsageViewDescriptor{
  private static final Logger LOG = Logger.getInstance("#com.intellij.refactoring.rename.RenameViewDescriptor");
  private final String myProcessedElementsHeader;
  private final String myCodeReferencesText;
  private final PsiElement[] myElements;

  public RenameViewDescriptor(LinkedHashMap<PsiElement, String> renamesMap) {

    myElements = PsiUtilCore.toPsiElementArray(renamesMap.keySet());

    Set<String> processedElementsHeaders = new THashSet<>();
    Set<String> codeReferences = new THashSet<>();

    for (final PsiElement element : myElements) {
      PsiUtilCore.ensureValid(element);
      String newName = renamesMap.get(element);

      String prefix = "";
      if (element instanceof PsiDirectory/* || element instanceof PsiClass*/) {
        String fullName = UsageViewUtil.getLongName(element);
        int lastDot = fullName.lastIndexOf('.');
        if (lastDot >= 0 && 
            lastDot + 1 < fullName.length() && ((PsiDirectory)element).getName().equals(fullName.substring(lastDot + 1))) {
          prefix = fullName.substring(0, lastDot + 1);
        }
      }

      processedElementsHeaders.add(StringUtil.capitalize(
        RefactoringBundle.message("0.to.be.renamed.to.1.2", UsageViewUtil.getType(element), prefix, newName)));
      codeReferences.add(UsageViewUtil.getType(element) + " " + UsageViewUtil.getLongName(element));
    }


    myProcessedElementsHeader = StringUtil.join(ArrayUtil.toStringArray(processedElementsHeaders), ", ");
    myCodeReferencesText =  RefactoringBundle.message("references.in.code.to.0", StringUtil.join(ArrayUtil.toStringArray(codeReferences),
                                                                                                 ", "));
  }

  @Override
  @NotNull
  public PsiElement[] getElements() {
    return myElements;
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