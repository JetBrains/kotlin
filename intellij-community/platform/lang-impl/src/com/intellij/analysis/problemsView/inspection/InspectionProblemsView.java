// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.analysis.problemsView.inspection;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.icons.AllIcons;
import com.intellij.notification.*;
import com.intellij.notification.impl.NotificationSettings;
import com.intellij.notification.impl.NotificationsConfigurationImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.util.Collection;

/**
 * Control "Problems View" tool window
 */
@State(
  name = "InspectionProblemsView",
  storages = @Storage(StoragePathMacros.WORKSPACE_FILE)
)
public class InspectionProblemsView implements PersistentStateComponent<InspectionProblemsViewSettings> {
  private static final NotificationGroup NOTIFICATION_GROUP =
    NotificationGroup.toolWindowGroup(getToolWindowId(), getToolWindowId(), false);

  private final Project myProject;
  private final InspectionProblemsPresentationHelper myPresentationHelper;

  @NotNull
  private Icon myCurrentIcon = AllIcons.Toolwindows.NoEvents;
  private volatile boolean myAnalysisIsBusy;

  private Notification myNotification;
  private boolean myDisabledForSession;

  public InspectionProblemsView(@NotNull Project project) {
    myProject = project;
    myPresentationHelper = new InspectionProblemsPresentationHelper();
    myProject.getMessageBus().connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void fileOpened(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {
        updateCurrentFile();
      }

      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        updateCurrentFile();
      }

      @Override
      public void fileClosed(@NotNull final FileEditorManager source, @NotNull final VirtualFile file) {
        updateCurrentFile();
      }
    });
    Alarm updateIcon = new Alarm(project);
    myProject.getMessageBus().connect().subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new DaemonCodeAnalyzer.DaemonListener() {
      @Override
      public void daemonStarting(@NotNull Collection<? extends FileEditor> fileEditors) {
        myAnalysisIsBusy = true;
        updateIcon.cancelAllRequests();
        updateIcon.addRequest(()->updateIcon(), 200);
      }

      @Override
      public void daemonFinished(@NotNull Collection<? extends FileEditor> fileEditors) {
        myAnalysisIsBusy = false;
        updateIcon.cancelAllRequests();
        // replace all problems in the table with current file highlightings. needed to get rid of "stuck" stale problems, disappearance of which was not fired for some reason
        updateIcon.addRequest(()->setCurrentFile(getCurrentFile()), 0);
      }

      @Override
      public void daemonCancelEventOccurred(@NotNull String reason) {
        myAnalysisIsBusy = false;
        updateIcon.cancelAllRequests();
        updateIcon.addRequest(()->updateIcon(), 200);
      }
    });

    ApplicationManager.getApplication().invokeLater(this::updateCurrentFile, project.getDisposed());
  }

  private void updateCurrentFile() {
    FileEditor editor = FileEditorManager.getInstance(myProject).getSelectedEditor();
    VirtualFile file = editor == null ? null : editor.getFile();
    if (file == null && editor instanceof TextEditor) {
      Document document = ((TextEditor)editor).getEditor().getDocument();
      file = FileDocumentManager.getInstance().getFile(document);
    }
    setCurrentFile(file);
  }

  @NotNull
  InspectionProblemsPresentationHelper getPresentationHelper() {
    return myPresentationHelper;
  }

  ToolWindow getToolWindow() {
    return ToolWindowManager.getInstance(myProject).getToolWindow(getToolWindowId());
  }

  private InspectionProblemsViewPanel getProblemsViewPanel() {
    ToolWindow toolWindow = getToolWindow();
    Content content = toolWindow != null ? toolWindow.getContentManager().getContent(0) : null;
    return content != null ? (InspectionProblemsViewPanel)content.getComponent() : null;
  }

  void setHeaderText(@NotNull String headerText) {
    ToolWindow toolWindow = getToolWindow();
    Content content = toolWindow != null ? toolWindow.getContentManager().getContent(0) : null;
    if (content != null) {
      content.setDisplayName(headerText);
    }
  }

  void setToolWindowIcon(@NotNull Icon icon) {
    myCurrentIcon = icon;
    updateIcon();
  }

  private void updateIcon() {
    ToolWindow toolWindow = getToolWindow();
    if (toolWindow == null) return;

    if (myAnalysisIsBusy) {
      toolWindow.setIcon(ExecutionUtil.getLiveIndicator(myCurrentIcon));
    }
    else {
      toolWindow.setIcon(myCurrentIcon);
    }
  }

  public static InspectionProblemsView getInstance(@NotNull final Project project) {
    return ServiceManager.getService(project, InspectionProblemsView.class);
  }

  private VirtualFile getCurrentFile() {
    return myPresentationHelper.getCurrentFile();
  }

  public void showWarningNotification(@NotNull String title, @Nullable String content, @Nullable Icon icon) {
    showNotification(NotificationType.WARNING, title, content, icon, false);
  }

  public void showErrorNotificationTerse(@NotNull String title) {
    showNotification(NotificationType.ERROR, title, null, null, true);
  }

  public void showErrorNotification(@NotNull String title, @Nullable String content, @Nullable Icon icon) {
    showNotification(NotificationType.ERROR, title, content, icon, false);
  }

  private void clearNotifications() {
    if (myNotification != null) {
      myNotification.expire();
      myNotification = null;
    }
  }

  private static final String OPEN_DART_ANALYSIS_LINK = "open.dart.analysis";

  private void showNotification(@NotNull NotificationType notificationType,
                                @NotNull String title,
                                @Nullable String content,
                                @Nullable Icon icon,
                                boolean terse) {
    clearNotifications();

    if (myDisabledForSession) return;

    content = StringUtil.notNullize(content);
    if (!terse) {
      if (!content.endsWith("<br>")) content += "<br>";
      content += "<br><a href='disable.for.session'>Don't show for this session</a>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" +
                 "<a href='never.show.again'>Never show again</a>";
    }

    myNotification = NOTIFICATION_GROUP.createNotification(title, content, notificationType, new NotificationListener.Adapter() {
      @Override
      protected void hyperlinkActivated(@NotNull final Notification notification, @NotNull final HyperlinkEvent e) {
        notification.expire();

        if (OPEN_DART_ANALYSIS_LINK.equals(e.getDescription())) {
          ToolWindow toolWindow = getToolWindow();
          if (toolWindow != null) {
            toolWindow.activate(null);
          }
        }
        else if ("disable.for.session".equals(e.getDescription())) {
          myDisabledForSession = true;
        }
        else if ("never.show.again".equals(e.getDescription())) {
          NOTIFICATION_GROUP.createNotification("Warning disabled.",
                                                "You can enable it back in the <a href=''>Event Log</a> settings.",
                                                NotificationType.INFORMATION, new Adapter() {
              @Override
              protected void hyperlinkActivated(@NotNull Notification notification, @NotNull HyperlinkEvent e) {
                notification.expire();
                final ToolWindow toolWindow = EventLog.getEventLog(myProject);
                if (toolWindow != null) toolWindow.activate(null);
              }
            }).notify(myProject);

          final NotificationSettings oldSettings = NotificationsConfigurationImpl.getSettings(notification.getGroupId());
          NotificationsConfigurationImpl.getInstanceImpl().changeSettings(oldSettings.getGroupId(), NotificationDisplayType.NONE,
                                                                          oldSettings.isShouldLog(), oldSettings.isShouldReadAloud());
        }
      }
    });

    if (icon != null) {
      myNotification.setIcon(icon);
    }

    myNotification.notify(myProject);
  }

  @Override
  public InspectionProblemsViewSettings getState() {
    return myPresentationHelper.getSettings();
  }

  @Override
  public void loadState(@NotNull InspectionProblemsViewSettings state) {
    myPresentationHelper.setSettings(state);
  }

  private void setCurrentFile(@Nullable final VirtualFile file) {
    ApplicationManager.getApplication().assertIsDispatchThread();

    // Calling getProblemsViewPanel() here also ensures that the tool window contents becomes visible when Analysis server starts
    InspectionProblemsViewPanel panel = getProblemsViewPanel();
    if (panel != null) {
      myPresentationHelper.setCurrentFile(file);
      panel.setCurrentFile(file);
      panel.fireGroupingOrFilterChanged();
    }
  }

  @NotNull
  private static String getToolWindowId() {
    return "Problems View";
  }

  public static void selectProblemIfVisible(@NotNull Project project, @NotNull HighlightInfo highlightInfo) {
    // take care to not instantiate InspectionProblemsView/panel if the toolwindow is not visible because otherwise toolwindow spring into view unexpectedly
    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(getToolWindowId());
    if (toolWindow != null && toolWindow.isVisible()) {
      InspectionProblemsView view = getInstance(project);
      InspectionProblemsViewPanel panel = view.getProblemsViewPanel();
      if (panel != null) {
        VirtualFile file = view.getCurrentFile();
        HighlightingProblem problem = new HighlightingProblem(project, file, highlightInfo);
        panel.selectProblem(problem);
      }
    }
  }
}
