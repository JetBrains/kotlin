// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.template.CustomLiveTemplateBase;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CustomLiveTemplateLookupElement extends LiveTemplateLookupElement {
  @NotNull private final CustomLiveTemplateBase myCustomLiveTemplate;

  @NotNull private final String myTemplateKey;
  @NotNull private final String myItemText;

  public CustomLiveTemplateLookupElement(@NotNull CustomLiveTemplateBase customLiveTemplate,
                                         @NotNull String templateKey,
                                         @NotNull String itemText,
                                         @Nullable String description,
                                         boolean sudden,
                                         boolean worthShowingInAutoPopup) {
    super(templateKey, description, sudden, worthShowingInAutoPopup);
    myCustomLiveTemplate = customLiveTemplate;
    myTemplateKey = templateKey;
    myItemText = itemText;
  }

  @NotNull
  @Override
  protected String getItemText() {
    return myItemText;
  }

  @NotNull
  public CustomLiveTemplateBase getCustomLiveTemplate() {
    return myCustomLiveTemplate;
  }

  @Override
  public char getTemplateShortcut() {
    return myCustomLiveTemplate.getShortcut();
  }

  @Override
  public void handleInsert(@NotNull InsertionContext context) {
    context.setAddCompletionChar(false);
    expandTemplate(context.getEditor(), context.getFile());
  }

  public void expandTemplate(@NotNull Editor editor, @NotNull PsiFile file) {
    myCustomLiveTemplate.expand(myTemplateKey, new CustomTemplateCallback(editor, file));
  }
}
