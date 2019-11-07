// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

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
import com.intellij.util.ArrayUtilRt;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Set;

public class RenameViewDescriptor implements UsageViewDescriptor{
  private static final Logger LOG = Logger.getInstance(RenameViewDescriptor.class);
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


    myProcessedElementsHeader = StringUtil.join(ArrayUtilRt.toStringArray(processedElementsHeaders), ", ");
    myCodeReferencesText =  RefactoringBundle.message("references.in.code.to.0", StringUtil.join(ArrayUtilRt.toStringArray(codeReferences),
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