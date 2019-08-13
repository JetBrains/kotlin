/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.find.findUsages;

import com.intellij.find.FindManager;
import com.intellij.find.FindSettings;
import com.intellij.find.impl.FindManagerImpl;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usages.UsageInfoToUsageConverter;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

class PsiElement2UsageTargetComposite extends PsiElement2UsageTargetAdapter {
  private final UsageInfoToUsageConverter.TargetElementsDescriptor myDescriptor;
  PsiElement2UsageTargetComposite(@NotNull PsiElement[] primaryElements,
                                  @NotNull PsiElement[] secondaryElements,
                                  @NotNull FindUsagesOptions options) {
    super(primaryElements[0], options);
    myDescriptor = new UsageInfoToUsageConverter.TargetElementsDescriptor(primaryElements, secondaryElements);
  }

  @Override
  public void findUsages() {
    PsiElement element = getElement();
    if (element == null) return;
    FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(element.getProject())).getFindUsagesManager();
    FindUsagesHandler handler = findUsagesManager.getFindUsagesHandler(element, false);
    boolean skipResultsWithOneUsage = FindSettings.getInstance().isSkipResultsWithOneUsage();
    findUsagesManager.findUsages(myDescriptor.getPrimaryElements(), myDescriptor.getAdditionalElements(), handler, myOptions, skipResultsWithOneUsage);
  }

  @Override
  public VirtualFile[] getFiles() {
    Set<VirtualFile> files = ContainerUtil.map2Set(myDescriptor.getAllElements(), element -> PsiUtilCore.getVirtualFile(element));
    return VfsUtilCore.toVirtualFileArray(files);
  }

  @NotNull
  public PsiElement[] getPrimaryElements() {
    return myDescriptor.getPrimaryElements();
  }
  @NotNull
  public PsiElement[] getSecondaryElements() {
    return myDescriptor.getAdditionalElements();
  }
}
