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
package com.intellij.packaging.impl.ui;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.packaging.ui.PackagingElementPresentation;
import com.intellij.packaging.ui.TreeNodePresentation;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author nik
 */
public class DelegatedPackagingElementPresentation extends PackagingElementPresentation {
  private final TreeNodePresentation myDelegate;

  public DelegatedPackagingElementPresentation(TreeNodePresentation delegate) {
    myDelegate = delegate;
  }

  @Override
  public String getPresentableName() {
    return myDelegate.getPresentableName();
  }

  @Override
  public String getSearchName() {
    return myDelegate.getSearchName();
  }

  @Override
  public void render(@NotNull PresentationData presentationData, SimpleTextAttributes mainAttributes, SimpleTextAttributes commentAttributes) {
    myDelegate.render(presentationData, mainAttributes, commentAttributes);
  }

  @Override
  @Nullable
  public String getTooltipText() {
    return myDelegate.getTooltipText();
  }

  @Override
  public boolean canNavigateToSource() {
    return myDelegate.canNavigateToSource();
  }

  @Override
  public void navigateToSource() {
    myDelegate.navigateToSource();
  }

  @Override
  public int getWeight() {
    return myDelegate.getWeight();
  }
}
