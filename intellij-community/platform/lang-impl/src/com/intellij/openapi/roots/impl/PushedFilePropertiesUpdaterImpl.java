// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.roots.impl;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Attachment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.extensions.ExtensionPointListener;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.fileTypes.InternalFileType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressWrapper;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.file.impl.FileManagerImpl;
import com.intellij.ui.GuiUtils;
import com.intellij.util.MathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.function.Function;

public final class PushedFilePropertiesUpdaterImpl extends PushedFilePropertiesUpdater {
  private static final Logger LOG = Logger.getInstance(PushedFilePropertiesUpdater.class);

  private final Project myProject;

  private final Queue<Runnable> myTasks = new ConcurrentLinkedQueue<>();

  public PushedFilePropertiesUpdaterImpl(@NotNull Project project) {
    myProject = project;

    project.getMessageBus().connect().subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        if (LOG.isTraceEnabled()) {
          LOG
            .trace(new Throwable("Processing roots changed event (caused by file type change: " + event.isCausedByFileTypesChange() + ")"));
        }
        for (FilePropertyPusher<?> pusher : FilePropertyPusher.EP_NAME.getExtensionList()) {
          pusher.afterRootsChanged(project);
        }
      }
    });
    FilePropertyPusher.EP_NAME.addExtensionPointListener(new ExtensionPointListener<FilePropertyPusher<?>>() {
      @Override
      public void extensionAdded(@NotNull FilePropertyPusher<?> pusher, @NotNull PluginDescriptor pluginDescriptor) {
        queueFullUpdate();
      }
    }, project);
  }

  private void queueFullUpdate() {
    myTasks.clear();
    queueTasks(Arrays.asList(this::initializeProperties, () -> doPushAll(FilePropertyPusher.EP_NAME.getExtensionList(),
                                                                         ProjectFileScanner.EP_NAME.getExtensionList())));
  }

  @ApiStatus.Internal
  public void processAfterVfsChanges(@NotNull List<? extends VFileEvent> events) {
    List<Runnable> syncTasks = new ArrayList<>();
    List<Runnable> delayedTasks = new ArrayList<>();
    List<FilePropertyPusher<?>> filePushers = getFilePushers();

    for (VFileEvent event : events) {
      if (event instanceof VFileCreateEvent) {
        boolean isDirectory = ((VFileCreateEvent)event).isDirectory();
        List<FilePropertyPusher<?>> pushers = isDirectory ? FilePropertyPusher.EP_NAME.getExtensionList() : filePushers;

        if (!event.isFromRefresh()) {
          ContainerUtil.addIfNotNull(syncTasks, createRecursivePushTask(event, pushers));
        }
        else {
          FileType fileType = FileTypeRegistry.getInstance().getFileTypeByFileName(((VFileCreateEvent)event).getChildName());
          boolean isProjectOrWorkspaceFile = fileType instanceof InternalFileType ||
                                             VfsUtilCore.findContainingDirectory(((VFileCreateEvent)event).getParent(),
                                                                                 Project.DIRECTORY_STORE_FOLDER) != null;
          if (!isProjectOrWorkspaceFile) {
            ContainerUtil.addIfNotNull(delayedTasks, createRecursivePushTask(event, pushers));
          }
        }
      }
      else if (event instanceof VFileMoveEvent || event instanceof VFileCopyEvent) {
        VirtualFile file = getFile(event);
        if (file == null) continue;
        boolean isDirectory = file.isDirectory();
        List<FilePropertyPusher<?>> pushers = isDirectory ? FilePropertyPusher.EP_NAME.getExtensionList() : filePushers;
        for (FilePropertyPusher<?> pusher : pushers) {
          file.putUserData(pusher.getFileDataKey(), null);
        }
        ContainerUtil.addIfNotNull(syncTasks, createRecursivePushTask(event, pushers));
      }
    }
    boolean pushingSomethingSynchronously =
      !syncTasks.isEmpty() && syncTasks.size() < FileBasedIndexProjectHandler.ourMinFilesToStartDumMode;
    if (pushingSomethingSynchronously) {
      // push synchronously to avoid entering dumb mode in the middle of a meaningful write action
      // when only a few files are created/moved
      syncTasks.forEach(Runnable::run);
    }
    else {
      delayedTasks.addAll(syncTasks);
    }
    if (!delayedTasks.isEmpty()) {
      queueTasks(delayedTasks);
    }
    if (pushingSomethingSynchronously) {
      GuiUtils.invokeLaterIfNeeded(() -> scheduleDumbModeReindexingIfNeeded(), ModalityState.defaultModalityState());
    }
  }

  private static VirtualFile getFile(@NotNull VFileEvent event) {
    VirtualFile file = event.getFile();
    if (event instanceof VFileCopyEvent) {
      file = ((VFileCopyEvent)event).getNewParent().findChild(((VFileCopyEvent)event).getNewChildName());
    }
    return file;
  }

  @Override
  public void runConcurrentlyIfPossible(List<Runnable> tasks) {
    invokeConcurrentlyIfPossible(tasks);
  }

  @Override
  public void initializeProperties() {
    FilePropertyPusher.EP_NAME.forEachExtensionSafe(pusher -> {
      pusher.initExtra(myProject);
    });
  }

  @Override
  public void pushAllPropertiesNow() {
    performPushTasks();
    doPushAll(FilePropertyPusher.EP_NAME.getExtensionList(), ProjectFileScanner.EP_NAME.getExtensionList());
  }

  @Nullable
  private Runnable createRecursivePushTask(@NotNull VFileEvent event, @NotNull List<? extends FilePropertyPusher<?>> pushers) {
    List<ProjectFileScanner> scanners = ProjectFileScanner.EP_NAME.getExtensionList();
    if (pushers.isEmpty() && scanners.isEmpty()) {
      return null;
    }

    return () -> {
      // delay calling event.getFile() until background to avoid expensive VFileCreateEvent.getFile() in EDT
      VirtualFile dir = getFile(event);
      ProjectFileIndex fileIndex = ReadAction.compute(() -> ProjectFileIndex.getInstance(myProject));
      if (dir != null && ReadAction.compute(() -> fileIndex.isInContent(dir)) && !ProjectUtil.isProjectOrWorkspaceFile(dir)) {
        doPushRecursively(dir, pushers, scanners, fileIndex);
      }
    };
  }

  private void doPushRecursively(@NotNull VirtualFile dir,
                                 @NotNull List<? extends FilePropertyPusher<?>> pushers,
                                 @NotNull List<ProjectFileScanner> scanners,
                                 @NotNull ProjectFileIndex fileIndex) {
    List<ProjectFileScanner.ScanSession> sessions = ContainerUtil.map(scanners,
                                                                      visitor -> visitor.startSession(myProject, dir));
    fileIndex.iterateContentUnderDirectory(dir, fileOrDir -> {
      applyPushersToFile(fileOrDir, pushers, null);
      applyScannersToFile(fileOrDir, sessions);
      return true;
    });
  }

  private static void applyScannersToFile(@NotNull VirtualFile fileOrDir, @NotNull List<ProjectFileScanner.ScanSession> sessions) {
    for (ProjectFileScanner.ScanSession session : sessions) {
      try {
        session.visitFile(fileOrDir);
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Exception e) {
        LOG.error("Failed to visit file", e, new Attachment("filePath.txt", fileOrDir.getPath()));
      }
    }
  }

  private void queueTasks(@NotNull List<? extends Runnable> actions) {
    actions.forEach(myTasks::offer);
    DumbModeTask task = new DumbModeTask(this) {
      @Override
      public void performInDumbMode(@NotNull ProgressIndicator indicator) {
        indicator.setIndeterminate(true);
        indicator.setText(IndexingBundle.message("progress.indexing.scanning"));
        performPushTasks();
      }
    };
    myProject.getMessageBus().connect(task).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        DumbService.getInstance(myProject).cancelTask(task);
      }
    });
    FilePropertyPusher.EP_NAME.addChangeListener(() -> {
      DumbService.getInstance(myProject).cancelTask(task);
      queueFullUpdate();
    }, task);
    DumbService.getInstance(myProject).queueTask(task);
  }

  private void performPushTasks() {
    boolean hadTasks = false;
    while (true) {
      Runnable task = myTasks.poll();
      if (task == null) {
        break;
      }
      try {
        task.run();
        hadTasks = true;
      }
      catch (ProcessCanceledException e) {
        queueTasks(Collections.singletonList(task)); // reschedule dumb mode and ensure the canceled task is enqueued again
        throw e;
      }
    }

    if (hadTasks) {
      scheduleDumbModeReindexingIfNeeded();
    }
  }

  private void scheduleDumbModeReindexingIfNeeded() {
    if (myProject.isDisposed()) return;

    DumbModeTask task = FileBasedIndexProjectHandler.createChangedFilesIndexingTask(myProject);
    if (task != null) {
      DumbService.getInstance(myProject).queueTask(task);
    }
  }

  @Override
  public void filePropertiesChanged(@NotNull VirtualFile fileOrDir, @NotNull Condition<? super VirtualFile> acceptFileCondition) {
    if (fileOrDir.isDirectory()) {
      for (VirtualFile child : fileOrDir.getChildren()) {
        if (!child.isDirectory() && acceptFileCondition.value(child)) {
          filePropertiesChanged(child);
        }
      }
    }
    else if (acceptFileCondition.value(fileOrDir)) {
      filePropertiesChanged(fileOrDir);
    }
  }

  private static <T> T findNewPusherValue(Project project, VirtualFile fileOrDir, FilePropertyPusher<? extends T> pusher, T moduleValue) {
    //Do not check fileOrDir.getUserData() as it may be outdated.
    T immediateValue = pusher.getImmediateValue(project, fileOrDir);
    if (immediateValue != null) return immediateValue;
    if (moduleValue != null) return moduleValue;
    return findNewPusherValueFromParent(project, fileOrDir, pusher);
  }

  private static <T> T findNewPusherValueFromParent(Project project, VirtualFile fileOrDir, FilePropertyPusher<? extends T> pusher) {
    final VirtualFile parent = fileOrDir.getParent();
    if (parent != null && ProjectFileIndex.getInstance(project).isInContent(parent)) {
      final T userValue = parent.getUserData(pusher.getFileDataKey());
      if (userValue != null) return userValue;
      return findNewPusherValue(project, parent, pusher, null);
    }
    T projectValue = pusher.getImmediateValue(project, null);
    return projectValue != null ? projectValue : pusher.getDefaultValue();
  }

  @Override
  public void pushAll(FilePropertyPusher<?> @NotNull ... pushers) {
    queueTasks(Collections.singletonList(() -> doPushAll(Arrays.asList(pushers), ProjectFileScanner.EP_NAME.getExtensionList())));
  }

  private void doPushAll(@NotNull List<? extends FilePropertyPusher<?>> pushers, @NotNull List<ProjectFileScanner> scanners) {
    List<ProjectFileScanner.ScanSession> sessions = ContainerUtil.map(scanners,
                                                                      visitor -> visitor.startSession(myProject, null));
    scanProject(myProject, module -> {
      final Object[] moduleValues = new Object[pushers.size()];
      for (int i = 0; i < moduleValues.length; i++) {
        moduleValues[i] = pushers.get(i).getImmediateValue(module);
      }
      return fileOrDir -> {
        applyPushersToFile(fileOrDir, pushers, moduleValues);
        applyScannersToFile(fileOrDir, sessions);
        return ContentIteratorEx.Status.CONTINUE;
      };
    });
  }

  public static void scanProject(@NotNull Project project, @NotNull Function<Module, ContentIteratorEx> iteratorProducer) {
    Module[] modules = ReadAction.compute(() -> ModuleManager.getInstance(project).getModules());
    List<Runnable> tasks = ContainerUtil.mapNotNull(modules, module -> {
      return ReadAction.compute(() -> {
        if (module.isDisposed()) return null;
        ProgressManager.checkCanceled();
        ModuleFileIndex fileIndex = ModuleRootManager.getInstance(module).getFileIndex();
        ContentIteratorEx iterator = iteratorProducer.apply(module);
        return () -> fileIndex.iterateContent(iterator);
      });
    });
    invokeConcurrentlyIfPossible(tasks);
  }

  public static void invokeConcurrentlyIfPossible(@NotNull List<? extends Runnable> tasks) {
    if (tasks.isEmpty()) return;
    if (tasks.size() == 1 ||
        ApplicationManager.getApplication().isWriteAccessAllowed()) {
      for (Runnable r : tasks) r.run();
      return;
    }

    final ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();

    final ConcurrentLinkedQueue<Runnable> tasksQueue = new ConcurrentLinkedQueue<>(tasks);
    List<Future<?>> results = new ArrayList<>();
    if (tasks.size() > 1) {
      int numThreads = MathUtil.clamp(tasks.size() - 1, 1, UnindexedFilesUpdater.getNumberOfIndexingThreads() - 1);

      for (int i = 0; i < numThreads; ++i) {
        results.add(ApplicationManager.getApplication().executeOnPooledThread(() -> ProgressManager.getInstance().runProcess(() -> {
          Runnable runnable;
          while ((runnable = tasksQueue.poll()) != null) runnable.run();
        }, ProgressWrapper.wrap(progress))));
      }
    }

    Runnable runnable;
    while ((runnable = tasksQueue.poll()) != null) runnable.run();

    for (Future<?> result : results) {
      try {
        result.get();
      }
      catch (InterruptedException ex) {
        throw new ProcessCanceledException(ex);
      }
      catch (Exception ex) {
        LOG.error(ex);
      }
    }
  }

  private void applyPushersToFile(final VirtualFile fileOrDir,
                                  @NotNull List<? extends FilePropertyPusher<?>> pushers,
                                  final Object[] moduleValues) {
    if (pushers.isEmpty()) return;
    if (fileOrDir.isDirectory()) {
      fileOrDir.getChildren(); // outside read action to avoid freezes
    }

    ApplicationManager.getApplication().runReadAction(() -> {
      ProgressManager.checkCanceled();
      if (!fileOrDir.isValid()) return;
      doApplyPushersToFile(fileOrDir, pushers, moduleValues);
    });
  }

  private void doApplyPushersToFile(@NotNull VirtualFile fileOrDir,
                                    @NotNull List<? extends FilePropertyPusher<?>> pushers,
                                    Object @Nullable[] moduleValues) {
    final boolean isDir = fileOrDir.isDirectory();
    for (int i = 0; i < pushers.size(); i++) {
      //noinspection unchecked
      FilePropertyPusher<Object> pusher = (FilePropertyPusher<Object>)pushers.get(i);
      if (isDir
          ? !pusher.acceptsDirectory(fileOrDir, myProject)
          : pusher.pushDirectoriesOnly() || !pusher.acceptsFile(fileOrDir, myProject)) {
        continue;
      }
      Object value = moduleValues != null ? moduleValues[i] : null;
      findAndUpdateValue(fileOrDir, pusher, value);
    }
  }

  @Override
  public <T> void findAndUpdateValue(@NotNull VirtualFile fileOrDir, @NotNull FilePropertyPusher<T> pusher, @Nullable T moduleValue) {
    T newValue = findNewPusherValue(myProject, fileOrDir, pusher, moduleValue);
    T oldValue = fileOrDir.getUserData(pusher.getFileDataKey());
    if (newValue != oldValue) {
      fileOrDir.putUserData(pusher.getFileDataKey(), newValue);
      try {
        pusher.persistAttribute(myProject, fileOrDir, newValue);
      }
      catch (IOException e) {
        LOG.error(e);
      }
    }
  }

  @Override
  public void filePropertiesChanged(@NotNull final VirtualFile file) {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();
    if (fileBasedIndex instanceof FileBasedIndexImpl) {
      ((FileBasedIndexImpl) fileBasedIndex).requestReindex(file, false);
    }
    for (final Project project : ProjectManager.getInstance().getOpenProjects()) {
      reloadPsi(file, project);
    }
  }

  private static void reloadPsi(final VirtualFile file, final Project project) {
    final FileManagerImpl fileManager = (FileManagerImpl)PsiManagerEx.getInstanceEx(project).getFileManager();
    if (fileManager.findCachedViewProvider(file) != null) {
      GuiUtils.invokeLaterIfNeeded(() -> WriteAction.run(() -> fileManager.forceReload(file)),
                                   ModalityState.defaultModalityState(),
                                   project.getDisposed());
    }
  }

  private static List<FilePropertyPusher<?>> getFilePushers() {
    return ContainerUtil.findAll(FilePropertyPusher.EP_NAME.getExtensionList(), pusher -> !pusher.pushDirectoriesOnly());
  }
}
