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

package com.intellij.codeInsight.daemon.impl;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.ex.DocumentEx;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerEvent;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.DumbAwareRunnable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.util.Alarm;
import org.jetbrains.annotations.NotNull;

public class StatusBarUpdater implements Disposable {
  private final Project myProject;
  private final DumbAwareRunnable myUpdateStatusRunnable = new DumbAwareRunnable() {
    @Override
    public void run() {
      if (!myProject.isDisposed()) {
        updateStatus();
      }
    }
  };
  private final Alarm updateStatusAlarm = new Alarm();

  StatusBarUpdater(Project project) {
    myProject = project;

    project.getMessageBus().connect(this).subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(@NotNull FileEditorManagerEvent event) {
        updateLater();
      }
    });

    project.getMessageBus().connect(this).subscribe(DaemonCodeAnalyzer.DAEMON_EVENT_TOPIC, new DaemonCodeAnalyzer.DaemonListener() {
      @Override
      public void daemonFinished() {
        updateLater();
      }
    });
  }

  private void updateLater() {
    final Application application = ApplicationManager.getApplication();
    if (application.isUnitTestMode()) {
      myUpdateStatusRunnable.run();
    }
    else {
      updateStatusAlarm.cancelAllRequests();
      updateStatusAlarm.addRequest(myUpdateStatusRunnable, 100);
    }
  }

  @Override
  public void dispose() {
  }

  private static final HighlightSeverity MIN = new HighlightSeverity("min", HighlightSeverity.INFORMATION.myVal + 1);
  private void updateStatus() {
    Editor editor = FileEditorManager.getInstance(myProject).getSelectedTextEditor();
    if (editor == null || !editor.getContentComponent().hasFocus()){
      return;
    }

    final Document document = editor.getDocument();
    if (document instanceof DocumentEx && ((DocumentEx)document).isInBulkUpdate()) return;

    int offset = editor.getCaretModel().getOffset();
    DaemonCodeAnalyzer codeAnalyzer = DaemonCodeAnalyzer.getInstance(myProject);
    HighlightInfo info = ((DaemonCodeAnalyzerImpl)codeAnalyzer).findHighlightByOffset(document, offset, false, MIN);
    String text = info != null && info.getDescription() != null ? info.getDescription() : "";

    StatusBar statusBar = WindowManager.getInstance().getStatusBar(editor.getContentComponent(), myProject);
    if (statusBar != null && !text.equals(statusBar.getInfo())) {
      statusBar.setInfo(text, "updater");
    }
  }
}
