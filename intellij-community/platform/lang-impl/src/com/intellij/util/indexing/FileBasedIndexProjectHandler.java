// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.util.indexing;

import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.lightEdit.LightEdit;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.util.Disposer;
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

@Service
public final class FileBasedIndexProjectHandler implements IndexableFileSet {
  private static final Logger LOG = Logger.getInstance(FileBasedIndexProjectHandler.class);
  private final Project myProject;
  private final @NotNull ProjectFileIndex myProjectFileIndex;

  private boolean isRemoved;

  private FileBasedIndexProjectHandler(@NotNull Project project) {
    myProject = project;
    myProjectFileIndex = ProjectFileIndex.getInstance(myProject);
  }

  static final class FileBasedIndexProjectHandlerStartupActivity implements StartupActivity {
    FileBasedIndexProjectHandlerStartupActivity() {
      ApplicationManager.getApplication().getMessageBus().connect().subscribe(ProjectManager.TOPIC,  new ProjectManagerListener() {
        @Override
        public void projectClosing(@NotNull Project project) {
          FileBasedIndexProjectHandler handler = project.getServiceIfCreated(FileBasedIndexProjectHandler.class);
          if (handler != null && !handler.isRemoved) {
            handler.isRemoved = true;
            FileBasedIndex.getInstance().removeIndexableSet(handler);
          }
        }
      });
    }

    @Override
    public void runActivity(@NotNull Project project) {
      if (ApplicationManager.getApplication().isInternal()) {
        project.getMessageBus().connect().subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
          @Override
          public void exitDumbMode() {
            LOG.info("Has changed files: " + (createChangedFilesIndexingTask(project) != null) + "; project=" + project);
          }
        });
      }

      FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
      PushedFilePropertiesUpdater.getInstance(project).initializeProperties();

      // schedule dumb mode start after the read action we're currently in
      if (fileBasedIndex instanceof FileBasedIndexImpl) {
        DumbService.getInstance(project).queueTask(new UnindexedFilesUpdater(project, IndexInfrastructure.isIndexesInitializationSuspended()));
      }

      FileBasedIndexProjectHandler handler = project.getService(FileBasedIndexProjectHandler.class);
      fileBasedIndex.registerIndexableSet(handler, project);
      // done mostly for tests. In real life this is no-op, because the set was removed on project closing
      Disposer.register(project, () -> {
        handler.isRemoved = true;
        fileBasedIndex.removeIndexableSet(handler);
      });
    }
  }

  @Override
  public boolean isInSet(@NotNull final VirtualFile file) {
    if (LightEdit.owns(myProject)) {
      return false;
    }
    if (myProjectFileIndex.isInContent(file) || myProjectFileIndex.isInLibrary(file)) {
      return !FileTypeManager.getInstance().isFileIgnored(file);
    }
    return false;
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

  @ApiStatus.Internal
  public static final int ourMinFilesToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesToStart", 20);
  private static final int ourMinFilesSizeToStartDumMode = Registry.intValue("ide.dumb.mode.minFilesSizeToStart", 1048576);

  @Nullable
  public static DumbModeTask createChangedFilesIndexingTask(@NotNull Project project) {
    final FileBasedIndex i = FileBasedIndex.getInstance();
    if (!(i instanceof FileBasedIndexImpl) || !IndexInfrastructure.hasIndices()) {
      return null;
    }

    FileBasedIndexImpl index = (FileBasedIndexImpl)i;
    if (!mightHaveManyChangedFilesInProject(project, index)) {
      return null;
    }

    return new DumbModeTask(project.getService(FileBasedIndexProjectHandler.class)) {
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
          index.indexFiles(project, files, indicator);
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
}
