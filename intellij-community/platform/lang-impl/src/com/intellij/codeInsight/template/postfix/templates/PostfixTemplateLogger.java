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
  private static final String CUSTOM = "custom";
  private static final String NO_PROVIDER = "no.provider";
  private static final String TEMPLATE_FIELD = "template";

  static void log(@NotNull final PostfixTemplate template, @NotNull final PsiElement context) {
    final FeatureUsageData data = new FeatureUsageData().addLanguage(context.getLanguage());
    if (template.isBuiltin()) {
      data.addData(TEMPLATE_FIELD, template.getId());
      final PostfixTemplateProvider provider = template.getProvider();
      final String providerId = provider != null ? provider.getId() : NO_PROVIDER;
      FUCounterUsageLogger.getInstance().logEvent(context.getProject(), USAGE_GROUP, providerId, data);
    }
    else {
      FUCounterUsageLogger.getInstance().logEvent(context.getProject(), USAGE_GROUP, CUSTOM, data);
    }
  }

  public static class PostfixTemplateValidator extends CustomWhiteListRule {

    @Override
    public boolean acceptRuleId(@Nullable String ruleId) {
      return "completion_template".equals(ruleId) || "completion_provider_template".equals(ruleId);
    }

    @NotNull
    @Override
    protected ValidationResultType doValidate(@NotNull String data, @NotNull EventContext context) {
      if (StringUtil.equals(data, CUSTOM) || StringUtil.equals(data, NO_PROVIDER)) return ValidationResultType.ACCEPTED;

      final Language lang = getLanguage(context);
      if (lang == null) return ValidationResultType.REJECTED;

      final String providerId = context.eventId;
      final String templateId = context.eventData.containsKey(TEMPLATE_FIELD) ? context.eventData.get(TEMPLATE_FIELD).toString() : null;
      if ((StringUtil.equals(data, providerId) || StringUtil.equals(data, templateId)) && StringUtil.isNotEmpty(templateId)) {
        final Pair<PostfixTemplate, PostfixTemplateProvider> template = findPostfixTemplate(lang, providerId, templateId);
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
    private static Pair<PostfixTemplate, PostfixTemplateProvider> findPostfixTemplate(@NotNull Language lang,
                                                                                      @NotNull String providerId,
                                                                                      @NotNull String templateId) {
      if (!StringUtil.equals(providerId, NO_PROVIDER)) {
        final PostfixTemplateProvider provider = findProviderById(providerId, lang);
        final PostfixTemplate template = provider != null ? findTemplateById(provider, templateId) : null;
        return provider != null && template != null ? Pair.create(template, provider) : Pair.empty();
      }
      else {
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
