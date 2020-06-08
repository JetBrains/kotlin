// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.packageDependencies;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class FindDependencyUtil {
  private FindDependencyUtil() {}

  public static UsageInfo[] findDependencies(@Nullable final List<? extends DependenciesBuilder> builders, Set<? extends PsiFile> searchIn, Set<? extends PsiFile> searchFor) {
    final List<UsageInfo> usages = new ArrayList<>();
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    int totalCount = searchIn.size();
    int count = 0;

    for (final PsiFile psiFile : searchIn) {
      count = updateIndicator(indicator, totalCount, count, psiFile);

      if (!psiFile.isValid()) continue;

      final Set<PsiFile> precomputedDeps;
      if (builders != null) {
        final Set<PsiFile> depsByFile = new HashSet<>();
        for (DependenciesBuilder builder : builders) {
          final Set<PsiFile> deps = builder.getDependencies().get(psiFile);
          if (deps != null) {
            depsByFile.addAll(deps);
          }
        }
        precomputedDeps = new HashSet<>(depsByFile);
        precomputedDeps.retainAll(searchFor);
        if (precomputedDeps.isEmpty()) continue;
      }
      else {
        precomputedDeps = Collections.unmodifiableSet(searchFor);
      }

      analyzeFileDependencies(psiFile, precomputedDeps, usages);
    }

    return usages.toArray(UsageInfo.EMPTY_ARRAY);
  }

  public static UsageInfo[] findBackwardDependencies(final List<? extends DependenciesBuilder> builders, final Set<? extends PsiFile> searchIn, final Set<? extends PsiFile> searchFor) {
    final List<UsageInfo> usages = new ArrayList<>();
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();


    final Set<PsiFile> deps = new HashSet<>();
    for (PsiFile psiFile : searchFor) {
      for (DependenciesBuilder builder : builders) {
        final Set<PsiFile> depsByBuilder = builder.getDependencies().get(psiFile);
        if (depsByBuilder != null) {
          deps.addAll(depsByBuilder);
        }
      }
    }
    deps.retainAll(searchIn);
    if (deps.isEmpty()) return UsageInfo.EMPTY_ARRAY;

    int totalCount = deps.size();
    int count = 0;
    for (final PsiFile psiFile : deps) {
      count = updateIndicator(indicator, totalCount, count, psiFile);

      analyzeFileDependencies(psiFile, searchFor, usages);
    }

    return usages.toArray(UsageInfo.EMPTY_ARRAY);
  }

  private static void analyzeFileDependencies(PsiFile psiFile, final Set<? extends PsiFile> searchFor, final List<? super UsageInfo> result) {
    DependenciesBuilder.analyzeFileDependencies(psiFile, (place, dependency) -> {
      PsiFile dependencyFile = dependency.getContainingFile();
      if (dependencyFile != null) {
        final PsiElement navigationElement = dependencyFile.getNavigationElement();
        if (navigationElement instanceof PsiFile) {
          dependencyFile = (PsiFile)navigationElement;
        }
      }
      if (searchFor.contains(dependencyFile)) {
        result.add(new UsageInfo(place));
      }
    });
  }

  private static int updateIndicator(final ProgressIndicator indicator, final int totalCount, int count, final PsiFile psiFile) {
    if (indicator != null) {
      ProgressIndicatorUtils.checkCancelledEvenWithPCEDisabled(indicator);
      indicator.setFraction(((double)++count) / totalCount);
      final VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null) {
        indicator.setText(CodeInsightBundle.message("find.dependencies.progress.text", virtualFile.getPresentableUrl()));
      }
    }
    return count;
  }

  public static UsageInfo[] findDependencies(final DependenciesBuilder builder, final Set<? extends PsiFile> searchIn, final Set<? extends PsiFile> searchFor) {
    return findDependencies(Collections.singletonList(builder), searchIn, searchFor);
  }

  public static UsageInfo[] findBackwardDependencies(final DependenciesBuilder builder, final Set<? extends PsiFile> searchIn, final Set<? extends PsiFile> searchFor) {
    return findBackwardDependencies(Collections.singletonList(builder), searchIn, searchFor);
  }
}