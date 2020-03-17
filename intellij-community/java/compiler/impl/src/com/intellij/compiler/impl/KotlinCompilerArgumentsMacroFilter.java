// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.compiler.impl;

import com.intellij.openapi.application.PathMacroFilter;
import com.intellij.util.xmlb.Constants;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

final class KotlinCompilerArgumentsMacroFilter extends PathMacroFilter {
  @Override
  public boolean recursePathMacros(@NotNull Attribute attribute) {
    if (!attribute.getName().equals(Constants.VALUE)) return false;

    Element optionTag = attribute.getParent();
    if (optionTag == null ||
        !Constants.OPTION.equals(optionTag.getName()) ||
        !"additionalArguments".equals(optionTag.getAttributeValue(Constants.NAME))) {
      return false;
    }

    Element compilerSettingsTag = optionTag.getParentElement();
    if (compilerSettingsTag == null || !compilerSettingsTag.getName().equals("compilerSettings")) {
      return false;
    }
    Element configurationTag = compilerSettingsTag.getParentElement();
    if (configurationTag == null || !configurationTag.getName().equals("configuration")) {
      return false;
    }

    Element facetTag = configurationTag.getParentElement();
    return facetTag != null && facetTag.getName().equals("facet");
  }
}
