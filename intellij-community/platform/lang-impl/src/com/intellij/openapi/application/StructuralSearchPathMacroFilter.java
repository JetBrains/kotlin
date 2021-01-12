// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application;

import org.jdom.Attribute;
import org.jetbrains.annotations.NotNull;

/**
 * Since SSR inspections can be stored in inspection profiles and loaded by users who don't have the SSR plugin installed, unfortunately
 * this must be in the platform and not in the SSR plugin.
 *
 * @author yole
 */
public class StructuralSearchPathMacroFilter extends PathMacroFilter {
  @Override
  public boolean skipPathMacros(@NotNull Attribute attribute) {
    final String parentName = attribute.getParent().getName();
    if ("replaceConfiguration".equals(parentName) || "searchConfiguration".equals(parentName)) {
      return true;
    }
    return false;
  }
}
