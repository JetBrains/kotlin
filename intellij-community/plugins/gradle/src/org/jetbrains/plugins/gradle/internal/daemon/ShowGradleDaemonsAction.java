// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.internal.daemon;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.statistics.GradleActionsUsagesCollector;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class ShowGradleDaemonsAction extends DumbAwareAction {

  private DaemonsUi myUi;

  public ShowGradleDaemonsAction() {
    super("Show Gradle Daemons");
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabled(myUi == null);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    final Project project = e.getProject();
    GradleActionsUsagesCollector.trigger(project, GradleActionsUsagesCollector.ActionID.showGradleDaemonsAction);
    myUi = new DaemonsUi(project) {
      @Override
      public void dispose() {
        myUi = null;
      }
    };
    List<DaemonState> daemonsStatus = GradleDaemonServices.getDaemonsStatus();
    myUi.show(daemonsStatus);
  }
}