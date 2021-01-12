// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.IntentionActionDelegate;
import com.intellij.codeInsight.intention.impl.IntentionActionWithTextCaching;
import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ex.QuickFixWrapper;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.ListPopup;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Konstantin Bulenkov
 */
public final class IntentionsCollector {
  public static void record(@NotNull Project project, @NotNull IntentionAction action, @NotNull Language language) {
    recordIntentionEvent(project, action, language, "called");
  }

  protected static void recordIntentionEvent(@NotNull Project project, @NotNull IntentionAction action, @NotNull Language language, @NonNls String eventId) {
    final Class<?> clazz = getOriginalHandlerClass(action);
    final PluginInfo info = PluginInfoDetectorKt.getPluginInfo(clazz);

    final FeatureUsageData data = new FeatureUsageData().
      addData("id", clazz.getName()).
      addPluginInfo(info).
      addLanguage(language);
    FUCounterUsageLogger.getInstance().logEvent(project, "intentions", eventId, data);
  }

  @NotNull
  private static Class<?> getOriginalHandlerClass(@NotNull IntentionAction action) {
    Object handler = action;
    if (action instanceof IntentionActionDelegate) {
      IntentionAction delegate = ((IntentionActionDelegate)action).getDelegate();
      if (delegate != action) {
        return getOriginalHandlerClass(delegate);
      }
    }
    else if (action instanceof QuickFixWrapper) {
      LocalQuickFix fix = ((QuickFixWrapper)action).getFix();
      if (fix != action) {
        handler = fix;
      }
    }
    return handler.getClass();
  }

  public static void reportShownIntentions(@NotNull Project project,
                                           ListPopup popup,
                                           @NotNull Language language) {
    @SuppressWarnings("unchecked") List<IntentionActionWithTextCaching> values = popup.getListStep().getValues();
    for (IntentionActionWithTextCaching value : values) {
      recordIntentionEvent(project, value.getAction(), language, "shown");
    }
  }
}

