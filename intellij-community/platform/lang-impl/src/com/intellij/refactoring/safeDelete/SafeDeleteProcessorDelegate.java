/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/**
 * @author yole
 */
public interface SafeDeleteProcessorDelegate {
  ExtensionPointName<SafeDeleteProcessorDelegate> EP_NAME = ExtensionPointName.create("com.intellij.refactoring.safeDeleteProcessor");

  boolean handlesElement(PsiElement element);
  @Nullable
  NonCodeUsageSearchInfo findUsages(@NotNull PsiElement element, @NotNull PsiElement[] allElementsToDelete, @NotNull List<UsageInfo> result);

  /**
   * Called before the refactoring dialog is shown. Returns the list of elements for which the
   * usages should be searched for the specified element selected by the user for deletion.
   * May show UI to ask the user if some additional elements should be deleted along with the
   * specified selected element.
   *
   * @param element an element selected for deletion.
   * @param allElementsToDelete all elements selected for deletion.
   * @return additional elements to search for usages, or null if the user has cancelled the refactoring.
   */
  @Nullable
  Collection<? extends PsiElement> getElementsToSearch(@NotNull PsiElement element, @NotNull Collection<PsiElement> allElementsToDelete);

  @Nullable
  Collection<PsiElement> getAdditionalElementsToDelete(@NotNull PsiElement element, @NotNull Collection<PsiElement> allElementsToDelete, final boolean askUser);

  @Nullable
  Collection<String> findConflicts(@NotNull PsiElement element, @NotNull PsiElement[] allElementsToDelete);

  /**
   * Called after the user has confirmed the refactoring. Can filter out some of the usages
   * found by the refactoring. May show UI to ask the user if some of the usages should
   * be excluded.
   *
   * @param project the project where the refactoring happens.
   * @param usages all usages to be processed by the refactoring. 
   * @return the filtered list of usages, or null if the user has cancelled the refactoring.
   */
  @Nullable
  UsageInfo[] preprocessUsages(Project project, UsageInfo[] usages);

  void prepareForDeletion(PsiElement element) throws IncorrectOperationException;

  boolean isToSearchInComments(final PsiElement element);

  void setToSearchInComments(final PsiElement element, boolean enabled);

  boolean isToSearchForTextOccurrences(final PsiElement element);

  void setToSearchForTextOccurrences(final PsiElement element, boolean enabled);
}
