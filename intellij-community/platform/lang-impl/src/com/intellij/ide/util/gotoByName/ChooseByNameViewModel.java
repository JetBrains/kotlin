// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.util.gotoByName;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the state and settings of a "choose by name" popup from the point of view of the logic which fills it with items.
 *
 * @see DefaultChooseByNameItemProvider#filterElements(ChooseByNameViewModel, String, boolean, ProgressIndicator, PsiElement, Processor)
 * @author yole
 */
public interface ChooseByNameViewModel {
  Project getProject();

  @NotNull
  ChooseByNameModel getModel();

  /**
   * If true, the pattern entered in the dialog should be searched anywhere in the text of the candidate items, not just in the beginning.
   */
  boolean isSearchInAnyPlace();

  /**
   * Transforms text entered by the user in the dialog into the search pattern (for example, removes irrelevant suffixes like "line ...")
   */
  @NotNull
  String transformPattern(@NotNull String pattern);

  /**
   * If true, top matching candidates should be shown in the popup also when the entered pattern is empty. If false, an empty list is
   * displayed when the user has not entered any pattern.
   */
  boolean canShowListForEmptyPattern();

  /**
   * Returns the maximum number of candidates to show in the popup.
   */
  int getMaximumListSizeLimit();
}
