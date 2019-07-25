// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.execution.actions;

import com.intellij.execution.ExecutionBundle;
import com.intellij.execution.Executor;
import com.intellij.execution.KillableProcess;
import com.intellij.execution.impl.ExecutionManagerImpl;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.ui.popup.Balloon;
import com.intellij.openapi.ui.popup.BalloonBuilder;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.scale.JBUIScale;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ShowRunningListAction extends AnAction {
  public ShowRunningListAction() {
    super(ExecutionBundle.message("show.running.list.action.name"), ExecutionBundle.message("show.running.list.action.description"), null);
  }

  @Override
  public void actionPerformed(@NotNull final AnActionEvent e) {
    final Project project = e.getProject();
    if (project == null || project.isDisposed()) return;
    final Ref<Pair<? extends JComponent, String>> stateRef = new Ref<>();
    final Ref<Balloon> balloonRef = new Ref<>();

    final Timer timer = UIUtil.createNamedTimer("runningLists", 250);
    ActionListener actionListener = new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent actionEvent) {
        Balloon balloon = balloonRef.get();
        if (project.isDisposed() || (balloon != null && balloon.isDisposed())) {
          timer.stop();
          return;
        }
        ArrayList<Project> projects = new ArrayList<>(Arrays.asList(ProjectManager.getInstance().getOpenProjects()));
        //List should begin with current project
        projects.remove(project);
        projects.add(0, project);
        Pair<? extends JComponent, String> state = getCurrentState(projects);

        Pair<? extends JComponent, String> prevState = stateRef.get();
        if (prevState != null && prevState.getSecond().equals(state.getSecond())) return;
        stateRef.set(state);

        BalloonBuilder builder = JBPopupFactory.getInstance().createBalloonBuilder(state.getFirst());
        builder.setShowCallout(false)
          .setTitle(ExecutionBundle.message("show.running.list.balloon.title"))
          .setBlockClicksThroughBalloon(true)
          .setDialogMode(true)
          .setHideOnKeyOutside(false);
        IdeFrame frame = e.getData(IdeFrame.KEY);
        if (frame == null) {
          frame = WindowManagerEx.getInstanceEx().getFrame(project);
        }
        if (balloon != null) {
          balloon.hide();
        }
        builder.setClickHandler(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof MouseEvent) {
              MouseEvent mouseEvent = (MouseEvent)e.getSource();
              Component component = mouseEvent.getComponent();
              component = SwingUtilities.getDeepestComponentAt(component, mouseEvent.getX(), mouseEvent.getY());
              Object value = ((JComponent)component).getClientProperty(KEY);
              if (value instanceof Trinity) {
                Project aProject = (Project)((Trinity)value).first;
                JFrame aFrame = WindowManager.getInstance().getFrame(aProject);
                if (aFrame != null && !aFrame.isActive()) {
                  IdeFocusManager.getGlobalInstance().doWhenFocusSettlesDown(() -> IdeFocusManager.getGlobalInstance().requestFocus(aFrame, true));
                }
                ExecutionManagerImpl.getInstance(aProject).getContentManager().
                  toFrontRunContent((Executor)((Trinity)value).second, (RunContentDescriptor)((Trinity)value).third);
              }
            }
          }
        }, false);
        balloon = builder.createBalloon();

        balloonRef.set(balloon);
        JComponent component = frame.getComponent();
        RelativePoint point = new RelativePoint(component, new Point(component.getWidth(), 0));
        balloon.show(point, Balloon.Position.below);
      }
    };
    timer.addActionListener(actionListener);
    timer.setInitialDelay(0);
    timer.start();
  }

  private static final Object KEY = new Object();

  private static Pair<? extends JComponent, String> getCurrentState(@NotNull List<? extends Project> projects) {
    NonOpaquePanel panel = new NonOpaquePanel(new GridLayout(0, 1, 10, 10));
    StringBuilder state = new StringBuilder();
    for (int i = 0; i < projects.size(); i++) {
      Project project = projects.get(i);
      final ExecutionManagerImpl executionManager = ExecutionManagerImpl.getInstance(project);
      List<RunContentDescriptor> runningDescriptors = executionManager.getRunningDescriptors(Condition.TRUE);

      if (!runningDescriptors.isEmpty() && projects.size() > 1) {
        state.append(project.getName());
        panel.add(new JLabel("<html><body><b>Project '" + project.getName() + "'</b></body></html>"));
      }

      for (RunContentDescriptor descriptor : runningDescriptors) {
        Set<Executor> executors = executionManager.getExecutors(descriptor);
        for (Executor executor : executors) {
          state.append(System.identityHashCode(descriptor.getAttachedContent())).append("@")
            .append(System.identityHashCode(executor.getIcon())).append(";");
          ProcessHandler processHandler = descriptor.getProcessHandler();
          Icon icon = (processHandler instanceof KillableProcess && processHandler.isProcessTerminating())
                      ? AllIcons.Debugger.KillProcess
                      : executor.getIcon();
                    HyperlinkLabel label = new HyperlinkLabel(descriptor.getDisplayName());
          label.setIcon(icon);
          label.setIconTextGap(JBUIScale.scale(2));
          label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
          label.putClientProperty(KEY, Trinity.create(project, executor, descriptor));
          panel.add(label);
        }
      }
    }
    if (panel.getComponentCount() == 0) {
      panel.setBorder(JBUI.Borders.empty(10));
      panel.add(new JLabel(ExecutionBundle.message("show.running.list.balloon.nothing"), SwingConstants.CENTER));
    }
    else {
      panel.setBorder(JBUI.Borders.empty(10, 10, 0, 10));
      JLabel label = new JLabel(ExecutionBundle.message("show.running.list.balloon.hint"));
      label.setFont(JBUI.Fonts.miniFont());
      panel.add(label);
    }

    return Pair.create(panel, state.toString());
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project[] projects = ProjectManager.getInstance().getOpenProjects();
    for (Project project : projects) {
      boolean enabled = project != null && !project.isDisposed()
                        && !ExecutionManagerImpl.getInstance(project).getRunningDescriptors(Condition.TRUE).isEmpty();
      e.getPresentation().setEnabled(enabled);
      if (enabled) break;
    }
  }
}
