/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.codeInsight.actions;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class LayoutDirectoryDialog extends LayoutProjectCodeDialog implements DirectoryFormattingOptions {
  public LayoutDirectoryDialog(@NotNull Project project,
                               String title,
                               String text,
                               boolean enableOnlyVCSChangedTextCb)
  {
    super(project, title, text, enableOnlyVCSChangedTextCb);
  }

  @Override
  protected boolean shouldShowIncludeSubdirsCb() {
    return true;
  }

  public void setEnabledIncludeSubdirsCb(boolean isEnabled) {
    myIncludeSubdirsCb.setEnabled(isEnabled);
  }

  public void setSelectedIncludeSubdirsCb(boolean isSelected) {
    myIncludeSubdirsCb.setSelected(isSelected);
  }

  @Override
  public boolean isIncludeSubdirectories() {
    return myIncludeSubdirsCb.isSelected();
  }

}
