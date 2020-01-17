// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.ide;

import com.intellij.AppTopics;
import com.intellij.ProjectTopics;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileDocumentManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.roots.GeneratedSourcesFilter;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.Alarm;
import com.intellij.util.SingleAlarm;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.util.*;
import java.util.concurrent.TimeUnit;

public final class GeneratedSourceFileChangeTrackerImpl extends GeneratedSourceFileChangeTracker {
  private final Project myProject;
  private final SingleAlarm myCheckingQueue;
  private final Set<VirtualFile> myFilesToCheck = Collections.synchronizedSet(new HashSet<>());
  private final Set<VirtualFile> myEditedGeneratedFiles = Collections.synchronizedSet(new HashSet<>());
  public static boolean IN_TRACKER_TEST;

  public GeneratedSourceFileChangeTrackerImpl(@NotNull Project project) {
    myProject = project;
    myCheckingQueue = new SingleAlarm(this::checkFiles, 500, Alarm.ThreadToUse.POOLED_THREAD, project);
  }

  @TestOnly
  void waitForAlarm(long timeout, @NotNull TimeUnit timeUnit) throws Exception {
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      throw new IllegalStateException("Must not wait for the alarm under write action");
    }
    myCheckingQueue.waitForAllExecuted(timeout, timeUnit);
  }

  @TestOnly
  public void cancelAllAndWait(long timeout, @NotNull TimeUnit timeUnit) throws Exception {
    myFilesToCheck.clear();
    myCheckingQueue.cancelAllRequests();
    waitForAlarm(timeout, timeUnit);
  }

  @Override
  public boolean isEditedGeneratedFile(@NotNull VirtualFile file) {
    return myEditedGeneratedFiles.contains(file);
  }

  static final class MyDocumentListener implements DocumentListener {
    @Override
    public void documentChanged(@NotNull DocumentEvent event) {
      if (isListenerInactive()) {
        return;
      }

      Project[] openProjects = ProjectUtil.getOpenProjects();
      if (openProjects.length == 0) {
        return;
      }

      VirtualFile file = FileDocumentManager.getInstance().getFile(event.getDocument());
      if (file == null) {
        return;
      }

      for (Project project : ProjectUtil.getOpenProjects()) {
        if (project.isDisposed()) {
          continue;
        }

        GeneratedSourceFileChangeTrackerImpl fileChangeTracker = (GeneratedSourceFileChangeTrackerImpl)getInstance(project);
        ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
        if (fileIndex.isInContent(file) || fileIndex.isInLibrary(file)) {
          fileChangeTracker.myFilesToCheck.add(file);
          fileChangeTracker.myCheckingQueue.cancelAndRequest();
          // don't stop, one file maybe in different projects
        }
      }
    }
  }

  private static boolean isListenerInactive() {
    return !IN_TRACKER_TEST && ApplicationManager.getApplication().isUnitTestMode();
  }

  static final class MyProjectManagerListener implements ProjectManagerListener {
    @Override
    public void projectOpened(@NotNull Project project) {
      if (isListenerInactive()) {
        return;
      }

      ((GeneratedSourceFileChangeTrackerImpl)getInstance(project)).projectOpened();
    }
  }

  private void projectOpened() {
    MessageBusConnection connection = myProject.getMessageBus().connect();
    connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, new FileDocumentManagerListener() {
      @Override
      public void fileContentReloaded(@NotNull VirtualFile file, @NotNull Document document) {
        myFilesToCheck.remove(file);
        if (myEditedGeneratedFiles.remove(file)) {
          EditorNotifications.getInstance(myProject).updateNotifications(file);
        }
      }
    });
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new FileEditorManagerListener() {
      @Override
      public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
        myEditedGeneratedFiles.remove(file);
      }
    });
    connection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        myFilesToCheck.addAll(myEditedGeneratedFiles);
        myEditedGeneratedFiles.clear();
        myCheckingQueue.cancelAndRequest();
      }
    });
  }

  private void checkFiles() {
    final VirtualFile[] files;
    synchronized (myFilesToCheck) {
      files = myFilesToCheck.toArray(VirtualFile.EMPTY_ARRAY);
      myFilesToCheck.clear();
    }
    if (files.length == 0) return;
    final List<VirtualFile> newEditedGeneratedFiles = new ArrayList<>();
    ReadAction.run(() -> {
      if (myProject.isDisposed()) return;
      for (VirtualFile file : files) {
        if (GeneratedSourcesFilter.isGeneratedSourceByAnyFilter(file, myProject)) {
          newEditedGeneratedFiles.add(file);
        }
      }
    });

    if (!newEditedGeneratedFiles.isEmpty()) {
      myEditedGeneratedFiles.addAll(newEditedGeneratedFiles);
      EditorNotifications.getInstance(myProject).updateAllNotifications();
    }
  }
}
