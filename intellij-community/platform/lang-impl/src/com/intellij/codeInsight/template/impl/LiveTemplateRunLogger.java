// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomUtilsWhiteListRule;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.lang.Language;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class LiveTemplateRunLogger {
  private static final String GROUP = "live.templates";

  static void log(@NotNull TemplateImpl template, @NotNull Language language) {
    String key = template.getKey();
    String groupName = template.getGroupName();
    if (isCreatedProgrammatically(key, groupName)) return;

    PluginInfo plugin = TemplateSettings.getInstance().findPluginForPredefinedTemplate(template);
    if (plugin == null) {
      key = "user.defined.template";
      groupName = "user.defined.group";
    }
    else if (!plugin.isSafeToReport()) {
      key = "custom.plugin.template";
      groupName = "custom.plugin.group";
    }

    FeatureUsageData data = new FeatureUsageData().addLanguage(language).addData("group", groupName);
    if (plugin != null) {
      data.addPluginInfo(plugin);
    }
    FUCounterUsageLogger.getInstance().logEvent(GROUP, key, data);
  }

  private static boolean isCreatedProgrammatically(String key, String groupName) {
    return StringUtil.isEmpty(key) || StringUtil.isEmpty(groupName);
  }

  public static class LiveTemplateValidator extends CustomUtilsWhiteListRule {
    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "live_template".equals(ruleId) || "live_template_group".equals(ruleId) ;
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      Object group = context.eventData.get("group");
      if (group == null) return ValidationResultType.REJECTED;
      if ("user.defined.template".equals(context.eventId) && "user.defined.group".equals(group)) return ValidationResultType.ACCEPTED;
      if ("custom.plugin.template".equals(context.eventId) && "custom.plugin.group".equals(group)) return ValidationResultType.ACCEPTED;
      try {
        TemplateImpl template = TemplateSettings.getInstance().getTemplate(context.eventId, group.toString());
        if (template != null) {
          PluginInfo info = TemplateSettings.getInstance().findPluginForPredefinedTemplate(template);
          if (info != null && info.isSafeToReport()) return ValidationResultType.ACCEPTED;
        }
      } catch (Exception ignored) { }
      return ValidationResultType.REJECTED;
    }
  }

}
