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

package com.intellij.packageDependencies;

import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class FindDependencyUtil {
  private FindDependencyUtil() {}

  public static UsageInfo[] findDependencies(@Nullable final List<? extends DependenciesBuilder> builders, Set<? extends PsiFile> searchIn, Set<? extends PsiFile> searchFor) {
    final List<UsageInfo> usages = new ArrayList<>();
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    int totalCount = searchIn.size();
    int count = 0;

    nextFile: for (final PsiFile psiFile : searchIn) {
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
        if (precomputedDeps.isEmpty()) continue nextFile;
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
    DependenciesBuilder.analyzeFileDependencies(psiFile, new DependenciesBuilder.DependencyProcessor() {
      @Override
      public void process(PsiElement place, PsiElement dependency) {
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
      }
    });
  }

  private static int updateIndicator(final ProgressIndicator indicator, final int totalCount, int count, final PsiFile psiFile) {
    if (indicator != null) {
      if (indicator.isCanceled()) throw new ProcessCanceledException();
      indicator.setFraction(((double)++count) / totalCount);
      final VirtualFile virtualFile = psiFile.getVirtualFile();
      if (virtualFile != null) {
        indicator.setText(AnalysisScopeBundle.message("find.dependencies.progress.text", virtualFile.getPresentableUrl()));
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