// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.openapi.options.CompoundScheme;

public final class ToolsGroup<T extends Tool> extends CompoundScheme<T> {
  public ToolsGroup(final String name) {
    super(name);
  }

  void moveElementUp(T tool) {
    int index = myElements.indexOf(tool);
    myElements.remove(index);
    myElements.add(index - 1, tool);
  }

  void moveElementDown(T tool) {
    int index = myElements.indexOf(tool);
    myElements.remove(index);
    myElements.add(index + 1, tool);
  }
}
