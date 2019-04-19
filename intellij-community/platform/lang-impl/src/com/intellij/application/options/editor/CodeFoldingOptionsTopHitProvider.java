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
package com.intellij.application.options.editor;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;

import javax.swing.*;

/**
 * @author Sergey.Malenkov
 */
public class CodeFoldingOptionsTopHitProvider extends EditorOptionsTopHitProviderBase.NoPrefix {
  private int myCount;

  @Override
  protected Configurable getConfigurable(Project project) {
    myCount = 0;
    return new CodeFoldingConfigurable();
  }

  @Override
  protected String getOptionName(JCheckBox checkbox) {
    String name = super.getOptionName(checkbox);
    if (name != null && 0 < myCount++) {
      name = "Collapse by default: " + name;
    }
    return name;
  }
}
