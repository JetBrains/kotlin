// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.settings;

import com.intellij.codeInsight.template.postfix.templates.LanguagePostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixChangedBuiltinTemplate;
import com.intellij.codeInsight.template.postfix.templates.editable.PostfixTemplateWrapper;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.SimpleModificationTracker;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@State(name = "PostfixTemplates", storages = @Storage("postfixTemplates.xml"))
public class PostfixTemplateStorage extends SimpleModificationTracker implements PersistentStateComponent<Element> {
  private static final String TEMPLATE_TAG = "template";
  private static final String PROVIDER_ATTR_NAME = "provider";
  private static final String ID_ATTR_NAME = "id";
  private static final String KEY_ATTR_NAME = "key";
  private static final String BUILTIN_ATTR_NAME = "builtin";

  private final Map<String, PostfixTemplateProvider> myTemplateProviders;
  private final MultiMap<String, PostfixTemplate> myTemplates = MultiMap.createSmart();
  private final List<Element> myUnloadedTemplates = ContainerUtil.newSmartList();

  public PostfixTemplateStorage() {
    myTemplateProviders = new HashMap<>();
    LanguageExtensionPoint[] extensions = new ExtensionPointName<LanguageExtensionPoint>(LanguagePostfixTemplate.EP_NAME).getExtensions();
    for (LanguageExtensionPoint extension : extensions) {
      Object provider = extension.getInstance();
      if (provider instanceof PostfixTemplateProvider) {
        myTemplateProviders.put(((PostfixTemplateProvider)provider).getId(), (PostfixTemplateProvider)provider);
      }
    }
  }

  @NotNull
  public static PostfixTemplateStorage getInstance() {
    return ServiceManager.getService(PostfixTemplateStorage.class);
  }

  @NotNull
  public Set<PostfixTemplate> getTemplates(@NotNull PostfixTemplateProvider provider) {
    return new HashSet<>(myTemplates.get(provider.getId()));
  }

  public void setTemplates(@NotNull PostfixTemplateProvider provider, @NotNull Collection<PostfixTemplate> templates) {
    Collection<PostfixTemplate> oldTemplates = myTemplates.get(provider.getId());
    if (!templates.equals(oldTemplates)) {
      myTemplates.put(provider.getId(), templates);
      incModificationCount();
    }
  }

  @Override
  public void loadState(@NotNull final Element state) {
    myTemplates.clear();
    for (Element templateElement : state.getChildren(TEMPLATE_TAG)) {
      PostfixTemplateProvider provider = myTemplateProviders.get(templateElement.getAttributeValue(PROVIDER_ATTR_NAME, ""));
      if (provider != null) {
        String templateId = templateElement.getAttributeValue(ID_ATTR_NAME, "");
        String templateName = StringUtil.trimStart(StringUtil.notNullize(templateElement.getAttributeValue(KEY_ATTR_NAME)), ".");
        if (StringUtil.isEmpty(templateId) || StringUtil.isEmpty(templateName)) continue;

        PostfixTemplate externalTemplate = provider.readExternalTemplate(templateId, templateName, templateElement);
        String builtinId = templateElement.getAttributeValue(BUILTIN_ATTR_NAME);
        PostfixTemplate builtinTemplate = findBuiltinTemplate(builtinId, provider);
        if (builtinTemplate != null) {
          PostfixTemplate delegate = externalTemplate != null
                                     ? externalTemplate
                                     : new PostfixTemplateWrapper(templateId, templateName, "." + templateName, builtinTemplate, provider);
          myTemplates.putValue(provider.getId(), new PostfixChangedBuiltinTemplate(delegate, builtinTemplate));
        }
        else if (externalTemplate != null) {
          myTemplates.putValue(provider.getId(), externalTemplate);
        }
        else {
          myUnloadedTemplates.add(templateElement);
        }
      }
      else {
        myUnloadedTemplates.add(templateElement);
      }
    }
  }

  @Nullable
  private static PostfixTemplate findBuiltinTemplate(@Nullable String id, @NotNull PostfixTemplateProvider provider) {
    return ContainerUtil.find(provider.getTemplates(), p -> p.getId().equals(id));
  }

  @Override
  public Element getState() {
    Element element = new Element("state");
    for (Element template : myUnloadedTemplates) {
      element.addContent(template.clone());
    }
    for (Map.Entry<String, Collection<PostfixTemplate>> entry : myTemplates.entrySet()) {
      for (PostfixTemplate template : entry.getValue()) {
        PostfixTemplateProvider provider = template.getProvider();
        if (provider != null) {
          if (template instanceof PostfixChangedBuiltinTemplate) {
            PostfixChangedBuiltinTemplate changedBuiltinTemplate = (PostfixChangedBuiltinTemplate)template;
            String builtin = changedBuiltinTemplate.getBuiltinTemplate().getId();
            element.addContent(writeTemplate(changedBuiltinTemplate.getDelegate(), provider, builtin));
          }
          else {
            element.addContent(writeTemplate(template, provider, null));
          }
        }
      }
    }
    return element;
  }

  private static Element writeTemplate(@NotNull PostfixTemplate template,
                                       @NotNull PostfixTemplateProvider provider,
                                       @Nullable String builtinId) {
    Element templateElement = new Element(TEMPLATE_TAG);
    templateElement.setAttribute(ID_ATTR_NAME, template.getId());
    templateElement.setAttribute(KEY_ATTR_NAME, template.getKey());
    templateElement.setAttribute(PROVIDER_ATTR_NAME, provider.getId());
    if (builtinId != null) {
      templateElement.setAttribute(BUILTIN_ATTR_NAME, builtinId);
    }
    provider.writeExternalTemplate(template, templateElement);
    return templateElement;
  }
}