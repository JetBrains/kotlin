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
package com.intellij.ide.util.projectWizard.actions;

import com.intellij.ide.util.projectWizard.ProjectSettingsStepBase;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAware;
import com.intellij.platform.DirectoryProjectGenerator;
import org.jetbrains.annotations.NotNull;

public class ProjectSpecificAction extends DefaultActionGroup implements DumbAware {
  public ProjectSpecificAction(@NotNull final DirectoryProjectGenerator projectGenerator, final ProjectSettingsStepBase step) {
    this(projectGenerator, projectGenerator.getName(), step);
  }

  public ProjectSpecificAction(@NotNull final DirectoryProjectGenerator projectGenerator,
                               @NotNull final String name, final ProjectSettingsStepBase step) {
    super(name, true);
    getTemplatePresentation().setIcon(projectGenerator.getLogo());
    add(step);
  }
}
