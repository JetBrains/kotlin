// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomWhiteListRule;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.lang.Language;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PostfixTemplateLogger {
  private static final String USAGE_GROUP = "completion.postfix";
  public static final String CUSTOM = "custom";

  static void log(@NotNull final PostfixTemplate template, @NotNull final PsiElement context) {
    final FeatureUsageData data = new FeatureUsageData().addLanguage(context.getLanguage());
    FUCounterUsageLogger.getInstance().logEvent(context.getProject(), USAGE_GROUP, getTemplateId(template), data);
  }

  @NotNull
  private static String getTemplateId(@NotNull PostfixTemplate template) {
    if (!template.isBuiltin()) {
      return CUSTOM;
    }
    final PostfixTemplateProvider provider = template.getProvider();
    return provider != null ? provider.getId() + "/" + template.getId() : template.getId();
  }

  public static class PostfixTemplateValidator extends CustomWhiteListRule {

    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "completion_template".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      if (StringUtil.equals(data, CUSTOM)) return ValidationResultType.ACCEPTED;

      final Language lang = getLanguage(context);
      if (lang != null) {
        final Pair<PostfixTemplate, PostfixTemplateProvider> template = findPostfixTemplate(lang, data);
        if (template.getFirst() != null && template.getSecond() != null) {
          final PluginInfo templateInfo = PluginInfoDetectorKt.getPluginInfo(template.getFirst().getClass());
          final PluginInfo providerInfo = PluginInfoDetectorKt.getPluginInfo(template.getSecond().getClass());
          if (StringUtil.equals(data, context.eventId)) {
            context.setPluginInfo(templateInfo);
          }
          return templateInfo.isDevelopedByJetBrains() && providerInfo.isDevelopedByJetBrains() ?
                 ValidationResultType.ACCEPTED : ValidationResultType.THIRD_PARTY;
        }
      }
      return ValidationResultType.REJECTED;
    }

    @NotNull
    private static Pair<PostfixTemplate, PostfixTemplateProvider> findPostfixTemplate(@NotNull Language lang, @NotNull String data) {
      final String[] split = data.split("/");
      if (split.length == 2) {
        final PostfixTemplateProvider provider = findProviderById(split[0].trim(), lang);
        final PostfixTemplate template = provider != null ? findTemplateById(provider, split[1].trim()) : null;
        return provider != null && template != null ? Pair.create(template, provider) : Pair.empty();
      }
      else if (split.length == 1) {
        final String templateId = split[0].trim();
        for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(lang)) {
          final PostfixTemplate template = findTemplateById(provider, templateId);
          if (template != null) {
            return Pair.create(template, provider);
          }
        }
      }
      return Pair.empty();
    }

    @Nullable
    private static PostfixTemplateProvider findProviderById(@NotNull String id, @NotNull Language lang) {
      for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(lang)) {
        if (StringUtil.equals(provider.getId(), id)) {
          return provider;
        }
      }
      return null;
    }

    @Nullable
    private static PostfixTemplate findTemplateById(@NotNull PostfixTemplateProvider provider, @NotNull String id) {
      for (PostfixTemplate template : provider.getTemplates()) {
        if (StringUtil.equals(template.getId(), id)) {
          return template;
        }
      }
      return null;
    }
  }
}
