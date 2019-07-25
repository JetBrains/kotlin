// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.moduleDependencies;

import com.intellij.analysis.AnalysisScope;
import com.intellij.analysis.AnalysisScopeBundle;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.PsiElement;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author anna
 */
public class ShowModuleDependenciesAction extends AnAction {
  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(e.getProject() != null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;

    Module[] modules = e.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    if (modules == null) {
      PsiElement element = e.getData(CommonDataKeys.PSI_FILE);
      Module module = element != null ? ModuleUtilCore.findModuleForPsiElement(element) : null;
      if (module != null && ModuleManager.getInstance(project).getModules().length > 1) {
        MyModuleOrProjectScope dlg = new MyModuleOrProjectScope(module.getName());
        if (!dlg.showAndGet()) {
          return;
        }
        if (!dlg.useProjectScope()) {
          modules = new Module[]{module};
        }
      }
    }

    ModulesDependenciesPanel panel = new ModulesDependenciesPanel(project, modules);
    AnalysisScope scope = modules != null ? new AnalysisScope(modules) : new AnalysisScope(project);
    Content content = ContentFactory.SERVICE.getInstance().createContent(panel, scope.getDisplayName(), false);
    content.setHelpId(ModulesDependenciesPanel.HELP_ID);
    content.setDisposer(panel);
    panel.setContent(content);
    DependenciesAnalyzeManager.getInstance(project).addContent(content);
  }

  private static class MyModuleOrProjectScope extends DialogWrapper {
    private final JRadioButton myProjectScope;
    private final JRadioButton myModuleScope;

    protected MyModuleOrProjectScope(String moduleName) {
      super(false);
      setTitle(AnalysisScopeBundle.message("module.dependencies.scope.dialog.title"));
      ButtonGroup group = new ButtonGroup();
      myProjectScope = new JRadioButton(AnalysisScopeBundle.message("module.dependencies.scope.dialog.project.button"));
      myModuleScope = new JRadioButton(AnalysisScopeBundle.message("module.dependencies.scope.dialog.module.button", moduleName));
      group.add(myProjectScope);
      group.add(myModuleScope);
      myProjectScope.setSelected(true);
      init();
    }

    @Override
    protected JComponent createCenterPanel() {
      JPanel panel = new JPanel(new GridLayout(2, 1));
      panel.add(myProjectScope);
      panel.add(myModuleScope);
      return panel;
    }

    public boolean useProjectScope() {
      return myProjectScope.isSelected();
    }
  }
}