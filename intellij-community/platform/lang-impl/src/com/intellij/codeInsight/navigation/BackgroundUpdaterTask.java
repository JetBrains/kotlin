/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.codeInsight.navigation;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ListComponentUpdater;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageView;
import com.intellij.usages.impl.UsageViewImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public abstract class BackgroundUpdaterTask extends GenericBackgroundUpdaterTask<PsiElement> {
  private Ref<? extends UsageView> myUsageView;

  public BackgroundUpdaterTask(@Nullable Project project, @NotNull String title, @Nullable Comparator<PsiElement> comparator) {
    super(project, title, comparator);
  }

  public void init(@NotNull JBPopup popup, @NotNull ListComponentUpdater updater, @NotNull Ref<? extends UsageView> usageView) {
    myUsageView = usageView;
    super.init(popup, updater);
  }

  /**
   * @deprecated Use {@link #BackgroundUpdaterTask(Project, String, Comparator)} and {@link #updateComponent(PsiElement)} instead
   */
  @Deprecated
  @Override
  public boolean updateComponent(@NotNull PsiElement element, @Nullable Comparator comparator) {
    final UsageView view = myUsageView.get();
    if (view != null && !((UsageViewImpl)view).isDisposed()) {
      ApplicationManager.getApplication().runReadAction(() -> view.appendUsage(new UsageInfo2UsageAdapter(new UsageInfo(element))));
      return true;
    }

    return super.updateComponent(element, comparator);
  }

  @Override
  public boolean updateComponent(@NotNull PsiElement element) {
    final UsageView view = myUsageView.get();
    if (view != null && !((UsageViewImpl)view).isDisposed()) {
      ApplicationManager.getApplication().runReadAction(() -> view.appendUsage(new UsageInfo2UsageAdapter(new UsageInfo(element))));
      return true;
    }

    return super.updateComponent(element);
  }
}

