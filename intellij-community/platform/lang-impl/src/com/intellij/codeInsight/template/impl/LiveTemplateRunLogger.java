// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomWhiteListRule;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import kotlin.Triple;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class LiveTemplateRunLogger {
  private static final String GROUP = "live.templates";

  static void log(@NotNull Project project, @NotNull TemplateImpl template, @NotNull Language language) {
    final FeatureUsageData data = createTemplateData(template, language);
    if (data != null) {
      FUCounterUsageLogger.getInstance().logEvent(project, GROUP, "started", data);
    }
  }

  @Nullable
  static Triple<String, String, PluginInfo> getKeyGroupPluginToLog(@NotNull TemplateImpl template) {
    String key = template.getKey();
    String groupName = template.getGroupName();
    if (isCreatedProgrammatically(key, groupName)) return null;

    PluginInfo plugin = TemplateSettings.getInstance().findPluginForPredefinedTemplate(template);
    if (plugin == null) {
      key = "user.defined.template";
      groupName = "user.defined.group";
    }
    else if (!plugin.isSafeToReport()) {
      key = "custom.plugin.template";
      groupName = "custom.plugin.group";
    }
    return new Triple<>(key, groupName, plugin);
  }

  @Nullable
  static FeatureUsageData createTemplateData(@NotNull TemplateImpl template, @NotNull Language language) {
    Triple<String, String, PluginInfo> keyGroupPluginToLog = getKeyGroupPluginToLog(template);
    if (keyGroupPluginToLog == null) {
      return null;
    }

    FeatureUsageData data = new FeatureUsageData().addLanguage(language).addData("group", keyGroupPluginToLog.getSecond());
    PluginInfo plugin = keyGroupPluginToLog.getThird();
    if (plugin != null) {
      data.addPluginInfo(plugin);
    }
    data.addData("key", keyGroupPluginToLog.getFirst());
    data.addData("changedByUser", TemplateSettings.getInstance().differsFromDefault(template));
    return data;
  }

  private static boolean isCreatedProgrammatically(String key, String groupName) {
    return StringUtil.isEmpty(key) || StringUtil.isEmpty(groupName);
  }

  public static class LiveTemplateValidator extends CustomWhiteListRule {
    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "live_template".equals(ruleId) || "live_template_group".equals(ruleId) ;
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      final String key = getEventDataField(context, "key");
      final String group = getEventDataField(context, "group");
      if (key == null || group == null || !isKeyOrGroup(data, key, group)) {
        return ValidationResultType.REJECTED;
      }
      return validateKeyGroup(key, group);
    }

    @NotNull
    public static ValidationResultType validateKeyGroup(String key, Object group) {
      if (group == null) return ValidationResultType.REJECTED;
      if ("user.defined.template".equals(key) && "user.defined.group".equals(group)) return ValidationResultType.ACCEPTED;
      if ("custom.plugin.template".equals(key) && "custom.plugin.group".equals(group)) return ValidationResultType.ACCEPTED;
      try {
        TemplateImpl template = TemplateSettings.getInstance().getTemplate(key, group.toString());
        if (template != null) {
          PluginInfo info = TemplateSettings.getInstance().findPluginForPredefinedTemplate(template);
          if (info != null && info.isSafeToReport()) return ValidationResultType.ACCEPTED;
        }
      } catch (Exception ignored) { }
      return ValidationResultType.REJECTED;
    }

    private static boolean isKeyOrGroup(@NotNull String data, @NotNull String key, @NotNull String group) {
      return StringUtil.equals(data, key) || StringUtil.equals(data, group);
    }
  }
}
