// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.caches;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ex.ApplicationManagerEx;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.util.ProgressIndicatorUtils;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import com.intellij.util.ExceptionUtil;
import com.intellij.util.PathUtil;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.diagnostic.FileIndexingStatistics;
import com.intellij.util.indexing.diagnostic.IndexingJobStatistics;
import com.intellij.util.progress.SubTaskProgressIndicator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@ApiStatus.Internal
public final class IndexUpdateRunner {

  private static final Key<Boolean> FAILED_TO_INDEX = Key.create("FAILED_TO_INDEX");

  private static final CopyOnWriteArrayList<IndexingJob> ourIndexingJobs = new CopyOnWriteArrayList<>();

  private final FileBasedIndexImpl myFileBasedIndex;

  private final ExecutorService myIndexingExecutor;

  private final int myNumberOfIndexingThreads;

  private static final CachedFileLoadLimiter LOAD_LIMITER_FOR_USUAL_FILES = new MaxTotalSizeCachedFileLoadLimiter(16 * 1024 * 1024);

  private static final CachedFileLoadLimiter LOAD_LIMITER_FOR_LARGE_FILES = new UnlimitedSingleCachedFileLoadLimiter();

  public IndexUpdateRunner(@NotNull FileBasedIndexImpl fileBasedIndex,
                           @NotNull ExecutorService indexingExecutor,
                           int numberOfIndexingThreads) {
    myFileBasedIndex = fileBasedIndex;
    myIndexingExecutor = indexingExecutor;
    myNumberOfIndexingThreads = numberOfIndexingThreads;
  }

  @NotNull
  public IndexingJobStatistics indexFiles(@NotNull Project project,
                                          @NotNull Collection<VirtualFile> files,
                                          @NotNull ProgressIndicator indicator) {
    indicator.checkCanceled();
    indicator.setIndeterminate(false);

    CachedFileContentLoader contentLoader = new CurrentProjectHintedCachedFileContentLoader(project);
    CachedFileContentQueue queue =
      LimitedCachedFileContentQueue.createNonAppendableForFiles(files, LOAD_LIMITER_FOR_USUAL_FILES, contentLoader);
    LimitedCachedFileContentQueue queueOfLargeFiles =
      LimitedCachedFileContentQueue.createEmptyAppendable(LOAD_LIMITER_FOR_LARGE_FILES, contentLoader);
    IndexingJob indexingJob = new IndexingJob(project, queue, queueOfLargeFiles, indicator, new AtomicInteger(), files.size());
    ourIndexingJobs.add(indexingJob);

    try {
      while (!indexingJob.isOutdated()) {
        processIndexJobsWhileUserIsInactive(indicator);
        if (!indexingJob.isOutdated() &&
            !ApplicationManager.getApplication().isDispatchThread()) { // damn tests with totally different threading
          ProgressIndicatorUtils.yieldToPendingWriteActions();
        }
      }
    }
    finally {
      ourIndexingJobs.remove(indexingJob);
    }

    if (project.isDisposed()) {
      indicator.cancel();
    }

    indicator.checkCanceled();

    return indexingJob.myStatistics;
  }

  private void processIndexJobsWhileUserIsInactive(@NotNull ProgressIndicator indicator) throws ProcessCanceledException {
    Runnable indexingWorker = () -> indexFilesOfJobsOneByOneWhileUserIsInactive();

    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      //If the current thread has acquired the write lock, we can't grant it to worker threads, so we must do the work in the current thread.
      indexingWorker.run();
    } else {
      List<Future<?>> futures = new ArrayList<>(myNumberOfIndexingThreads);
      for (int i = 0; i < myNumberOfIndexingThreads; i++) {
        futures.add(myIndexingExecutor.submit(indexingWorker));
      }
      for (Future<?> future : futures) {
        ProgressIndicatorUtils.awaitWithCheckCanceled(future, indicator);
      }
    }
  }

  //Does not throw PCE.
  private void indexFilesOfJobsOneByOneWhileUserIsInactive() {
    Disposable disposable = Disposer.newDisposable();
    ProgressIndicator writeActionIndicator = ProgressIndicatorUtils.forceWriteActionPriority(new EmptyProgressIndicator(), disposable);
    try {
      while (!ourIndexingJobs.isEmpty()) {
        for (IndexingJob job : ourIndexingJobs) {
          if (job.isOutdated()) {
            ourIndexingJobs.remove(job);
            return;
          }
          try {
            indexOneFileOfJobIfUserIsInactive(job, writeActionIndicator);
          }
          catch (ProcessCanceledException e) {
            return;
          }
          if (job.isOutdated()) {
            ourIndexingJobs.remove(job);
            return;
          }
        }
      }
    }
    finally {
      Disposer.dispose(disposable);
    }
  }

  private void indexOneFileOfJobIfUserIsInactive(@NotNull IndexingJob indexingJob,
                                                 @NotNull ProgressIndicator writeActionIndicator) throws ProcessCanceledException {
    long contentLoadingStartTime = System.nanoTime();
    CachedFileContentToken token;
    try {
      token = loadNextContent(indexingJob, writeActionIndicator);
    }
    catch (TooLargeContentException e) {
      indexingJob.oneMoreFileProcessed();
      FileBasedIndexImpl.LOG.info("File: " + e.getFile().getUrl() + " is too large for indexing");
      return;
    }
    catch (FailedToLoadContentException e) {
      indexingJob.oneMoreFileProcessed();
      logFailedToLoadContentException(e);
      return;
    }
    finally {
      indexingJob.myStatistics.addContentLoadingTime(System.nanoTime() - contentLoadingStartTime);
    }

    if (token == null) {
      indexingJob.myIsFinished.set(true);
      return;
    }

    try {
      CachedFileContent fileContent = token.getContent();
      long indexingStartTime = System.nanoTime();
      try {
        indexOneFileOfJobIfUserIsInactive(indexingJob, writeActionIndicator, fileContent);
      } finally {
        indexingJob.myStatistics.addIndexingTime(System.nanoTime() - indexingStartTime);
      }
      token.release();
      indexingJob.oneMoreFileProcessed();
    }
    catch (ProcessCanceledException e) {
      token.pushBack();
      throw e;
    }
    catch (Throwable e) {
      token.release();
      indexingJob.oneMoreFileProcessed();
      ExceptionUtil.rethrow(e);
    }
  }

  @Nullable
  private CachedFileContentToken loadNextContent(@NotNull IndexingJob indexingJob,
                                                 @NotNull ProgressIndicator writeActionIndicator) throws FailedToLoadContentException,
                                                                                                         TooLargeContentException,
                                                                                                         ProcessCanceledException {
    writeActionIndicator.checkCanceled();
    CachedFileContentToken token;
    try {
      // Try to load content from the main queue.
      token = indexingJob.myFileContentQueue.loadNextContent(writeActionIndicator);
    }
    catch (TooLargeContentException e) {
      VirtualFile largeFile = e.getFile();
      if (myFileBasedIndex.isTooLarge(largeFile)) {
        throw e;
      }
      indexingJob.myQueueOfLargeFiles.addFileToLoad(largeFile);
      return indexingJob.myQueueOfLargeFiles.loadNextContent(writeActionIndicator);
    }

    if (token == null) {
      // All files from the main queue have been processed. There can be files in the large files queue.
      return indexingJob.myQueueOfLargeFiles.loadNextContent(writeActionIndicator);
    }
    return token;
  }

  private static void logFailedToLoadContentException(@NotNull FailedToLoadContentException e) {
    Throwable cause = e.getCause();
    VirtualFile file = e.getFile();
    String fileUrl = "File: " + file.getUrl();
    if (cause instanceof FileNotFoundException) {
      // It is possible to not observe file system change until refresh finish, we handle missed file properly anyway.
      FileBasedIndexImpl.LOG.debug(fileUrl, e);
    }
    else if (cause instanceof IndexOutOfBoundsException || cause instanceof InvalidVirtualFileAccessException) {
      FileBasedIndexImpl.LOG.info(fileUrl, e);
    }
    else {
      FileBasedIndexImpl.LOG.error(fileUrl, e);
    }
  }

  private void indexOneFileOfJobIfUserIsInactive(@NotNull IndexingJob indexingJob,
                                                 @NotNull ProgressIndicator writeActionIndicator,
                                                 @NotNull CachedFileContent fileContent) throws ProcessCanceledException {
    Project project = indexingJob.myProject;
    if (project.isDisposed() || writeActionIndicator.isCanceled() || indexingJob.myIndicator.isCanceled()) {
      throw new ProcessCanceledException();
    }

    indexingJob.setLocationBeingIndexed(fileContent.getVirtualFile());
    if (!fileContent.isDirectory() && !Boolean.TRUE.equals(fileContent.getUserData(FAILED_TO_INDEX))) {
      try {
        AtomicReference<FileIndexingStatistics> fileStatistics = new AtomicReference<>();
        Runnable readAction = () -> {
          if (project.isDisposed() || writeActionIndicator.isCanceled()) {
            throw new ProcessCanceledException();
          }
          FileIndexingStatistics statistics = ProgressManager.getInstance().runProcess(
            () -> myFileBasedIndex.indexFileContent(project, fileContent), writeActionIndicator
          );
          fileStatistics.set(statistics);
        };
        if (!ApplicationManagerEx.getApplicationEx().tryRunReadAction(readAction)) {
          throw new ProcessCanceledException();
        }
        if (fileStatistics.get() != null) {
          indexingJob.myStatistics.addFileStatistics(fileStatistics.get(), getFileType(fileContent));
        }
      }
      catch (ProcessCanceledException e) {
        throw e;
      }
      catch (Throwable e) {
        fileContent.putUserData(FAILED_TO_INDEX, Boolean.TRUE);
        FileBasedIndexImpl.LOG.error("Error while indexing " + fileContent.getVirtualFile().getPresentableUrl() + "\n" +
                                     "To reindex this file IDEA has to be restarted", e);
      }
    }
  }

  private static FileType getFileType(@NotNull CachedFileContent fileContent) {
    // TODO consider CachedFileType
    VirtualFile virtualFile = fileContent.getVirtualFile();
    byte[] fileBytes;
    try {
      fileBytes = fileContent.getBytes();
    }
    catch (IOException e) {
      // Must not happen but let's play safe.
      FileBasedIndexImpl.LOG.warn("Failed to record statistics for file " + virtualFile.getUrl() + ". " +
                                  "Its content wasn't loaded " + e.getMessage());
      return FileTypeManager.getInstance().getFileTypeByFile(fileContent.getVirtualFile());
    }

    return FileTypeManager.getInstance().getFileTypeByFile(virtualFile, fileBytes);
  }

  @NotNull
  private static String getPresentableLocationBeingIndexed(@NotNull Project project, @NotNull VirtualFile file) {
    VirtualFile actualFile;
    if (file.getFileSystem() instanceof ArchiveFileSystem) {
      actualFile = VfsUtil.getLocalFile(file);
    }
    else {
      actualFile = file.getParent() != null ? file.getParent() : file;
    }
    return FileUtil.toSystemDependentName(getProjectRelativeOrAbsolutePath(project, actualFile));
  }

  @NotNull
  private static String getProjectRelativeOrAbsolutePath(@NotNull Project project, @NotNull VirtualFile file) {
    String projectBase = project.getBasePath();
    if (StringUtil.isNotEmpty(projectBase)) {
      String filePath = file.getPath();
      if (FileUtil.isAncestor(projectBase, filePath, true)) {
        String projectDirName = PathUtil.getFileName(projectBase);
        String relativePath = FileUtil.getRelativePath(projectBase, filePath, '/');
        if (StringUtil.isNotEmpty(projectDirName) && StringUtil.isNotEmpty(relativePath)) {
          return projectDirName + "/" + relativePath;
        }
      }
    }
    return file.getPath();
  }

  private static class IndexingJob {
    final Project myProject;
    final CachedFileContentQueue myFileContentQueue;
    final LimitedCachedFileContentQueue myQueueOfLargeFiles;
    final ProgressIndicator myIndicator;
    final AtomicInteger myNumberOfFilesProcessed;
    final int myTotalFiles;
    final AtomicBoolean myIsFinished = new AtomicBoolean();
    final IndexingJobStatistics myStatistics = new IndexingJobStatistics();

    IndexingJob(@NotNull Project project,
                @NotNull CachedFileContentQueue queue,
                @NotNull LimitedCachedFileContentQueue queueOfLargeFiles,
                @NotNull ProgressIndicator indicator,
                @NotNull AtomicInteger numberOfFilesProcessed,
                int totalFiles) {
      myProject = project;
      myFileContentQueue = queue;
      myIndicator = indicator;
      myNumberOfFilesProcessed = numberOfFilesProcessed;
      myTotalFiles = totalFiles;
      myQueueOfLargeFiles = queueOfLargeFiles;
    }

    public void oneMoreFileProcessed() {
      double newFraction = myNumberOfFilesProcessed.incrementAndGet() / (double)myTotalFiles;
      try {
        myIndicator.setFraction(newFraction);
      }
      catch (Exception ignored) {
        //Unexpected here. A misbehaved progress indicator must not break our code flow.
      }
    }

    public void setLocationBeingIndexed(@NotNull VirtualFile virtualFile) {
      String presentableLocation = getPresentableLocationBeingIndexed(myProject, virtualFile);
      if (myIndicator instanceof SubTaskProgressIndicator) {
        myIndicator.setText(presentableLocation);
      }
      else {
        myIndicator.setText2(presentableLocation);
      }
    }

    private boolean isOutdated() {
      return myProject.isDisposed() || myIsFinished.get() || myIndicator.isCanceled();
    }
  }
}