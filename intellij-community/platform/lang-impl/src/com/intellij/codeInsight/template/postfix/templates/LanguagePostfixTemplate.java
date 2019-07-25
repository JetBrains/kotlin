// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.postfix.templates;

import com.intellij.lang.LanguageExtension;
import com.intellij.lang.LanguageExtensionPoint;
import com.intellij.openapi.extensions.ExtensionPointName;

public class LanguagePostfixTemplate extends LanguageExtension<PostfixTemplateProvider> {
  public static final ExtensionPointName<LanguageExtensionPoint> EP_NAME = new ExtensionPointName<>("com.intellij.codeInsight.template.postfixTemplateProvider");
  public static final LanguagePostfixTemplate LANG_EP = new LanguagePostfixTemplate();

  private LanguagePostfixTemplate() {
    super(EP_NAME.getName());
  }
}
