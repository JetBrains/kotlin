/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.packaging.ui;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.SimpleTextAttributes;

/**
 * @author nik
 */
public abstract class TreeNodePresentation {
  public abstract String getPresentableName();

  public String getSearchName() {
    return getPresentableName();
  }

  public abstract void render(@NotNull PresentationData presentationData, SimpleTextAttributes mainAttributes,
                              SimpleTextAttributes commentAttributes);

  @Nullable
  public String getTooltipText() {
    return null;
  }

  public boolean canNavigateToSource() {
    return false;
  }

  public void navigateToSource() {
  }

  public abstract int getWeight();
}
