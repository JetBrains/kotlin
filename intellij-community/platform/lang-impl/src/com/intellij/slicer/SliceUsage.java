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
package com.intellij.slicer;

import com.intellij.analysis.AnalysisScope;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.util.CommonProcessors;
import com.intellij.util.Processor;
import gnu.trove.TObjectHashingStrategy;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author cdr
 */
public abstract class SliceUsage extends UsageInfo2UsageAdapter {
  private final SliceUsage myParent;
  public final SliceAnalysisParams params;

  public SliceUsage(@NotNull PsiElement element, @NotNull SliceUsage parent) {
    super(new UsageInfo(element));
    myParent = parent;
    params = parent.params;
    assert params != null;
  }

  // root usage
  protected SliceUsage(@NotNull PsiElement element, @NotNull SliceAnalysisParams params) {
    super(new UsageInfo(element));
    myParent = null;
    this.params = params;
  }

  @NotNull
  private static Collection<SliceUsage> transformToLanguageSpecificUsage(@NotNull SliceUsage usage) {
    PsiElement element = usage.getElement();
    if (element == null) return Collections.singletonList(usage);
    SliceLanguageSupportProvider provider = LanguageSlicing.getProvider(element);
    if (!(provider instanceof SliceUsageTransformer)) return Collections.singletonList(usage);
    Collection<SliceUsage> transformedUsages = ((SliceUsageTransformer)provider).transform(usage);
    return transformedUsages != null ? transformedUsages : Collections.singletonList(usage);
  }

  public void processChildren(@NotNull Processor<SliceUsage> processor) {
    final PsiElement element = ReadAction.compute(this::getElement);
    ProgressIndicator indicator = ProgressManager.getInstance().getProgressIndicator();
    indicator.checkCanceled();

    final Processor<SliceUsage> uniqueProcessor =
      new CommonProcessors.UniqueProcessor<SliceUsage>(processor, new TObjectHashingStrategy<SliceUsage>() {
        @Override
        public int computeHashCode(final SliceUsage object) {
          return object.getUsageInfo().hashCode();
        }

        @Override
        public boolean equals(final SliceUsage o1, final SliceUsage o2) {
          return o1.getUsageInfo().equals(o2.getUsageInfo());
        }
      }) {
        @Override
        public boolean process(SliceUsage usage) {
          return transformToLanguageSpecificUsage(usage).stream().allMatch(super::process);
        }
      };

    ApplicationManager.getApplication().runReadAction(() -> {
      if (params.dataFlowToThis) {
        processUsagesFlownDownTo(element, uniqueProcessor);
      }
      else {
        processUsagesFlownFromThe(element, uniqueProcessor);
      }
    });
  }

  protected abstract void processUsagesFlownFromThe(PsiElement element, Processor<SliceUsage> uniqueProcessor);

  protected abstract void processUsagesFlownDownTo(PsiElement element, Processor<SliceUsage> uniqueProcessor);

  public SliceUsage getParent() {
    return myParent;
  }

  @NotNull
  public AnalysisScope getScope() {
    return params.scope;
  }

  @NotNull
  protected abstract SliceUsage copy();

  public boolean canBeLeaf() {
    return getElement() != null;
  }
}
