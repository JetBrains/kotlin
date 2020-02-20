// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.inspection;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.registry.RegistryValue;
import com.intellij.openapi.util.registry.RegistryValueListener;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ProblemsViewToolWindowFactory implements ToolWindowFactory, DumbAware {
  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    return readRegistryValue().asBoolean();
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    InspectionProblemsViewPanel panel = new InspectionProblemsViewPanel(project, InspectionProblemsView.getInstance(project).getPresentationHelper());
    Content content = ContentFactory.SERVICE.getInstance().createContent(panel, "", false);
    toolWindow.getContentManager().addContent(content);

    //toolWindow.setHelpId("reference.toolWindow.DartAnalysis");
    //((ToolWindowEx)toolWindow).setTitleActions(new AnalysisServerFeedbackAction());

    RegistryValue value = readRegistryValue();
    value.addListener(new RegistryValueListener() {
      @Override
      public void afterValueChanged(@NotNull RegistryValue value) {
        wakeup(project, value);
      }
    }, project);
    wakeup(project, value);
  }

  @NotNull
  private static RegistryValue readRegistryValue() {
    return Registry.get("highlighting.showProblemsView");
  }

  private static void wakeup(@NotNull Project project, @NotNull RegistryValue value) {
    boolean activate = value.asBoolean();
    ToolWindow toolWindow = InspectionProblemsView.getInstance(project).getToolWindow();
    toolWindow.setAvailable(activate, null);
    if (activate) {
      toolWindow.activate(null, false);
    }
  }
}
