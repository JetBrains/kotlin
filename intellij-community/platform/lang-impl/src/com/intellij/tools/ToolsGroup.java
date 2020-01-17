// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.tools;

import com.intellij.openapi.options.CompoundScheme;

public final class ToolsGroup<T extends Tool> extends CompoundScheme<T> {
  public ToolsGroup(final String name) {
    super(name);
  }

  public void moveElementUp(final T tool) {
    int index = myElements.indexOf(tool);
    removeElement(tool);
    insertElement(tool, index - 1);
  }

  public void moveElementDown(final T tool) {
    int index = myElements.indexOf(tool);
    removeElement(tool);
    insertElement(tool, index + 1);
  }

  public void insertElement(T element, final int i) {
    if (!contains(element)) {
      myElements.add(i, element);
    }
  }
}
