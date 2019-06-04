// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.tools;

import com.intellij.codeInspection.InspectionEP;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.ScopeToolState;
import com.intellij.internal.statistic.beans.MetricEvent;
import com.intellij.internal.statistic.beans.MetricEventFactoryKt;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomWhiteListRule;
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

public class InspectionsUsagesCollector extends ProjectUsagesCollector {
  private static final Predicate<ScopeToolState> ENABLED = state -> !state.getTool().isEnabledByDefault() && state.isEnabled();

  private static final Predicate<ScopeToolState> DISABLED = state -> state.getTool().isEnabledByDefault() && !state.isEnabled();

  @NotNull
  @Override
  public Set<MetricEvent> getMetrics(@NotNull final Project project) {
    final Set<MetricEvent> result = new HashSet<>();
    final List<ScopeToolState> tools = InspectionProjectProfileManager.getInstance(project).getCurrentProfile().getAllTools();
    for (ScopeToolState state : tools) {
      if (ENABLED.test(state)) {
        result.add(create(state, true));
      }
      else if (DISABLED.test(state)) {
        result.add(create(state, false));
      }
    }
    return result;
  }

  @NotNull
  private static MetricEvent create(@NotNull ScopeToolState state, boolean enabled) {
    final InspectionToolWrapper tool = state.getTool();
    final FeatureUsageData data = new FeatureUsageData().addData("enabled", enabled);
    final String language = tool.getLanguage();
    if (StringUtil.isNotEmpty(language)) {
      data.addLanguage(Language.findLanguageByID(language));
    }
    final InspectionEP extension = tool.getExtension();
    final PluginInfo info = extension != null ? PluginInfoDetectorKt.getPluginInfoById(extension.getPluginId()) : null;
    if (info != null) {
      data.addPluginInfo(info);
    }
    final String id = info != null && info.isSafeToReport() ? state.getTool().getID() : "third.party";
    return MetricEventFactoryKt.newMetric(id, data);
  }

  @NotNull
  @Override
  public String getGroupId() {
    return "inspections";
  }

  @Override
  public int getVersion() {
    return 1;
  }

  public static class InspectionToolValidator extends CustomWhiteListRule {

    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "tool".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      if (isThirdPartyValue(data)) return ValidationResultType.ACCEPTED;
      return acceptWhenReportedByPluginFromPluginRepository(context);
    }
  }
}