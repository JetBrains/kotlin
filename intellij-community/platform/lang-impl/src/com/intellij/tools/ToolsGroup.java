/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.tools;

import com.intellij.openapi.options.CompoundScheme;

public class ToolsGroup<T extends Tool> extends CompoundScheme<T> {
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
