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

package com.intellij.find.impl;

import com.intellij.find.FindBundle;
import com.intellij.find.FindManager;
import com.intellij.find.findUsages.FindUsagesManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.usages.ConfigurableUsageTarget;
import com.intellij.usages.UsageView;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShowRecentFindUsagesAction extends AnAction {
  @Override
  public void update(@NotNull final AnActionEvent e) {
    UsageView usageView = e.getData(UsageView.USAGE_VIEW_KEY);
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (usageView != null
        && project != null
        && ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager().getHistory().getAll().size() > 1) {
      e.getPresentation().setEnabled(true);
      return;
    }
    e.getPresentation().setEnabled(false);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    UsageView usageView = e.getData(UsageView.USAGE_VIEW_KEY);
    Project project = e.getData(CommonDataKeys.PROJECT);
    final FindUsagesManager findUsagesManager = ((FindManagerImpl)FindManager.getInstance(project)).getFindUsagesManager();
    List<ConfigurableUsageTarget> history = new ArrayList<>(findUsagesManager.getHistory().getAll());

    if (!history.isEmpty()) {
      // skip most recent find usage, it's under your nose
      history.remove(history.size() - 1);
      Collections.reverse(history);
    }
    if (history.isEmpty()) {
      history.add(null); // to fill the popup
    }

    BaseListPopupStep<ConfigurableUsageTarget> step =
      new BaseListPopupStep<ConfigurableUsageTarget>(FindBundle.message("recent.find.usages.action.title"), history) {
        @Override
        public Icon getIconFor(final ConfigurableUsageTarget data) {
          return null;
        }

        @Override
        @NotNull
        public String getTextFor(final ConfigurableUsageTarget data) {
          if (data == null) {
            return FindBundle.message("recent.find.usages.action.nothing");
          }
          return data.getLongDescriptiveName();
        }

        @Override
        public PopupStep onChosen(final ConfigurableUsageTarget selectedValue, final boolean finalChoice) {
          return doFinalStep(() -> {
            if (selectedValue != null) {
              findUsagesManager.rerunAndRecallFromHistory(selectedValue);
            }
          });
        }
      };
    JBPopupFactory.getInstance().createListPopup(step).showInCenterOf(usageView.getPreferredFocusableComponent());
  }
}
