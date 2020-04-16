// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.openapi.extensions.ExtensionPointName;

import java.util.ArrayList;
import java.util.List;

public abstract class ToolsProvider {
  public static final ExtensionPointName<ToolsProvider> EP_NAME = ExtensionPointName.create("com.intellij.toolsProvider");

  public abstract List<Tool> getTools();

  public static List<Tool> getAllTools() {
    List<Tool> result = new ArrayList<>();
    for (ToolsProvider provider : EP_NAME.getExtensions()) {
      result.addAll(provider.getTools());
    }

    return result;
  }
}
