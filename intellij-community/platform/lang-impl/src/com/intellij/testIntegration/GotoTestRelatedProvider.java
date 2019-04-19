// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.testIntegration;

import com.intellij.navigation.GotoRelatedItem;
import com.intellij.navigation.GotoRelatedProvider;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public class GotoTestRelatedProvider extends GotoRelatedProvider {
  @NotNull
  @Override
  public List<? extends GotoRelatedItem> getItems(@NotNull DataContext context) {
    final PsiFile file = CommonDataKeys.PSI_FILE.getData(context);
    if (file == null) return Collections.emptyList();

    Collection<PsiElement> result;
    final boolean isTest = TestFinderHelper.isTest(file);
    if (isTest) {
      result = TestFinderHelper.findClassesForTest(file);
    }
    else {
      result = TestFinderHelper.findTestsForClass(file);
    }

    if (!result.isEmpty()) {
      final List<GotoRelatedItem> items = new ArrayList<>();
      for (PsiElement element : result) {
        items.add(new GotoRelatedItem(element, isTest ? "Tested classes" : "Tests"));
      }
      return items;
    }
    return Collections.emptyList();
  }
}