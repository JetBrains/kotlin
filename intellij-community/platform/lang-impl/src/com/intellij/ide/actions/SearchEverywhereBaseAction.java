// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.featureStatistics.FeatureUsageTracker;
import com.intellij.ide.IdeEventQueue;
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereManager;
import com.intellij.ide.actions.searcheverywhere.statistics.SearchEverywhereUsageTriggerCollector;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import static com.intellij.ide.actions.GotoActionBase.getInitialText;

public abstract class SearchEverywhereBaseAction extends AnAction {

  @Override
  public void update(@NotNull final AnActionEvent event) {
    final Presentation presentation = event.getPresentation();
    final DataContext dataContext = event.getDataContext();
    final Project project = CommonDataKeys.PROJECT.getData(dataContext);
    boolean hasContributors = hasContributors(dataContext);
    presentation.setEnabled((!requiresProject() || project != null) && hasContributors);
    presentation.setVisible(hasContributors);
  }

  protected boolean requiresProject() {
    return true;
  }

  protected boolean hasContributors(DataContext context){
    return true;
  }

  protected void showInSearchEverywherePopup(@NotNull String searchProviderID,
                                             @NotNull AnActionEvent event,
                                             boolean useEditorSelection,
                                             boolean sendStatistics) {
    Project project = event.getProject();
    SearchEverywhereManager seManager = SearchEverywhereManager.getInstance(project);
    FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_SEARCH_EVERYWHERE);

    if (seManager.isShown()) {
      if (searchProviderID.equals(seManager.getSelectedContributorID())) {
        seManager.toggleEverywhereFilter();
      }
      else {
        seManager.setSelectedContributor(searchProviderID);
        if (sendStatistics) {
          FeatureUsageData data = SearchEverywhereUsageTriggerCollector
            .createData(searchProviderID)
            .addInputEvent(event);
          SearchEverywhereUsageTriggerCollector.trigger(project, SearchEverywhereUsageTriggerCollector.TAB_SWITCHED, data);
        }
      }
      return;
    }

    if (sendStatistics) {
      FeatureUsageData data = SearchEverywhereUsageTriggerCollector.createData(searchProviderID).addInputEvent(event);
      SearchEverywhereUsageTriggerCollector.trigger(project, SearchEverywhereUsageTriggerCollector.DIALOG_OPEN, data);
    }
    IdeEventQueue.getInstance().getPopupManager().closeAllPopups(false);
    String searchText = StringUtil.nullize(getInitialText(useEditorSelection, event).first);
    seManager.show(searchProviderID, searchText, event);
  }
}
