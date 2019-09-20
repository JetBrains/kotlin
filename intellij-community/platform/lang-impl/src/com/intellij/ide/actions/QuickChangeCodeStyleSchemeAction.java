// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide.actions;

import com.intellij.icons.AllIcons;
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
      group.add(new AnAction("<Project>", "",
                             manager.USE_PER_PROJECT_SETTINGS ? AllIcons.Actions.Forward : ourNotCurrentAction) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          manager.updateSettingsTracker();
          manager.USE_PER_PROJECT_SETTINGS = true;
          CodeStyleSettingsManager.getInstance(project).fireCodeStyleSettingsChanged(null);
        }
      });
    }

    CodeStyleScheme currentScheme = CodeStyleSchemes.getInstance().getCurrentScheme();
    for (CodeStyleScheme scheme : CodeStyleSchemesImpl.getSchemeManager().getAllSchemes()) {
      addScheme(group, manager, currentScheme, scheme);
    }
  }

  private static void addScheme(final DefaultActionGroup group,
                                final CodeStyleSettingsManager manager,
                                final CodeStyleScheme currentScheme,
                                final CodeStyleScheme scheme) {
    group.add(new DumbAwareAction(scheme.getName(), "",
                                  scheme == currentScheme && !manager.USE_PER_PROJECT_SETTINGS ? AllIcons.Actions.Forward
                                                                                               : ourNotCurrentAction) {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        CodeStyleSchemes.getInstance().setCurrentScheme(scheme);
        manager.updateSettingsTracker();
        manager.USE_PER_PROJECT_SETTINGS = false;
        manager.PREFERRED_PROJECT_CODE_STYLE = scheme.getName();
        manager.fireCodeStyleSettingsChanged(null);
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
