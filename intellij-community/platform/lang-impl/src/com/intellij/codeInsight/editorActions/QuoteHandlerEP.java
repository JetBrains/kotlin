// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.editorActions;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.util.LazyInstance;
import com.intellij.util.KeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
public class QuoteHandlerEP extends AbstractExtensionPointBean implements KeyedLazyInstance<QuoteHandler> {
  public static final ExtensionPointName<KeyedLazyInstance<QuoteHandler>> EP_NAME = new ExtensionPointName<>("com.intellij.quoteHandler");

  // these must be public for scrambling compatibility
  @Attribute("fileType")
  public String fileType;
  @Attribute("className")
  public String className;

  private final LazyInstance<QuoteHandler> myHandler = new LazyInstance<QuoteHandler>() {
    @Override
    protected Class<QuoteHandler> getInstanceClass() {
      return findExtensionClass(className);
    }
  };

  @Override
  public String getKey() {
    return fileType;
  }

  @NotNull
  @Override
  public QuoteHandler getInstance() {
    return myHandler.getValue();
  }
}