// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.configuration;

import com.intellij.openapi.application.PathMacroFilter;
import com.intellij.util.xmlb.Constants;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * @author yole
 */
class RunConfigurationPathMacroFilter extends PathMacroFilter {
  @Override
  public boolean skipPathMacros(@NotNull Attribute attribute) {
    return attribute.getName().equals(Constants.NAME) && attribute.getParent().getName().equals("configuration");
  }

  @Override
  public boolean recursePathMacros(@NotNull Attribute attribute) {
    final Element parent = attribute.getParent();
    if (parent != null && Constants.OPTION.equals(parent.getName())) {
      final Element grandParent = parent.getParentElement();
      return grandParent != null && "configuration".equals(grandParent.getName());
    }
    return false;
  }
}
