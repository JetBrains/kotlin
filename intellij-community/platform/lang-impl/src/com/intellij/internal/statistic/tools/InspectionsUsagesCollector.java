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
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import org.jdom.Attribute;
import org.jdom.Content;
import org.jdom.DataConversionException;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class InspectionsUsagesCollector extends ProjectUsagesCollector {
  private static final Predicate<ScopeToolState> ENABLED = state -> !state.getTool().isEnabledByDefault() && state.isEnabled();

  private static final Predicate<ScopeToolState> DISABLED = state -> state.getTool().isEnabledByDefault() && !state.isEnabled();
  private static final String SETTING_VALUE = "option_value";
  private static final String SETTING_TYPE = "option_type";
  private static final String SETTING_INDEX = "option_index";
  private static final String INSPECTION_ID = "inspection_id";

  @NotNull
  @Override
  public String getGroupId() {
    return "inspections";
  }

  @Override
  public int getVersion() {
    return 4;
  }

  @NotNull
  @Override
  public Set<MetricEvent> getMetrics(@NotNull final Project project) {
    final Set<MetricEvent> result = new HashSet<>();
    final List<ScopeToolState> tools = InspectionProjectProfileManager.getInstance(project).getCurrentProfile().getAllTools();
    for (ScopeToolState state : tools) {
      InspectionToolWrapper<?, ?> tool = state.getTool();
      PluginInfo pluginInfo = getInfo(tool);
      if (ENABLED.test(state)) {
        result.add(create(tool, pluginInfo, true));
      }
      else if (DISABLED.test(state)) {
        result.add(create(tool, pluginInfo, false));
      }

      result.addAll(getChangedSettingsEvents(tool, pluginInfo, state.isEnabled()));
    }
    return result;
  }

  private static Collection<MetricEvent> getChangedSettingsEvents(InspectionToolWrapper<?, ?> tool,
                                                                  PluginInfo pluginInfo,
                                                                  boolean inspectionEnabled) {
    if (!isSafeToReport(pluginInfo)) return Collections.emptyList();
    Collection<MetricEvent> result = new ArrayList<>();
    String inspectionId = tool.getID();
    List<Content> options = getOptions(tool);
    for (int i = 0; i < options.size(); i++) {
      Content option = options.get(i);
      if (option instanceof Element) {
        Attribute settingValue = ((Element)option).getAttribute("value");
        if (settingValue == null) continue;
        FeatureUsageData data = new FeatureUsageData();
        // setting the index instead of name is here because name can contain sensitive data
        data.addData(SETTING_INDEX, i);
        data.addData(INSPECTION_ID, inspectionId);
        data.addData("inspection_enabled", inspectionEnabled);
        data.addPluginInfo(pluginInfo);
        if (addSettingValue(settingValue, data)) {
          result.add(MetricEventFactoryKt.newMetric("setting.non.default.state", data));
        }
      }
    }
    return result;
  }

  private static boolean addSettingValue(Attribute settingValueAttribute, FeatureUsageData data) {
    try {
      boolean booleanValue = settingValueAttribute.getBooleanValue();
      data.addData(SETTING_VALUE, booleanValue);
      data.addData(SETTING_TYPE, "boolean");
      return true;
    }
    catch (DataConversionException e) {
      return addIntValue(settingValueAttribute, data);
    }
  }

  private static boolean addIntValue(Attribute value, FeatureUsageData data) {
    try {
      int intValue = value.getIntValue();
      data.addData(SETTING_VALUE, intValue);
      data.addData(SETTING_TYPE, "integer");
      return true;
    }
    catch (DataConversionException e) {
      return false;
    }

  }

  @NotNull
  private static MetricEvent create(InspectionToolWrapper<?, ?> tool, PluginInfo info, boolean enabled) {
    final FeatureUsageData data = new FeatureUsageData().addData("enabled", enabled);
    final String language = tool.getLanguage();
    if (StringUtil.isNotEmpty(language)) {
      data.addLanguage(Language.findLanguageByID(language));
    }
    if (info != null) {
      data.addPluginInfo(info);
    }
    data.addData(INSPECTION_ID, isSafeToReport(info) ? tool.getID() : "third.party");
    return MetricEventFactoryKt.newMetric("not.default.state", data);
  }

  private static boolean isSafeToReport(PluginInfo info) {
    return info != null && info.isSafeToReport();
  }

  @Nullable
  private static PluginInfo getInfo(InspectionToolWrapper<?, ?> tool) {
    InspectionEP extension = tool.getExtension();
    PluginDescriptor pluginDescriptor = extension == null ? null : extension.getPluginDescriptor();
    return pluginDescriptor != null ? PluginInfoDetectorKt.getPluginInfoByDescriptor(pluginDescriptor) : null;
  }

  @NotNull
  private static List<Content> getOptions(InspectionToolWrapper<?, ?> tool) {
    Element options = new Element("options");
    try {
      ScopeToolState.tryWriteSettings(tool.getTool(), options);
      return options.getContent();
    }
    catch (Exception e) {
      return Collections.emptyList();
    }
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