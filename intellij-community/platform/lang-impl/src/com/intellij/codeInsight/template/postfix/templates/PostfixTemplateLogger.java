// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.internal.statistic.eventLog.FeatureUsageData;
import com.intellij.internal.statistic.eventLog.validator.ValidationResultType;
import com.intellij.internal.statistic.eventLog.validator.rules.EventContext;
import com.intellij.internal.statistic.eventLog.validator.rules.impl.CustomValidationRule;
import com.intellij.internal.statistic.service.fus.collectors.FUCounterUsageLogger;
import com.intellij.internal.statistic.utils.PluginInfo;
import com.intellij.internal.statistic.utils.PluginInfoDetectorKt;
import com.intellij.lang.Language;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PostfixTemplateLogger {
  private static final String USAGE_GROUP = "completion.postfix";
  private static final String CUSTOM = "custom";
  private static final String NO_PROVIDER = "no.provider";
  private static final String TEMPLATE_FIELD = "template";
  private static final String PROVIDER_FIELD = "provider";

  static void log(@NotNull final PostfixTemplate template, @NotNull final PsiElement context) {
    final Project project = context.getProject();
    final FeatureUsageData data = new FeatureUsageData().addLanguage(context.getLanguage());
    if (template.isBuiltin()) {
      final PostfixTemplateProvider provider = template.getProvider();
      final String providerId = provider != null ? provider.getId() : NO_PROVIDER;
      data.addData(PROVIDER_FIELD, providerId).addData(TEMPLATE_FIELD, template.getId());
    }
    else {
      data.addData(PROVIDER_FIELD, CUSTOM).addData(TEMPLATE_FIELD, CUSTOM);
    }
    FUCounterUsageLogger.getInstance().logEvent(project, USAGE_GROUP, "expanded", data);
  }

  public static class PostfixTemplateValidator extends CustomValidationRule {

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

      final String provider = getEventDataField(context, PROVIDER_FIELD);
      final String template = getEventDataField(context, TEMPLATE_FIELD);
      if (provider == null || template == null || !isProviderOrTemplate(data, provider, template)) {
        return ValidationResultType.REJECTED;
      }

      final Pair<PostfixTemplate, PostfixTemplateProvider> result = findPostfixTemplate(lang, provider, template);
      if (result.getFirst() != null && result.getSecond() != null) {
        final PluginInfo templateInfo = PluginInfoDetectorKt.getPluginInfo(result.getFirst().getClass());
        final PluginInfo providerInfo = PluginInfoDetectorKt.getPluginInfo(result.getSecond().getClass());
        context.setPluginInfo(templateInfo);
        return templateInfo.isDevelopedByJetBrains() && providerInfo.isDevelopedByJetBrains() ?
               ValidationResultType.ACCEPTED : ValidationResultType.THIRD_PARTY;
      }
      return ValidationResultType.REJECTED;
    }

    private static boolean isProviderOrTemplate(@NotNull String data, @NotNull String provider, @NotNull String template) {
      return StringUtil.equals(data, provider) || StringUtil.equals(data, template);
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
