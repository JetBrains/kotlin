// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.template.impl.TemplateSettings;
import com.intellij.codeInsight.template.postfix.templates.LanguagePostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.lang.Language;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.Factory;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters;
import com.intellij.util.xmlb.XmlSerializer;
import com.intellij.util.xmlb.annotations.MapAnnotation;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

@State(name = "PostfixTemplatesSettings", storages = @Storage("postfixTemplates.xml"))
public class PostfixTemplatesSettings implements PersistentStateComponent<Element> {
  public static final Factory<Set<String>> SET_FACTORY = () -> ContainerUtil.newHashSet();
  private Map<String, Set<String>> myProviderToDisabledTemplates = ContainerUtil.newHashMap();
  /**
   * @deprecated use myProviderToDisabledTemplates
   */
  @Deprecated private Map<String, Set<String>> myLangToDisabledTemplates = ContainerUtil.newHashMap();

  private boolean postfixTemplatesEnabled = true;
  private boolean templatesCompletionEnabled = true;
  private int myShortcut = TemplateSettings.TAB_CHAR;

  public boolean isTemplateEnabled(@NotNull PostfixTemplate template, @NotNull PostfixTemplateProvider provider) {
    Set<String> result = myProviderToDisabledTemplates.get(provider.getId());
    return result == null || !result.contains(template.getId());
  }

  public void disableTemplate(@NotNull PostfixTemplate template, @NotNull PostfixTemplateProvider provider) {
    disableTemplate(template, provider.getId());
  }

  public void disableTemplate(@NotNull PostfixTemplate template, @NotNull String providerId) {
    Set<String> state = ContainerUtil.getOrCreate(myProviderToDisabledTemplates, providerId, SET_FACTORY);
    state.add(template.getId());
  }

  public boolean isPostfixTemplatesEnabled() {
    return postfixTemplatesEnabled;
  }

  public void setPostfixTemplatesEnabled(boolean postfixTemplatesEnabled) {
    this.postfixTemplatesEnabled = postfixTemplatesEnabled;
  }

  public boolean isTemplatesCompletionEnabled() {
    return templatesCompletionEnabled;
  }

  public void setTemplatesCompletionEnabled(boolean templatesCompletionEnabled) {
    this.templatesCompletionEnabled = templatesCompletionEnabled;
  }

  /**
   * @deprecated use getProviderToDisabledTemplates
   */
  @Deprecated
  @NotNull
  @MapAnnotation(entryTagName = "disabled-postfix-templates", keyAttributeName = "lang", surroundWithTag = false)
  public Map<String, Set<String>> getLangDisabledTemplates() {
    return myLangToDisabledTemplates;
  }

  /**
   * @deprecated use setProviderToDisabledTemplates
   */
  @Deprecated
  public void setLangDisabledTemplates(@NotNull Map<String, Set<String>> templatesState) {
    myLangToDisabledTemplates = templatesState;
  }

  @NotNull
  @MapAnnotation(entryTagName = "disabled-templates", keyAttributeName = "provider", surroundWithTag = false)
  public Map<String, Set<String>> getProviderToDisabledTemplates() {
    return myProviderToDisabledTemplates;
  }

  public void setProviderToDisabledTemplates(@NotNull Map<String, Set<String>> templatesState) {
    myProviderToDisabledTemplates = templatesState;
  }

  public int getShortcut() {
    return myShortcut;
  }

  public void setShortcut(int shortcut) {
    myShortcut = shortcut;
  }

  @NotNull
  public static PostfixTemplatesSettings getInstance() {
    return ServiceManager.getService(PostfixTemplatesSettings.class);
  }

  @Nullable
  @Override
  public Element getState() {
    return XmlSerializer.serialize(this, new SkipDefaultValuesSerializationFilters());
  }

  @Override
  public void loadState(@NotNull Element settings) {
    XmlSerializer.deserializeInto(this, settings);

    if (!myLangToDisabledTemplates.isEmpty()) {
      MultiMap<String, Language> importedLanguages = getLanguagesToImport();
      for (Map.Entry<String, Set<String>> entry : myLangToDisabledTemplates.entrySet()) {
        for (Language language : importedLanguages.get(entry.getKey())) {
          for (PostfixTemplateProvider provider : LanguagePostfixTemplate.LANG_EP.allForLanguage(language)) {
            for (PostfixTemplate template : provider.getTemplates()) {
              if (entry.getValue().contains(template.getKey())) {
                disableTemplate(template, provider);
              }
            }
          }
        }
      }
      for (String language : importedLanguages.keySet()) {
        myLangToDisabledTemplates.remove(language);
      }
    }
  }

  @NotNull
  private static MultiMap<String, Language> getLanguagesToImport() {
    MultiMap<String, Language> importedLanguages = MultiMap.create();
    LanguageExtensionPoint[] extensions = new ExtensionPointName<LanguageExtensionPoint>(LanguagePostfixTemplate.EP_NAME).getExtensions();
    for (LanguageExtensionPoint extension : extensions) {
      Language language = Language.findLanguageByID(extension.getKey());
      if (language == null) continue;
      importedLanguages.putValue(language.getDisplayName(), language);
    }
    return importedLanguages;
  }
}
