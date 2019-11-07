// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.ProjectTopics;
import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.CacheUpdateRunner;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.impl.ProjectLifecycleListener;
import com.intellij.openapi.roots.CollectingContentIterator;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class UnindexedFilesUpdater extends DumbModeTask {
  private static final Logger LOG = Logger.getInstance(UnindexedFilesUpdater.class);

  private final FileBasedIndexImpl myIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
  private final Project myProject;
  private final PushedFilePropertiesUpdater myPusher;

  public UnindexedFilesUpdater(final Project project) {
    myProject = project;
    myPusher = PushedFilePropertiesUpdater.getInstance(myProject);
    project.getMessageBus().connect(this).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        DumbService.getInstance(project).cancelTask(UnindexedFilesUpdater.this);
      }
    });
  }

  private void updateUnindexedFiles(ProgressIndicator indicator) {
    if (!IndexInfrastructure.hasIndices()) return;

    PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
    myPusher.pushAllPropertiesNow();
    boolean trackResponsiveness = !ApplicationManager.getApplication().isUnitTestMode();

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Pushing properties");

    indicator.setIndeterminate(true);
    indicator.setText(IdeBundle.message("progress.indexing.scanning"));

    myIndex.clearIndicesIfNecessary();

    CollectingContentIterator finder = myIndex.createContentIterator();
    snapshot = PerformanceWatcher.takeSnapshot();

    myIndex.iterateIndexableFilesConcurrently(finder, myProject, indicator);

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Indexable file iteration");

    List<VirtualFile> files = finder.getFiles();

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      // full VFS refresh makes sense only after it's loaded, i.e. after scanning files to index is finished
      scheduleInitialVfsRefresh();
    }

    if (files.isEmpty()) {
      return;
    }

    snapshot = PerformanceWatcher.takeSnapshot();

    if (trackResponsiveness) LOG.info("Unindexed files update started: " + files.size() + " files to update");

    indicator.setIndeterminate(false);
    indicator.setText(IdeBundle.message("progress.indexing.updating"));

    indexFiles(indicator, files);

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Unindexed files update");
  }

  private void scheduleInitialVfsRefresh() {
    ProjectRootManagerEx.getInstanceEx(myProject).markRootsForRefresh();

    Application app = ApplicationManager.getApplication();
    if (!app.isCommandLine()) {
      long sessionId = VirtualFileManager.getInstance().asyncRefresh(null);
      MessageBusConnection connection = app.getMessageBus().connect();
      connection.subscribe(ProjectLifecycleListener.TOPIC, new ProjectLifecycleListener() {
        @Override
        public void afterProjectClosed(@NotNull Project project) {
          if (project == myProject) {
            RefreshQueue.getInstance().cancelSession(sessionId);
            connection.disconnect();
          }
        }
      });
    }
    else {
      VirtualFileManager.getInstance().syncRefresh();
    }
  }

  private void indexFiles(ProgressIndicator indicator, List<VirtualFile> files) {
    CacheUpdateRunner.processFiles(indicator, files, myProject, content -> myIndex.indexFileContent(myProject, content));
  }

  @Override
  public void performInDumbMode(@NotNull ProgressIndicator indicator) {
    myIndex.filesUpdateStarted(myProject);
    try {
      updateUnindexedFiles(indicator);
    }
    catch (ProcessCanceledException e) {
      LOG.info("Unindexed files update canceled");
      throw e;
    }
    finally {
      myIndex.filesUpdateFinished(myProject);
    }
  }
}