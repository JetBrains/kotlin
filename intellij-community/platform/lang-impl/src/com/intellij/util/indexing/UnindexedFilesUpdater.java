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
package com.intellij.util.indexing;

import com.intellij.ProjectTopics;
import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.CacheUpdateRunner;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.CollectingContentIterator;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * @author Eugene Zhuravlev
 */
public class UnindexedFilesUpdater extends DumbModeTask {
  private static final Logger LOG = Logger.getInstance("#com.intellij.util.indexing.UnindexedFilesUpdater");

  private final FileBasedIndexImpl myIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
  private final Project myProject;
  private final StartupManager myStartupManager;
  private final PushedFilePropertiesUpdater myPusher;

  public UnindexedFilesUpdater(final Project project) {
    myProject = project;
    myStartupManager = StartupManager.getInstance(myProject);
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

    CollectingContentIterator finder = myIndex.createContentIterator(indicator);
    snapshot = PerformanceWatcher.takeSnapshot();

    myIndex.iterateIndexableFilesConcurrently(finder, myProject, indicator);

    myIndex.filesUpdateEnumerationFinished();

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Indexable file iteration");

    List<VirtualFile> files = finder.getFiles();

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      // full VFS refresh makes sense only after it's loaded, i.e. after scanning files to index is finished
      ((StartupManagerImpl)myStartupManager).scheduleInitialVfsRefresh();
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