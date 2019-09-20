// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

package com.intellij.ide.actions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.*;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.util.ui.EmptyIcon;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.*;

public class SelectInAction extends AnAction implements DumbAware {
  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.select.in");
    SelectInContext context = SelectInContextImpl.createContext(e);
    if (context == null) return;
    invoke(e.getDataContext(), context);
  }

  @Override
  public void beforeActionPerformedUpdate(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project != null) {
      PsiDocumentManager.getInstance(project).commitAllDocuments();
    }
    super.beforeActionPerformedUpdate(e);
  }

  @Override
  public void update(@NotNull AnActionEvent event) {
    Presentation presentation = event.getPresentation();

    if (SelectInContextImpl.createContext(event) == null) {
      presentation.setEnabledAndVisible(false);
    }
    else {
      presentation.setEnabledAndVisible(true);
    }
  }

  private static void invoke(@NotNull DataContext dataContext, @NotNull SelectInContext context) {
    final List<SelectInTarget> targetVector = Arrays.asList(getSelectInManager(context.getProject()).getTargets());
    ListPopup popup;
    if (targetVector.isEmpty()) {
      DefaultActionGroup group = new DefaultActionGroup();
      group.add(new NoTargetsAction());
      popup = JBPopupFactory.getInstance().createActionGroupPopup(IdeBundle.message("title.popup.select.target"), group, dataContext,
                                                                  JBPopupFactory.ActionSelectionAid.MNEMONICS, true);
    }
    else {
      popup = JBPopupFactory.getInstance().createListPopup(new SelectInActionsStep(targetVector, context));
    }

    popup.showInBestPositionFor(dataContext);
  }

  private static class SelectInActionsStep extends BaseListPopupStep<SelectInTarget> {
    private final SelectInContext mySelectInContext;
    private final List<SelectInTarget> myVisibleTargets;

    SelectInActionsStep(@NotNull final Collection<SelectInTarget> targetVector, @NotNull SelectInContext selectInContext) {
      mySelectInContext = selectInContext;
      myVisibleTargets = new ArrayList<>(targetVector);
      List<Icon> icons = fillInIcons(targetVector, selectInContext);
      init(IdeBundle.message("title.popup.select.target"), myVisibleTargets, icons);
    }

    @NotNull
    private static List<Icon> fillInIcons(@NotNull Collection<? extends SelectInTarget> targets, @NotNull SelectInContext selectInContext) {
      ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(selectInContext.getProject());
      List<Icon> list = new ArrayList<>();
      for (SelectInTarget target : targets) {
        String id = target.getMinorViewId() != null ? null : target.getToolWindowId();
        ToolWindow toolWindow = toolWindowManager.getToolWindow(id);
        Icon icon = toolWindow != null ? toolWindow.getIcon() : EmptyIcon.ICON_13;
        list.add(icon);
      }
      return list;
    }

    @Override
    @NotNull
    public String getTextFor(final SelectInTarget value) {
      String text = value.toString();
      String id = value.getMinorViewId() == null ? value.getToolWindowId() : null;
      ToolWindow toolWindow = id == null ? null : ToolWindowManager.getInstance(mySelectInContext.getProject()).getToolWindow(id);
      if (toolWindow != null) {
        text = text.replace(value.getToolWindowId(), toolWindow.getStripeTitle());
      }
      int n = myVisibleTargets.indexOf(value);
      return numberingText(n, text);
    }

    @Override
    public PopupStep onChosen(final SelectInTarget target, final boolean finalChoice) {
      if (finalChoice) {
        PsiDocumentManager.getInstance(mySelectInContext.getProject()).commitAllDocuments();
        target.selectIn(mySelectInContext, true);
        return FINAL_CHOICE;
      }
      if (target instanceof CompositeSelectInTarget) {
        final ArrayList<SelectInTarget> subTargets = new ArrayList<>(((CompositeSelectInTarget)target).getSubTargets(mySelectInContext));
        if (subTargets.size() > 0) {
          Collections.sort(subTargets, new SelectInManager.SelectInTargetComparator());
          return new SelectInActionsStep(subTargets, mySelectInContext);
        }
      }
      return FINAL_CHOICE;
    }

    @Override
    public boolean hasSubstep(final SelectInTarget selectedValue) {
      return selectedValue instanceof CompositeSelectInTarget &&
             ((CompositeSelectInTarget)selectedValue).getSubTargets(mySelectInContext).size() > 1;
    }

    @Override
    public boolean isSelectable(final SelectInTarget target) {
      if (DumbService.isDumb(mySelectInContext.getProject()) && !DumbService.isDumbAware(target)) {
        return false;
      }
      return target.canSelect(mySelectInContext);
    }

    @Override
    public boolean isMnemonicsNavigationEnabled() {
      return true;
    }
  }

  private static String numberingText(final int n, String text) {
    if (n < 9) {
      text = "&" + (n + 1) + ". " + text;
    }
    else if (n == 9) {
      text = "&" + 0 + ". " + text;
    }
    else {
      text = "&" + (char)('A' + n - 10) + ". " + text;
    }
    return text;
  }

  private static SelectInManager getSelectInManager(Project project) {
    return SelectInManager.getInstance(project);
  }

  private static class NoTargetsAction extends AnAction {
    NoTargetsAction() {
      super(IdeBundle.message("message.no.targets.available"));
    }

    @Override
    public void actionPerformed(@NotNull AnActionEvent e) {
    }
  }
}