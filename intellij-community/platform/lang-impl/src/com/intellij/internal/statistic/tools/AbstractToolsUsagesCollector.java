// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.internal.statistic.tools;

import com.intellij.codeInspection.InspectionEP;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import com.intellij.codeInspection.ex.ScopeToolState;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerMain;
import com.intellij.internal.statistic.beans.UsageDescriptor;
import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.service.fus.collectors.ProjectUsagesCollector;
import com.intellij.internal.statistic.utils.StatisticsUtilKt;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.profile.codeInspection.InspectionProjectProfileManager;
import com.intellij.util.ObjectUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractToolsUsagesCollector extends ProjectUsagesCollector {

  private static final Predicate<ScopeToolState> BUNDLED = state -> {
    final IdeaPluginDescriptor descriptor = getIdeaPluginDescriptor(state);
    return descriptor != null && descriptor.isBundled() && PluginManagerMain.isDevelopedByJetBrains(descriptor);
  };

  private static final Predicate<ScopeToolState> LISTED = state -> {
    IdeaPluginDescriptor descriptor = getIdeaPluginDescriptor(state);
    return descriptor != null && !descriptor.isBundled() && StatisticsUtilKt.isSafeToReportFrom(descriptor);
  };

  private static IdeaPluginDescriptor getIdeaPluginDescriptor(final ScopeToolState state) {
    final InspectionEP extension = state.getTool().getExtension();
    return extension != null ? ObjectUtils.tryCast(extension.getPluginDescriptor(), IdeaPluginDescriptor.class) : null;
  }

  private static final Predicate<ScopeToolState> ENABLED = state -> !state.getTool().isEnabledByDefault() && state.isEnabled();

  private static final Predicate<ScopeToolState> DISABLED = state -> state.getTool().isEnabledByDefault() && !state.isEnabled();

  @NotNull
  @Override
  public Set<UsageDescriptor> getUsages(@NotNull final Project project) {
    final List<ScopeToolState> tools = InspectionProjectProfileManager.getInstance(project).getCurrentProfile().getAllTools();
    return filter(tools.stream())
      .map(ScopeToolState::getTool)
      .map(tool -> new UsageDescriptor(tool.getID(), getInspectionToolData(tool)))
      .collect(Collectors.toSet());
  }

  @NotNull
  protected FeatureUsageData getInspectionToolData(InspectionToolWrapper tool) {
    final FeatureUsageData data = new FeatureUsageData();
    final String language = tool.getLanguage();
    if (StringUtil.isNotEmpty(language)) {
      data.addData("lang", language);
    }
    return data;
  }

  @Override
  public int getVersion() {
    return 2;
  }

  @NotNull
  protected abstract Stream<ScopeToolState> filter(@NotNull final Stream<ScopeToolState> tools);

  protected static abstract class AbstractListedToolsUsagesCollector extends AbstractToolsUsagesCollector {
    @NotNull
    @Override
    protected FeatureUsageData getInspectionToolData(InspectionToolWrapper tool) {
      final FeatureUsageData data = super.getInspectionToolData(tool);
      final PluginId pluginId = tool.getExtension().getPluginId();
      if (pluginId != null) {
        data.addData("plugin_id", pluginId.getIdString());
      }
      return data;
    }
  }

  public static class EnabledBundledToolsUsagesCollector extends AbstractToolsUsagesCollector {


    @NotNull
    @Override
    public String getGroupId() {
      return "enabled.bundled.tools";
    }

    @NotNull
    @Override
    protected Stream<ScopeToolState> filter(@NotNull final Stream<ScopeToolState> tools) {
      return tools.filter(ENABLED).filter(BUNDLED);
    }
  }

  public static class EnabledListedToolsUsagesCollector extends AbstractListedToolsUsagesCollector {

    @NotNull
    @Override
    public String getGroupId() {
      return "enabled.listed.tools";
    }

    @NotNull
    @Override
    protected Stream<ScopeToolState> filter(@NotNull final Stream<ScopeToolState> tools) {
      return tools.filter(ENABLED).filter(LISTED);
    }
  }

  public static class DisabledBundledToolsUsagesCollector extends AbstractToolsUsagesCollector {

    @NotNull
    @Override
    public String getGroupId() {
      return "disabled.bundled.tools";
    }

    @NotNull
    @Override
    protected Stream<ScopeToolState> filter(@NotNull final Stream<ScopeToolState> tools) {
      return tools.filter(DISABLED).filter(BUNDLED);
    }
  }

  public static class DisabledListedToolsUsagesCollector extends AbstractListedToolsUsagesCollector {

    @NotNull
    @Override
    public String getGroupId() {
      return "disabled.listed.tools";
    }

    @NotNull
    @Override
    protected Stream<ScopeToolState> filter(@NotNull final Stream<ScopeToolState> tools) {
      return tools.filter(DISABLED).filter(LISTED);
    }
  }
}