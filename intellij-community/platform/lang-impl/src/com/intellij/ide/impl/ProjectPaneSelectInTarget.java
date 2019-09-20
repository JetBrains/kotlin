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
package com.intellij.ide.impl;

import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInManager;
import com.intellij.ide.StandardTargetWeights;
import com.intellij.ide.projectView.impl.ProjectViewPane;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

public class ProjectPaneSelectInTarget extends ProjectViewSelectInTarget implements DumbAware {
  public ProjectPaneSelectInTarget(Project project) {
    super(project);
  }

  @Override
  public String toString() {
    return SelectInManager.PROJECT;
  }

  @Override
  public boolean isSubIdSelectable(String subId, SelectInContext context) {
    return canSelect(context);
  }

  @Override
  public String getMinorViewId() {
    return ProjectViewPane.ID;
  }

  @Override
  public float getWeight() {
    return StandardTargetWeights.PROJECT_WEIGHT;
  }

}
