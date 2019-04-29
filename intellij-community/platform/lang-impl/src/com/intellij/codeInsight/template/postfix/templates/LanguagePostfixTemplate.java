// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.lang.LanguageExtension;

public class LanguagePostfixTemplate extends LanguageExtension<PostfixTemplateProvider> {
  public static final LanguagePostfixTemplate LANG_EP = new LanguagePostfixTemplate();
  public static final String EP_NAME = "com.intellij.codeInsight.template.postfixTemplateProvider";

  private LanguagePostfixTemplate() {
    super(EP_NAME);
  }
}
