// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.template.CustomLiveTemplate;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomUtilsWhiteListRule;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.lang.Language;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.project.Project;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SurroundWithLogger {
  private final static String USAGE_GROUP = "surround.with";

  public static void logSurrounder(Surrounder surrounder, @NotNull Language language, @NotNull Project project) {
    log("surrounder", surrounder.getClass(), language, project);
  }

  private static void log(@NotNull String type,
                          @NotNull Class elementClass,
                          @NotNull Language language,
                          @NotNull Project project) {
    PluginInfo pluginInfo = PluginInfoDetectorKt.getPluginInfo(elementClass);
    FeatureUsageData data = new FeatureUsageData().addPluginInfo(pluginInfo).addLanguage(language);
    data.addData("type", type);
    String description = pluginInfo.getType().isDevelopedByJetBrains() ? elementClass.getName() : "third.party";
    FUCounterUsageLogger.getInstance().logEvent(project, USAGE_GROUP, description, data);
  }

  static void logTemplate(@NotNull TemplateImpl template, @NotNull Language language, @NotNull Project project) {
    Triple<String, String, PluginInfo> keyGroupPluginToReport = LiveTemplateRunLogger.getKeyGroupPluginToLog(template);
    if (keyGroupPluginToReport == null) return;

    FeatureUsageData data = new FeatureUsageData().addLanguage(language).
      addData("type", "template").
      addData("group", keyGroupPluginToReport.getSecond());
    PluginInfo pluginInfo = keyGroupPluginToReport.getThird();
    if (pluginInfo != null) {
      data.addPluginInfo(pluginInfo);
    }
    FUCounterUsageLogger.getInstance().logEvent(project, USAGE_GROUP, keyGroupPluginToReport.getFirst(), data);
  }

  static void logCustomTemplate(@NotNull CustomLiveTemplate template, @NotNull Language language, @NotNull Project project) {
    log("custom.template", template.getClass(), language, project);
  }

  public static class SurroundWithIdValidator extends CustomUtilsWhiteListRule {
    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "surround_with_id".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      if ("third.party".equals(data)) return ValidationResultType.ACCEPTED;
      Object typeObject = context.eventData.get("type");
      if (!(typeObject instanceof String)) {
        return ValidationResultType.REJECTED;
      }

      if ("surrounder".equals(typeObject) || "custom.template".equals(typeObject)) {
        boolean isFromPluginRepository = PluginInfoDetectorKt.getPluginInfo(data).isSafeToReport();
        return isFromPluginRepository ? ValidationResultType.ACCEPTED : ValidationResultType.REJECTED;
      }
      if ("template".equals(typeObject)) {
        Object group = context.eventData.get("group");
        return LiveTemplateRunLogger.LiveTemplateValidator.validateKeyGroup(data, group);
      }

      return ValidationResultType.REJECTED;
    }
  }
}
