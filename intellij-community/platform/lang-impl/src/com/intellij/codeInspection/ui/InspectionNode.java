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

package com.intellij.codeInspection.ui;

import com.intellij.codeInspection.ex.InspectionProfileImpl;
import com.intellij.codeInspection.ex.InspectionToolWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class InspectionNode extends InspectionTreeNode {
  @NotNull private final InspectionToolWrapper myToolWrapper;
  @NotNull private final InspectionProfileImpl myProfile;

  public InspectionNode(@NotNull InspectionToolWrapper toolWrapper,
                        @NotNull InspectionProfileImpl profile,
                        @NotNull InspectionTreeNode parent) {
    super(parent);
    myToolWrapper = toolWrapper;
    myProfile = profile;
  }

  @NotNull
  public InspectionToolWrapper getToolWrapper() {
    return myToolWrapper;
  }

  @Nullable
  @Override
  public String getTailText() {
    final String shortName = getToolWrapper().getShortName();
    return myProfile.getTools(shortName, null).isEnabled() ? null : "Disabled";
  }

  @Override
  public String getPresentableText() {
    return getToolWrapper().getDisplayName();
  }
}
