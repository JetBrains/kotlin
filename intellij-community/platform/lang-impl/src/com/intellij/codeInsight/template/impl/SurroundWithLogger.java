// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.lang.Language;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class SurroundWithLogger {
  private final static String USAGE_GROUP = "surround.with";

  public static void logSurrounder(Surrounder surrounder, @NotNull Language language, @NotNull Project project) {
    log("surrounder", surrounder.getClass(), language, project);
  }

  private static void log(@NotNull String type,
                          @NotNull Class<?> elementClass,
                          @NotNull Language language,
                          @NotNull Project project) {
    PluginInfo pluginInfo = PluginInfoDetectorKt.getPluginInfo(elementClass);
    FeatureUsageData data = new FeatureUsageData().addPluginInfo(pluginInfo).addLanguage(language);
    data.addData("class", pluginInfo.getType().isDevelopedByJetBrains() ? elementClass.getName() : "third.party");
    FUCounterUsageLogger.getInstance().logEvent(project, USAGE_GROUP, type + ".executed", data);
  }

  static void logTemplate(@NotNull TemplateImpl template, @NotNull Language language, @NotNull Project project) {
    final FeatureUsageData data = LiveTemplateRunLogger.createTemplateData(template, language);
    if (data != null) {
      FUCounterUsageLogger.getInstance().logEvent(project, USAGE_GROUP, "live.template.executed", data);
    }
  }

  static void logCustomTemplate(@NotNull CustomLiveTemplate template, @NotNull Language language, @NotNull Project project) {
    log("custom.template", template.getClass(), language, project);
  }
}
