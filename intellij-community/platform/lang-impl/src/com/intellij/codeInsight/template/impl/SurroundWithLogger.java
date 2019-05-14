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
import com.intellij.lang.LanguageSurrounders;
import com.intellij.lang.folding.CustomFoldingSurroundDescriptor;
import com.intellij.lang.surroundWith.SurroundDescriptor;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.openapi.project.Project;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SurroundWithLogger {
  private final static String USAGE_GROUP = "surround.with";

  public static void logSurrounder(Surrounder surrounder, @NotNull Language language, @NotNull Project project) {
    PluginInfo pluginInfo = PluginInfoDetectorKt.getPluginInfo(surrounder.getClass());
    FeatureUsageData data = new FeatureUsageData().addPluginInfo(pluginInfo).addLanguage(language);
    String description = pluginInfo.getType().isDevelopedByJetBrains() ? surrounder.getTemplateDescription() : "third.party.surrounder";
    data.addData("description", description);
    FUCounterUsageLogger.getInstance().logEvent(project, USAGE_GROUP, "surrounder", data);
  }

  static void logTemplate(@NotNull TemplateImpl template, @NotNull Language language, @NotNull Project project) {
    Triple<String, String, PluginInfo> keyGroupPluginToReport = LiveTemplateRunLogger.getKeyGroupPluginToLog(template);
    if (keyGroupPluginToReport == null) return;

    FeatureUsageData data = new FeatureUsageData().addLanguage(language).
      addData("key", keyGroupPluginToReport.getFirst()).
      addData("group", keyGroupPluginToReport.getSecond());
    PluginInfo pluginInfo = keyGroupPluginToReport.getThird();
    if (pluginInfo != null) {
      data.addPluginInfo(pluginInfo);
    }
    FUCounterUsageLogger.getInstance().logEvent(project, USAGE_GROUP, "template", data);
  }

  static void logCustomTemplate(@NotNull CustomLiveTemplate template, @NotNull Language language, @NotNull Project project) {
    PluginInfo pluginInfo = PluginInfoDetectorKt.getPluginInfo(template.getClass());
    FeatureUsageData data = new FeatureUsageData().addPluginInfo(pluginInfo).addLanguage(language);
    String title = pluginInfo.getType().isDevelopedByJetBrains() ? template.getTitle() : "third.party.custom.template";
    data.addData("title", title);
    FUCounterUsageLogger.getInstance().logEvent(project, USAGE_GROUP, "custom.template", data);
  }

  public static class SurroundWithSurrounderDescriptionValidator extends CustomUtilsWhiteListRule {
    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "surround_with_surrounder_description".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      if ("third.party.surrounder".equals(data)) return ValidationResultType.ACCEPTED;
      for (Surrounder surrounder : CustomFoldingSurroundDescriptor.INSTANCE.getSurrounders()) {
        if (data.equals(surrounder.getTemplateDescription())) {
          PluginInfo info = PluginInfoDetectorKt.getPluginInfo(surrounder.getClass());
          return info.isSafeToReport() ? ValidationResultType.ACCEPTED : ValidationResultType.REJECTED;
        }
      }

      Object languageId = context.eventData.get("lang");
      if (!(languageId instanceof String)) return ValidationResultType.REJECTED;
      Language language = Language.findLanguageByID((String)languageId);
      if (language == null) return ValidationResultType.REJECTED;
      List<SurroundDescriptor> descriptors = LanguageSurrounders.INSTANCE.allForLanguage(language);
      for (SurroundDescriptor descriptor : descriptors) {
        for (Surrounder surrounder : descriptor.getSurrounders()) {
          if (data.equals(surrounder.getTemplateDescription())) {
            PluginInfo info = PluginInfoDetectorKt.getPluginInfo(surrounder.getClass());
            return info.isSafeToReport() ? ValidationResultType.ACCEPTED : ValidationResultType.REJECTED;
          }
        }
      }
      return ValidationResultType.REJECTED;
    }
  }

  public static class SurroundWithTemplateValidator extends CustomUtilsWhiteListRule {
    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "surround_with_template".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      Object key = context.eventData.get("key");
      Object group = context.eventData.get("group");
      if (!(key instanceof String)) return ValidationResultType.REJECTED;
      return LiveTemplateRunLogger.LiveTemplateValidator.validateKeyGroup((String)key, group);
    }
  }

  public static class SurroundWithCustomTemplateValidator extends CustomUtilsWhiteListRule {
    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "surround_with_custom_template_title".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      if ("third.party.custom.template".equals(data)) return ValidationResultType.ACCEPTED;
      for (CustomLiveTemplate template : CustomLiveTemplate.EP_NAME.getExtensions()) {
        if (data.equals(template.getTitle())) {
          PluginInfo info = PluginInfoDetectorKt.getPluginInfo(template.getClass());
          return info.isSafeToReport() ? ValidationResultType.ACCEPTED : ValidationResultType.REJECTED;
        }
      }
      return ValidationResultType.REJECTED;
    }
  }
}
