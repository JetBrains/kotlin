// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.template.Template;
import com.intellij.codeInsight.template.TemplateManager;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class LiveTemplateLookupElementImpl extends LiveTemplateLookupElement {
  private final TemplateImpl myTemplate;

  public LiveTemplateLookupElementImpl(@NotNull TemplateImpl template, boolean sudden) {
    super(template.getKey(), StringUtil.notNullize(template.getDescription()), sudden, LiveTemplateCompletionContributor.shouldShowAllTemplates());
    myTemplate = template;
  }

  @NotNull
  @Override
  public String getLookupString() {
    return myTemplate.getKey();
  }

  @NotNull
  public TemplateImpl getTemplate() {
    return myTemplate;
  }

  @Override
  public char getTemplateShortcut() {
    return TemplateSettings.getInstance().getShortcutChar(myTemplate);
  }

  @Override
  public void handleInsert(@NotNull InsertionContext context) {
    startTemplate(context, myTemplate);
  }

  public static void startTemplate(InsertionContext context, @NotNull Template template) {
    context.getDocument().deleteString(context.getStartOffset(), context.getTailOffset());
    context.setAddCompletionChar(false);
    TemplateManager.getInstance(context.getProject()).startTemplate(context.getEditor(), template);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LiveTemplateLookupElementImpl element = (LiveTemplateLookupElementImpl)o;
    return Objects.equals(myTemplate, element.myTemplate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myTemplate);
  }
}
