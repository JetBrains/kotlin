/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package com.intellij.ide.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.codeStyle.CodeStyleScheme;
import com.intellij.psi.codeStyle.CodeStyleSchemes;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.psi.impl.source.codeStyle.CodeStyleSchemesImpl;
import org.jetbrains.annotations.NotNull;

/**
 * @author max
 */
public class QuickChangeCodeStyleSchemeAction extends QuickSwitchSchemeAction {
  @Override
  protected void fillActions(Project project, @NotNull DefaultActionGroup group, @NotNull DataContext dataContext) {
    final CodeStyleSettingsManager manager = CodeStyleSettingsManager.getInstance(project);
    if (manager.getMainProjectCodeStyle() != null) {
      //noinspection HardCodedStringLiteral
      group.add(new AnAction("<project>", "",
                             manager.USE_PER_PROJECT_SETTINGS ? ourCurrentAction : ourNotCurrentAction) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          manager.USE_PER_PROJECT_SETTINGS = true;
          CodeStyleSettingsManager.getInstance(project).fireCodeStyleSettingsChanged(null);
        }
      });
    }

    CodeStyleScheme currentScheme = CodeStyleSchemes.getInstance().getCurrentScheme();
    for (CodeStyleScheme scheme : CodeStyleSchemesImpl.getSchemeManager().getAllSchemes()) {
      addScheme(group, manager, currentScheme, scheme, false, project);
    }
  }

  private static void addScheme(final DefaultActionGroup group,
                                final CodeStyleSettingsManager manager,
                                final CodeStyleScheme currentScheme,
                                final CodeStyleScheme scheme,
                                final boolean addScheme,
                                Project project) {
    group.add(new DumbAwareAction(scheme.getName(), "",
                                  scheme == currentScheme && !manager.USE_PER_PROJECT_SETTINGS ? ourCurrentAction : ourNotCurrentAction) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        if (addScheme) {
          CodeStyleSchemes.getInstance().addScheme(scheme);
        }
        CodeStyleSchemes.getInstance().setCurrentScheme(scheme);
        manager.USE_PER_PROJECT_SETTINGS = false;
        manager.PREFERRED_PROJECT_CODE_STYLE = scheme.getName();
        CodeStyleSettingsManager.getInstance(project).fireCodeStyleSettingsChanged(null);
      }
    });
  }

  @Override
  protected boolean isEnabled() {
    return !CodeStyleSchemesImpl.getSchemeManager().getAllSchemes().isEmpty();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setEnabled(e.getProject() != null);
  }
}
