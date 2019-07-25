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

package com.intellij.usageView.impl;

import com.intellij.ide.hierarchy.*;
import com.intellij.ide.hierarchy.actions.BrowseHierarchyActionBase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.SimpleDataContext;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.*;
import com.intellij.usages.impl.UsageContextPanelBase;
import com.intellij.usages.impl.UsageViewImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class UsageContextCallHierarchyPanel extends UsageContextPanelBase {
  private HierarchyBrowser myBrowser;

  public static class Provider implements UsageContextPanel.Provider {
    @NotNull
    @Override
    public UsageContextPanel create(@NotNull UsageView usageView) {
      return new UsageContextCallHierarchyPanel(((UsageViewImpl)usageView).getProject(), usageView.getPresentation());
    }

    @Override
    public boolean isAvailableFor(@NotNull UsageView usageView) {
      UsageTarget[] targets = ((UsageViewImpl)usageView).getTargets();
      if (targets.length == 0) return false;
      UsageTarget target = targets[0];
      if (!(target instanceof PsiElementUsageTarget)) return false;
      PsiElement element = ((PsiElementUsageTarget)target).getElement();
      if (element == null || !element.isValid()) return false;

      Project project = element.getProject();
      DataContext context = SimpleDataContext.getSimpleContext(CommonDataKeys.PSI_ELEMENT.getName(), element,
                                                               SimpleDataContext.getProjectContext(project));
      HierarchyProvider provider = BrowseHierarchyActionBase.findBestHierarchyProvider(LanguageCallHierarchy.INSTANCE, element, context);
      if (provider == null) return false;
      PsiElement providerTarget = provider.getTarget(context);
      return providerTarget != null;
    }
    @NotNull
    @Override
    public String getTabTitle() {
      return "Call Hierarchy";
    }
  }

  public UsageContextCallHierarchyPanel(@NotNull Project project, @NotNull UsageViewPresentation presentation) {
    super(project, presentation);
  }

  @Override
  public void dispose() {
    super.dispose();
    myBrowser = null;
  }

  @Override
  public void updateLayoutLater(@Nullable final List<? extends UsageInfo> infos) {
    PsiElement element = infos == null ? null : getElementToSliceOn(infos);
    if (myBrowser instanceof Disposable) {
      Disposer.dispose((Disposable)myBrowser);
      myBrowser = null;
    }
    if (element != null) {
      myBrowser = createCallHierarchyPanel(element);
      if (myBrowser == null) {
        element = null;
      }
    }

    removeAll();
    if (element == null) {
      JComponent titleComp = new JLabel(UsageViewBundle.message("select.the.usage.to.preview", myPresentation.getUsagesWord()), SwingConstants.CENTER);
      add(titleComp, BorderLayout.CENTER);
    }
    else {
      if (myBrowser instanceof Disposable) {
        Disposer.register(this, (Disposable)myBrowser);
      }
      JComponent panel = myBrowser.getComponent();
      add(panel, BorderLayout.CENTER);
    }
    revalidate();
  }

  @Nullable
  private static HierarchyBrowser createCallHierarchyPanel(@NotNull PsiElement element) {
    DataContext context = SimpleDataContext.getSimpleContext(CommonDataKeys.PSI_ELEMENT.getName(), element, SimpleDataContext.getProjectContext(element.getProject()));
    HierarchyProvider provider = BrowseHierarchyActionBase.findBestHierarchyProvider(LanguageCallHierarchy.INSTANCE, element, context);
    if (provider == null) return null;
    PsiElement providerTarget = provider.getTarget(context);
    if (providerTarget == null) return null;

    HierarchyBrowser browser = provider.createHierarchyBrowser(providerTarget);
    if (browser instanceof HierarchyBrowserBaseEx) {
      HierarchyBrowserBaseEx browserEx = (HierarchyBrowserBaseEx)browser;
      // do not steal focus when scrolling through nodes
      browserEx.changeView(CallHierarchyBrowserBase.CALLER_TYPE, false);
    }
    return browser;
  }

  private static PsiElement getElementToSliceOn(@NotNull List<? extends UsageInfo> infos) {
    UsageInfo info = infos.get(0);
    return info.getElement();
  }
}
