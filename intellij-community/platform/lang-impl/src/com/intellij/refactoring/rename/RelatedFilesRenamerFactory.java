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
package com.intellij.refactoring.rename;

import com.intellij.ide.projectView.impl.NestingTreeStructureProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * @see RelatedFilesRenamer
 */
public class RelatedFilesRenamerFactory implements AutomaticRenamerFactory {

  @Override
  public boolean isApplicable(@NotNull final PsiElement element) {
    return element instanceof PsiFile &&
           ((PsiFile)element).getVirtualFile() != null &&
           !NestingTreeStructureProvider.getFilesShownAsChildrenInProjectView(element.getProject(),
                                                                              ((PsiFile)element).getVirtualFile()).isEmpty();
  }

  @Nullable
  @Override
  public String getOptionName() {
    return "Rename related files (with the same name)";
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public void setEnabled(final boolean enabled) {
  }

  @NotNull
  @Override
  public AutomaticRenamer createRenamer(@NotNull final PsiElement element,
                                        @NotNull final String newName,
                                        @NotNull final Collection<UsageInfo> usages) {
    return new RelatedFilesRenamer((PsiFile)element, newName);
  }
}
