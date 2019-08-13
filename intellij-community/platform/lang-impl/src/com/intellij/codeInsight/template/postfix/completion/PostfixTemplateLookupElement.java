// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.completion;

import com.intellij.codeInsight.lookup.LookupElementPresentation;
import com.intellij.codeInsight.template.CustomTemplateCallback;
import com.intellij.codeInsight.template.impl.CustomLiveTemplateLookupElement;
import com.intellij.codeInsight.template.postfix.templates.PostfixLiveTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplate;
import com.intellij.codeInsight.template.postfix.templates.PostfixTemplateProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

public class PostfixTemplateLookupElement extends CustomLiveTemplateLookupElement {
  @NotNull
  private final PostfixTemplate myTemplate;
  @NotNull
  private final String myTemplateKey;
  @NotNull
  private final PostfixTemplateProvider myProvider;


  public PostfixTemplateLookupElement(@NotNull PostfixLiveTemplate liveTemplate,
                                      @NotNull PostfixTemplate postfixTemplate,
                                      @NotNull String templateKey,
                                      @NotNull PostfixTemplateProvider provider,
                                      boolean sudden) {
    super(liveTemplate, templateKey, StringUtil.trimStart(templateKey, "."), postfixTemplate.getDescription(), sudden, true);
    myTemplate = postfixTemplate;
    myTemplateKey = templateKey;
    myProvider = provider;
  }

  @NotNull
  public PostfixTemplate getPostfixTemplate() {
    return myTemplate;
  }

  @NotNull
  public PostfixTemplateProvider getProvider() {
    return myProvider;
  }

  @Override
  public void renderElement(LookupElementPresentation presentation) {
    super.renderElement(presentation);
    if (sudden) {
      presentation.setTailText(" " + UIUtil.rightArrow() + " " + myTemplate.getExample());
    }
    else {
      presentation.setTypeText(myTemplate.getExample());
      presentation.setTypeGrayed(true);
    }
  }

  @Override
  public void expandTemplate(@NotNull Editor editor, @NotNull PsiFile file) {
    PostfixLiveTemplate.expandTemplate(myTemplateKey, new CustomTemplateCallback(editor, file), editor, myProvider, myTemplate);
  }
}
