// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.LazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;

/**
 * @author yole
 */
public class QuoteHandlerEP extends AbstractExtensionPointBean {
  public static final ExtensionPointName<QuoteHandlerEP> EP_NAME = new ExtensionPointName<>("com.intellij.quoteHandler");

  // these must be public for scrambling compatibility
  @Attribute("fileType")
  public String fileType;
  @Attribute("className")
  public String className;

  private final LazyInstance<QuoteHandler> myHandler = new LazyInstance<QuoteHandler>() {
    @Override
    protected Class<QuoteHandler> getInstanceClass() throws ClassNotFoundException {
      return findClass(className);
    }
  };

  public QuoteHandler getHandler() {
    return myHandler.getValue();
  }
}