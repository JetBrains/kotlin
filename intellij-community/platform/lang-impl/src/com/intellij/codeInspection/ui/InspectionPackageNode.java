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

package com.intellij.codeInspection.ui;

import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;

public class InspectionPackageNode extends InspectionTreeNode {
  private final String myPackageName;

  public InspectionPackageNode(@NotNull String packageName, InspectionTreeNode parent) {
    super(parent);
    myPackageName = packageName;
  }

  public String getPackageName() {
    return myPackageName;
  }

  @Override
  public Icon getIcon(boolean expanded) {
    return PlatformIcons.PACKAGE_ICON;
  }

  @Override
  public String getPresentableText() {
    return myPackageName;
  }
}
