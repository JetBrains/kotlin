// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.extensions.AbstractExtensionPointBean;
import com.intellij.openapi.util.LazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import com.intellij.openapi.extensions.RequiredElement;
import org.jetbrains.annotations.NotNull;

public class CreateDirectoryCompletionContributorEP extends AbstractExtensionPointBean {
  @Attribute("implementationClass")
  @RequiredElement
  public String implementationClass;

  private final LazyInstance<CreateDirectoryCompletionContributor> myHandler = new LazyInstance<CreateDirectoryCompletionContributor>() {
    @Override
    protected Class<CreateDirectoryCompletionContributor> getInstanceClass() {
      return findExtensionClass(implementationClass);
    }
  };

  @NotNull
  public CreateDirectoryCompletionContributor getImplementationClass() {
    return myHandler.getValue();
  }
}
