/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.util.indexing;

import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.TransactionGuard;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.util.Processor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class FileBasedIndexProjectHandler implements IndexableFileSet, Disposable {
  private static final Logger LOG = Logger.getInstance(FileBasedIndexProjectHandler.class);

  private final FileBasedIndex myIndex;
  private final FileBasedIndexScanRunnableCollector myCollector;

  public FileBasedIndexProjectHandler(@NotNull Project project, FileBasedIndex index, FileBasedIndexScanRunnableCollector collector) {
    myIndex = index;
    myCollector = collector;

    if (ApplicationManager.getApplication().isInternal()) {
      project.getMessageBus().connect(this).subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {

        @Override
        public void exitDumbMode() {
          LOG.info("Has changed files: " + (createChangedFilesIndexingTask(project) != null) + "; project=" + project);
        }
      });
    }

    StartupManager startupManager = StartupManager.getInstance(project);
    if (startupManager != null) {
      startupManager.registerPreStartupActivity(() -> {
        PushedFilePropertiesUpdater.getInstance(project).initializeProperties();

        // schedule dumb mode start after the read action we're currently in
        TransactionGuard.submitTransaction(project, () -> {
          if (FileBasedIndex.getInstance() instanceof FileBasedIndexImpl) {
            DumbService.getInstance(project).queueTask(new UnindexedFilesUpdater(project));
          }
        });

        myIndex.registerIndexableSet(this, project);
        project.getMessageBus().connect(this).subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
          private boolean removed;

          @Override
          public void projectClosing(@NotNull Project eventProject) {
            if (eventProject == project && !removed) {
              removed = true;
              myIndex.removeIndexableSet(FileBasedIndexProjectHandler.this);
            }
          }
        });
      });
    }
  }

  @Override
  public boolean isInSet(@NotNull final VirtualFile file) {
    return myCollector.shouldCollect(file);
  }

  @Override
  public void iterateIndexableFilesIn(@NotNull final VirtualFile file, @NotNull final ContentIterator iterator) {
    VfsUtilCore.visitChildrenRecursively(file, new VirtualFileVisitor<Void>() {
      @Override
      public boolean visitFile(@NotNull VirtualFile file) {

        if (!isInSet(file)) return false;
        iterator.processFile(file);

        return true;
      }
    });
  }

  @Override
  public void dispose() {
    // done mostly for tests. In real life this is no-op, because the set was removed on project closing
    myIndex.removeIndexableSet(this);
  }

  @ApiStatus.Internal
  public static final int ourMinFilesToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesToStart", 20);
  private static final int ourMinFilesSizeToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesSizeToStart", 1048576);
  
  @Nullable
  public static DumbModeTask createChangedFilesIndexingTask(final Project project) {
    final FileBasedIndex i = FileBasedIndex.getInstance();
    if (!(i instanceof FileBasedIndexImpl) || !IndexInfrastructure.hasIndices()) {
      return null;
    }

    FileBasedIndexImpl index = (FileBasedIndexImpl)i;

    if (!mightHaveManyChangedFilesInProject(project, index)) {
      return null;
    }

    return new DumbModeTask(project.getComponent(FileBasedIndexProjectHandler.class)) {
      @Override
      public void performInDumbMode(@NotNull ProgressIndicator indicator) {
        long start = System.currentTimeMillis();
        Collection<VirtualFile> files = index.getFilesToUpdate(project);
        long calcDuration = System.currentTimeMillis() - start;

        indicator.setIndeterminate(false);
        indicator.setText(IdeBundle.message("progress.indexing.updating"));
        
        LOG.info("Reindexing refreshed files: " + files.size() + " to update, calculated in " + calcDuration + "ms");
        if (!files.isEmpty()) {
          PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
          reindexRefreshedFiles(indicator, files, project, index);
          snapshot.logResponsivenessSinceCreation("Reindexing refreshed files");
        }
      }

      @Override
      public String toString() {
        StringBuilder sampleOfChangedFilePathsToBeIndexed = new StringBuilder();
        
        index.processChangedFiles(project, new Processor<VirtualFile>() {
          int filesInProjectToBeIndexed;
          final String projectBasePath = project.getBasePath();
          
          @Override
          public boolean process(VirtualFile file) {
            if (filesInProjectToBeIndexed != 0) sampleOfChangedFilePathsToBeIndexed.append(", ");
            
            String filePath = file.getPath();
            String loggedPath = projectBasePath != null ? FileUtil.getRelativePath(projectBasePath, filePath, '/') : null;
            if (loggedPath == null) loggedPath = filePath;
            else loggedPath = "%project_path%/" + loggedPath;
            sampleOfChangedFilePathsToBeIndexed.append(loggedPath);
            
            return ++filesInProjectToBeIndexed < ourMinFilesToStartDumMode;
          }
        });
        return super.toString() + " [" + project + ", " + sampleOfChangedFilePathsToBeIndexed + "]";
      }
    };
  }

  private static boolean mightHaveManyChangedFilesInProject(Project project, FileBasedIndexImpl index) {
    long start = System.currentTimeMillis();
    return !index.processChangedFiles(project, new Processor<VirtualFile>() {
      int filesInProjectToBeIndexed;
      long sizeOfFilesToBeIndexed;

      @Override
      public boolean process(VirtualFile file) {
        ++filesInProjectToBeIndexed;
        if (file.isValid() && !file.isDirectory()) sizeOfFilesToBeIndexed += file.getLength();
        return filesInProjectToBeIndexed < ourMinFilesToStartDumMode &&
               sizeOfFilesToBeIndexed < ourMinFilesSizeToStartDumMode &&
               System.currentTimeMillis() < start + 100;
      }
    });
  }

  private static void reindexRefreshedFiles(ProgressIndicator indicator,
                                            Collection<VirtualFile> files,
                                            final Project project,
                                            final FileBasedIndexImpl index) {
    CacheUpdateRunner.processFiles(indicator, files, project, content -> index.processRefreshedFile(project, content));
  }
}
