// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.inspection;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

public class ProblemsViewToolWindowFactory implements ToolWindowFactory, DumbAware {
  @Override
  public boolean shouldBeAvailable(@NotNull Project project) {
    return false;
  }

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    InspectionProblemsViewPanel panel = new InspectionProblemsViewPanel(project, InspectionProblemsView.getInstance(project).getPresentationHelper());
    Content content = ContentFactory.SERVICE.getInstance().createContent(panel, "", false);
    content.setCloseable(true);
    toolWindow.getContentManager().addContent(content);

    //toolWindow.setHelpId("reference.toolWindow.DartAnalysis");
    //((ToolWindowEx)toolWindow).setTitleActions(new AnalysisServerFeedbackAction());

    wakeup(project, false);
  }

  public static void wakeup(@NotNull Project project, boolean focus) {
    ToolWindow toolWindow = InspectionProblemsView.getInstance(project).getToolWindow();
    toolWindow.setAvailable(true, null);
    toolWindow.activate(null, focus);
  }
}
