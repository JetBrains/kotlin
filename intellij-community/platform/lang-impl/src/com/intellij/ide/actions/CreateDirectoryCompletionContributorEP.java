// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.openapi.extensions.RequiredElement;
import com.intellij.serviceContainer.BaseKeyedLazyInstance;
import com.intellij.util.xmlb.annotations.Attribute;
import org.jetbrains.annotations.Nullable;

public final class CreateDirectoryCompletionContributorEP extends BaseKeyedLazyInstance<CreateDirectoryCompletionContributor> {
  @Attribute("implementationClass")
  @RequiredElement
  public String implementationClass;

  @Nullable
  @Override
  protected String getImplementationClassName() {
    return implementationClass;
  }
}
