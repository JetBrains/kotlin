// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing.contentQueue;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.InvalidVirtualFileAccessException;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.ArchiveFileSystem;
import com.intellij.util.PathUtil;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.diagnostic.FileIndexingStatistics;
import com.intellij.util.indexing.diagnostic.IndexingJobStatistics;
import com.intellij.util.indexing.diagnostic.TooLargeForIndexingFile;
import com.intellij.util.progress.SubTaskProgressIndicator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@ApiStatus.Internal
public final class IndexUpdateRunner {

  private static final long SOFT_MAX_TOTAL_BYTES_LOADED_INTO_MEMORY = 20 * FileUtilRt.MEGABYTE;

  private static final CopyOnWriteArrayList<IndexingJob> ourIndexingJobs = new CopyOnWriteArrayList<>();

  private final FileBasedIndexImpl myFileBasedIndex;

  private final ExecutorService myIndexingExecutor;

  private final int myNumberOfIndexingThreads;

  /**
   * Memory optimization to prevent OutOfMemory on loading file contents.
   * <p>
   * "Soft" total limit of bytes loaded into memory in the whole application is {@link #SOFT_MAX_TOTAL_BYTES_LOADED_INTO_MEMORY}.
   * It is "soft" because one (and only one) "indexable" file can exceed this limit.
   * <p>
   * "Indexable" file is any file for which {@link FileBasedIndexImpl#isTooLarge(VirtualFile)} returns {@code false}.
   * Note that this method may return {@code false} even for relatively big files with size greater than {@link #SOFT_MAX_TOTAL_BYTES_LOADED_INTO_MEMORY}.
   * This is because for some files (or file types) the size limit is ignored.
   * <p>
   * So in its maximum we will load {@code SOFT_MAX_TOTAL_BYTES_LOADED_INTO_MEMORY + <size of not "too large" file>}, which seems acceptable,
   * because we have to index this "not too large" file anyway (even if its size is 4 Gb), and {@code SOFT_MAX_TOTAL_BYTES_LOADED_INTO_MEMORY}
   * additional bytes are insignificant.
   */
  private static long ourTotalBytesLoadedIntoMemory = 0;
  private static final Lock ourLoadedBytesLimitLock = new ReentrantLock();
  private static final Condition ourLoadedBytesAreReleasedCondition = ourLoadedBytesLimitLock.newCondition();

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
    IndexingJob indexingJob = new IndexingJob(project, indicator, contentLoader, files);
    if (ApplicationManager.getApplication().isWriteAccessAllowed()) {
      // If the current thread has acquired the write lock, we can't grant it to worker threads, so we must do the work in the current thread.
      while (!indexingJob.areAllFilesProcessed()) {
        indexOneFileOfJob(indexingJob);
      }
    }
    else {
      ourIndexingJobs.add(indexingJob);
      try {
        AtomicInteger numberOfRunningWorkers = new AtomicInteger();
        Runnable worker = () -> {
          try {
            indexJobsFairly();
          }
          finally {
            numberOfRunningWorkers.decrementAndGet();
          }
        };
        for (int i = 0; i < myNumberOfIndexingThreads; i++) {
          myIndexingExecutor.execute(worker);
          numberOfRunningWorkers.incrementAndGet();
        }
        while (!project.isDisposed() && !indexingJob.areAllFilesProcessed() && indexingJob.myError.get() == null) {
          // Add a worker if the previous died for whatever reason, to avoid waiting for nothing.
          if (numberOfRunningWorkers.get() < myNumberOfIndexingThreads) {
            myIndexingExecutor.execute(worker);
            numberOfRunningWorkers.incrementAndGet();
          }
          indicator.checkCanceled();
          try {
            if (indexingJob.myAllFilesAreProcessedLatch.await(100, TimeUnit.MILLISECONDS)) {
              break;
            }
          }
          catch (InterruptedException e) {
            throw new ProcessCanceledException(e);
          }
        }
        Throwable error = indexingJob.myError.get();
        if (error instanceof ProcessCanceledException) {
          throw (ProcessCanceledException) error;
        }
        if (error != null) {
          throw new RuntimeException("Indexing of " + project.getName() + " has failed", error);
        }
      }
      finally {
        ourIndexingJobs.remove(indexingJob);
      }
    }
    return indexingJob.myStatistics;
  }

  // Index jobs one by one while there are some. Jobs may belong to different projects, and we index them fairly.
  // Drops finished, cancelled and failed jobs from {@code ourIndexingJobs}. Does not throw exceptions.
  private void indexJobsFairly() {
    while (!ourIndexingJobs.isEmpty()) {
      for (IndexingJob job : ourIndexingJobs) {
        if (job.myProject.isDisposed()
            || job.myNoMoreFilesInQueue.get()
            || job.myIndicator.isCanceled()
            || job.myError.get() != null) {
          ourIndexingJobs.remove(job);
          continue;
        }
        try {
          indexOneFileOfJob(job);
        }
        catch (Throwable e) {
          job.myError.compareAndSet(null, e);
          ourIndexingJobs.remove(job);
        }
      }
    }
  }

  private void indexOneFileOfJob(@NotNull IndexingJob indexingJob) throws ProcessCanceledException {
    long contentLoadingTime = System.nanoTime();
    ContentLoadingResult loadingResult;
    try {
      loadingResult = loadNextContent(indexingJob, indexingJob.myIndicator);
    }
    catch (TooLargeContentException e) {
      indexingJob.oneMoreFileProcessed();
      indexingJob.myStatistics.getNumberOfTooLargeForIndexingFiles().incrementAndGet();
      indexingJob.myStatistics.getTooLargeForIndexingFiles().addElement(new TooLargeForIndexingFile(e.getFile().getName(), e.getFile().getLength()));
      FileBasedIndexImpl.LOG.info("File: " + e.getFile().getUrl() + " is too large for indexing");
      return;
    }
    catch (FailedToLoadContentException e) {
      indexingJob.oneMoreFileProcessed();
      logFailedToLoadContentException(e);
      return;
    }
    finally {
      contentLoadingTime = System.nanoTime() - contentLoadingTime;
    }

    if (loadingResult == null) {
      indexingJob.myNoMoreFilesInQueue.set(true);
      return;
    }

    CachedFileContent fileContent = loadingResult.cachedFileContent;
    VirtualFile file = fileContent.getVirtualFile();
    try {
      indexingJob.setLocationBeingIndexed(file);
      if (!file.isDirectory()) {
        FileIndexingStatistics fileIndexingStatistics = ReadAction
          .nonBlocking(() -> myFileBasedIndex.indexFileContent(indexingJob.myProject, fileContent))
          .expireWith(indexingJob.myProject)
          .wrapProgress(indexingJob.myIndicator)
          .executeSynchronously();
        indexingJob.myStatistics.addFileStatistics(fileIndexingStatistics, contentLoadingTime, loadingResult.fileLength);
      }
      indexingJob.oneMoreFileProcessed();
    }
    catch (ProcessCanceledException e) {
      // Push back the file.
      indexingJob.myQueueOfFiles.add(file);
      throw e;
    }
    catch (Throwable e) {
      indexingJob.oneMoreFileProcessed();
      FileBasedIndexImpl.LOG.error("Error while indexing " + file.getPresentableUrl() + "\n" +
                                   "To reindex this file IDEA has to be restarted", e);
    }
    finally {
      signalThatFileIsUnloaded(loadingResult.fileLength);
    }
  }

  @Nullable
  private IndexUpdateRunner.ContentLoadingResult loadNextContent(@NotNull IndexingJob indexingJob,
                                                                 @NotNull ProgressIndicator indicator) throws FailedToLoadContentException,
                                                                                                              TooLargeContentException,
                                                                                                              ProcessCanceledException {
    VirtualFile file = indexingJob.myQueueOfFiles.poll();
    if (file == null) {
      return null;
    }
    if (myFileBasedIndex.isTooLarge(file)) {
      throw new TooLargeContentException(file);
    }
    try {
      long fileLength = file.getLength();
      waitForFreeMemoryToLoadFileContent(indicator, fileLength);
      CachedFileContent fileContent = indexingJob.myContentLoader.loadContent(file);
      return new ContentLoadingResult(fileContent, fileLength);
    }
    catch (ProcessCanceledException e) {
      indexingJob.myQueueOfFiles.add(file);
      throw e;
    }
  }

  private static class ContentLoadingResult {
    final @NotNull CachedFileContent cachedFileContent;
    final long fileLength;

    private ContentLoadingResult(@NotNull CachedFileContent cachedFileContent, long fileLength) {
      this.cachedFileContent = cachedFileContent;
      this.fileLength = fileLength;
    }
  }

  private static void waitForFreeMemoryToLoadFileContent(@NotNull ProgressIndicator indicator, long fileLength) {
    ourLoadedBytesLimitLock.lock();
    try {
      while (ourTotalBytesLoadedIntoMemory >= SOFT_MAX_TOTAL_BYTES_LOADED_INTO_MEMORY) {
        indicator.checkCanceled();
        try {
          ourLoadedBytesAreReleasedCondition.await(100, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
          throw new ProcessCanceledException(e);
        }
      }
      ourTotalBytesLoadedIntoMemory += fileLength;
    }
    finally {
      ourLoadedBytesLimitLock.unlock();
    }
  }

  private static void signalThatFileIsUnloaded(long fileLength) {
    ourLoadedBytesLimitLock.lock();
    try {
      assert ourTotalBytesLoadedIntoMemory >= fileLength;
      ourTotalBytesLoadedIntoMemory -= fileLength;
      if (ourTotalBytesLoadedIntoMemory < SOFT_MAX_TOTAL_BYTES_LOADED_INTO_MEMORY) {
        ourLoadedBytesAreReleasedCondition.signalAll();
      }
    }
    finally {
      ourLoadedBytesLimitLock.unlock();
    }
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

  @NotNull
  public static String getPresentableLocationBeingIndexed(@NotNull Project project, @NotNull VirtualFile file) {
    VirtualFile actualFile = file;
    if (actualFile.getFileSystem() instanceof ArchiveFileSystem) {
      actualFile = VfsUtil.getLocalFile(actualFile);
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
    final CachedFileContentLoader myContentLoader;
    final BlockingQueue<VirtualFile> myQueueOfFiles;
    final ProgressIndicator myIndicator;
    final int myTotalFiles;
    final AtomicBoolean myNoMoreFilesInQueue = new AtomicBoolean();
    final CountDownLatch myAllFilesAreProcessedLatch;
    final IndexingJobStatistics myStatistics = new IndexingJobStatistics();
    final AtomicReference<Throwable> myError = new AtomicReference<>();

    IndexingJob(@NotNull Project project,
                @NotNull ProgressIndicator indicator,
                @NotNull CachedFileContentLoader contentLoader,
                @NotNull Collection<VirtualFile> files) {
      myProject = project;
      myIndicator = indicator;
      myTotalFiles = files.size();
      myContentLoader = contentLoader;
      myQueueOfFiles = new ArrayBlockingQueue<>(files.size(), false, files);
      myAllFilesAreProcessedLatch = new CountDownLatch(files.size());
    }

    public void oneMoreFileProcessed() {
      myAllFilesAreProcessedLatch.countDown();
      double newFraction = 1.0 - myAllFilesAreProcessedLatch.getCount() / (double) myTotalFiles;
      try {
        myIndicator.setFraction(newFraction);
      }
      catch (Exception ignored) {
        //Unexpected here. A misbehaved progress indicator must not break our code flow.
      }
    }

    boolean areAllFilesProcessed() {
      return myAllFilesAreProcessedLatch.getCount() == 0;
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
  }
}