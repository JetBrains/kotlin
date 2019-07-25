/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.application.options.colors.fileStatus;

import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vcs.FileStatus;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class FileStatusColorDescriptor {
  private final FileStatus myStatus;
  private Color myColor;
  private final Color myDefaultColor;

  public FileStatusColorDescriptor(@NotNull FileStatus fileStatus, Color color, Color defaultColor) {
    myStatus = fileStatus;
    myColor = color;
    myDefaultColor = defaultColor;
  }

  @NotNull
  public FileStatus getStatus() {
    return myStatus;
  }

  public Color getColor() {
    return myColor;
  }

  public void setColor(Color color) {
    myColor = color;
  }

  public boolean isDefault() {
    return Comparing.equal(myColor, myDefaultColor);
  }

  public void resetToDefault() {
    myColor = myDefaultColor;
  }

  public Color getDefaultColor() {
    return myDefaultColor;
  }
}
