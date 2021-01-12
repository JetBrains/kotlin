// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions.searcheverywhere.statistics;

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class SearchEverywhereUsageTriggerCollector {

  // this string will be used as ID for contributors from private
  // plugins that mustn't be sent in statistics
  private static final String NOT_REPORTABLE_CONTRIBUTOR_ID = "third.party";

  public static final String DIALOG_OPEN = "dialogOpen";
  public static final String DIALOG_CLOSED = "dialogClosed";
  public static final String TAB_SWITCHED = "tabSwitched";
  public static final String GROUP_NAVIGATE = "navigateThroughGroups";
  public static final String CONTRIBUTOR_ITEM_SELECTED = "contributorItemChosen";
  public static final String MORE_ITEM_SELECTED = "moreItemChosen";
  public static final String COMMAND_USED = "commandUsed";
  public static final String COMMAND_COMPLETED = "commandCompleted";
  public static final String SESSION_FINISHED = "sessionFinished";

  public static final String CONTRIBUTOR_ID_FIELD = "contributorID";
  public static final String CURRENT_TAB_FIELD = "currentTabId";
  public static final String SELECTED_ITEM_NUMBER = "selectedItemNumber";
  public static final String TYPED_SYMBOL_KEYS = "typedSymbolKeys";
  public static final String TYPED_NAVIGATION_KEYS = "typedNavigationKeys";

  public static void trigger(@Nullable Project project, @NotNull String feature) {
    trigger(project, feature, new FeatureUsageData());
  }

  public static void trigger(@Nullable Project project, @NotNull String feature, @NotNull FeatureUsageData data) {
    FUCounterUsageLogger.getInstance().logEvent(project, "searchEverywhere", feature, data);
  }

  @NotNull
  public static FeatureUsageData createData(@Nullable String contributorID) {
    FeatureUsageData res = new FeatureUsageData();
    if (contributorID != null) {
      res.addData(CONTRIBUTOR_ID_FIELD, contributorID);
    }

    return res;
  }

  public static FeatureUsageData createData(@Nullable String contributorID, @Nullable String currentTab, int itemNumber) {
    FeatureUsageData res = createData(contributorID);

    if (currentTab != null) {
      res.addData(CURRENT_TAB_FIELD, currentTab);
    }

    res.addData(SELECTED_ITEM_NUMBER, itemNumber);

    return res;
  }

  @NotNull
  public static String getReportableContributorID(@NotNull SearchEverywhereContributor<?> contributor) {
    Class<? extends SearchEverywhereContributor> clazz = contributor.getClass();
    PluginInfo pluginInfo = PluginInfoDetectorKt.getPluginInfo(clazz);
    return pluginInfo.isDevelopedByJetBrains() ? contributor.getSearchProviderId() : NOT_REPORTABLE_CONTRIBUTOR_ID;
  }
}
