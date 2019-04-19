// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.application;

import org.jdom.Attribute;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

/**
 * Filter remote path in {@link com.intellij.util.PathMappingSettings.PathMapping}
 *
 * @author Svetlana.Zemlyanskaya
 */
public class PathMappingsMacroFilter extends PathMacroFilter {
  @Override
  public boolean skipPathMacros(@NotNull Attribute attribute) {
    final Element parent = attribute.getParent();
    if ("mapping".equals(parent.getName()) && "remote-root".equals(attribute.getName())) {
      return true;
    }
    return false;
  }
}
