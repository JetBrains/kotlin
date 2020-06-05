// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.ProjectTopics;
import com.intellij.diagnostic.PerformanceWatcher;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.impl.ProgressSuspender;
import com.intellij.openapi.project.*;
import com.intellij.openapi.roots.ContentIterator;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdater;
import com.intellij.openapi.roots.impl.PushedFilePropertiesUpdaterImpl;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.RefreshQueue;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.concurrency.NonUrgentExecutor;
import com.intellij.util.containers.ConcurrentBitSet;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.contentQueue.IndexUpdateRunner;
import com.intellij.util.indexing.diagnostic.FileProviderIndexStatistics;
import com.intellij.util.indexing.diagnostic.IndexDiagnosticDumper;
import com.intellij.util.indexing.diagnostic.IndexingJobStatistics;
import com.intellij.util.indexing.diagnostic.ProjectIndexingHistory;
import com.intellij.util.indexing.roots.IndexableFilesProvider;
import com.intellij.util.indexing.roots.SdkIndexableFilesProvider;
import com.intellij.util.messages.MessageBusConnection;
import com.intellij.util.progress.ConcurrentTasksProgressManager;
import com.intellij.util.progress.SubTaskProgressIndicator;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public final class UnindexedFilesUpdater extends DumbModeTask {
  private static final Logger LOG = Logger.getInstance(UnindexedFilesUpdater.class);

  private static final int DEFAULT_MAX_INDEXER_THREADS = 4;

  public static final ExecutorService GLOBAL_INDEXING_EXECUTOR = AppExecutorUtil.createBoundedApplicationPoolExecutor(
    "Indexing", getMaxNumberOfIndexingThreads()
  );

  private final FileBasedIndexImpl myIndex = (FileBasedIndexImpl)FileBasedIndex.getInstance();
  private final Project myProject;
  private final boolean myStartSuspended;
  private final PushedFilePropertiesUpdater myPusher;

  public UnindexedFilesUpdater(@NotNull Project project, boolean startSuspended) {
    super(project);
    myProject = project;
    myStartSuspended = startSuspended;
    myPusher = PushedFilePropertiesUpdater.getInstance(myProject);
    project.getMessageBus().connect(this).subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        DumbService.getInstance(project).cancelTask(UnindexedFilesUpdater.this);
      }
    });
  }

  public UnindexedFilesUpdater(@NotNull Project project) {
    this(project, false);
  }

  private void updateUnindexedFiles(ProgressIndicator indicator) {
    if (!IndexInfrastructure.hasIndices()) return;
    ProjectIndexingHistory projectIndexingHistory = new ProjectIndexingHistory(myProject.getName());
    projectIndexingHistory.getTimes().setIndexingStart(Instant.now());

    if (myStartSuspended) {
      ProgressSuspender suspender = ProgressSuspender.getSuspender(indicator);
      if (suspender == null) {
        throw new IllegalStateException("Indexing progress indicator must be suspendable!");
      }
      if (!suspender.isSuspended()) {
        suspender.suspendProcess(IndexingBundle.message("progress.indexing.started.as.suspended"));
      }
    }

    indicator.setIndeterminate(true);
    indicator.setText(IndexingBundle.message("progress.indexing.scanning"));

    projectIndexingHistory.getTimes().setPushPropertiesStart(Instant.now());

    PerformanceWatcher.Snapshot snapshot = PerformanceWatcher.takeSnapshot();
    myPusher.pushAllPropertiesNow();
    boolean trackResponsiveness = !ApplicationManager.getApplication().isUnitTestMode();

    projectIndexingHistory.getTimes().setPushPropertiesEnd(Instant.now());

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Pushing properties");

    myIndex.clearIndicesIfNecessary();

    snapshot = PerformanceWatcher.takeSnapshot();

    projectIndexingHistory.getTimes().setIndexExtensionsStart(Instant.now());

    FileBasedIndexInfrastructureExtension.EP_NAME.extensions().forEach(ex -> ex.processIndexingProject(myProject, indicator));

    projectIndexingHistory.getTimes().setIndexExtensionsEnd(Instant.now());

    projectIndexingHistory.getTimes().setScanFilesStart(Instant.now());

    List<IndexableFilesProvider> orderedProviders = getOrderedProviders();

    Map<IndexableFilesProvider, List<VirtualFile>> providerToFiles = collectIndexableFilesConcurrently(myProject, indicator, orderedProviders);

    projectIndexingHistory.getTimes().setScanFilesEnd(Instant.now());

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Indexable file iteration");

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      // full VFS refresh makes sense only after it's loaded, i.e. after scanning files to index is finished
      scheduleInitialVfsRefresh();
    }

    int totalFiles = providerToFiles.values().stream().mapToInt(it -> it.size()).sum();
    if (trackResponsiveness) {
      LOG.info("Unindexed files update started: " + totalFiles + " files to index");
    }

    if (totalFiles == 0 || SystemProperties.getBooleanProperty("idea.indexes.pretendNoFiles", false)) {
      FileBasedIndexInfrastructureExtension.EP_NAME.extensions().forEach(ex -> ex.noFilesFoundToProcessIndexingProject(myProject, indicator));
      return;
    }

    snapshot = PerformanceWatcher.takeSnapshot();

    ProgressIndicator poweredIndicator = PoweredProgressIndicator.wrap(indicator, getPowerForSmoothProgressIndicator());
    poweredIndicator.setIndeterminate(false);
    poweredIndicator.setFraction(0);
    poweredIndicator.setText(IndexingBundle.message("progress.indexing.updating"));
    ConcurrentTasksProgressManager concurrentTasksProgressManager = new ConcurrentTasksProgressManager(poweredIndicator, totalFiles);

    int numberOfIndexingThreads = getNumberOfIndexingThreads();
    LOG.info("Using " + numberOfIndexingThreads + " " + StringUtil.pluralize("thread", numberOfIndexingThreads) + " for indexing");
    IndexUpdateRunner indexUpdateRunner = new IndexUpdateRunner(myIndex, GLOBAL_INDEXING_EXECUTOR, numberOfIndexingThreads);
    projectIndexingHistory.setNumberOfIndexingThreads(numberOfIndexingThreads);

    for (IndexableFilesProvider provider : orderedProviders) {
      List<VirtualFile> providerFiles = providerToFiles.get(provider);
      if (providerFiles == null || providerFiles.isEmpty()) {
        continue;
      }
      concurrentTasksProgressManager.setText(provider.getIndexingProgressText());
      SubTaskProgressIndicator subTaskIndicator = concurrentTasksProgressManager.createSubTaskIndicator(providerFiles.size());
      try {
        long startTime = System.nanoTime();
        IndexingJobStatistics indexStatistics = indexUpdateRunner.indexFiles(myProject, providerFiles, subTaskIndicator);
        long totalTime = System.nanoTime() - startTime;
        try {
          FileProviderIndexStatistics statistics = new FileProviderIndexStatistics(provider.getDebugName(),
                                                                                   providerFiles.size(),
                                                                                   totalTime,
                                                                                   indexStatistics);
          projectIndexingHistory.addProviderStatistics(statistics);
        }
        catch (Exception e) {
          LOG.error("Failed to add indexing statistics for " + provider.getDebugName(), e);
        }
      } finally {
        subTaskIndicator.finished();
      }
    }

    projectIndexingHistory.getTimes().setIndexingEnd(Instant.now());

    if (!ApplicationManager.getApplication().isUnitTestMode()) {
      NonUrgentExecutor.getInstance().execute(() -> {
        IndexDiagnosticDumper.INSTANCE.dumpProjectIndexingHistoryToLogSubdirectory(projectIndexingHistory);
      });
    }

    if (trackResponsiveness) snapshot.logResponsivenessSinceCreation("Unindexed files update");

    FileBasedIndexInfrastructureExtension.EP_NAME.extensions().forEach(ex -> ex.noFilesFoundToProcessIndexingProject(myProject, indicator));
    myIndex.dumpIndexStatistics();
  }

  /**
   * Returns providers of files. Since LAB-22 (Smart Dumb Mode) is not implemented yet, the order of the providers is not strictly specified.
   * For shared indexes it is a good idea to index JDKs in the last turn (because they likely have shared index available)
   * so this method moves all SDK providers to the end.
   */
  @NotNull
  private List<IndexableFilesProvider> getOrderedProviders() {
    List<IndexableFilesProvider> originalOrderedProviders = myIndex.getOrderedIndexableFilesProviders(myProject);

    List<IndexableFilesProvider> orderedProviders = new ArrayList<>();
    originalOrderedProviders.stream()
      .filter(p -> !(p instanceof SdkIndexableFilesProvider))
      .collect(Collectors.toCollection(() -> orderedProviders));

    originalOrderedProviders.stream()
      .filter(p -> p instanceof SdkIndexableFilesProvider)
      .collect(Collectors.toCollection(() -> orderedProviders));
    return orderedProviders;
  }

  @NotNull
  private Map<IndexableFilesProvider, List<VirtualFile>> collectIndexableFilesConcurrently(
    @NotNull Project project,
    @NotNull ProgressIndicator indicator,
    @NotNull List<IndexableFilesProvider> providers
  ) {
    if (providers.isEmpty()) {
      return Collections.emptyMap();
    }
    VirtualFileFilter unindexedFileFilter = new UnindexedFilesFinder(project, myIndex);
    Map<IndexableFilesProvider, List<VirtualFile>> providerToFiles = new IdentityHashMap<>();
    ConcurrentBitSet visitedFileSet = new ConcurrentBitSet();

    indicator.setText(IndexingBundle.message("progress.indexing.scanning"));
    indicator.setIndeterminate(false);
    indicator.setFraction(0);

    ConcurrentTasksProgressManager concurrentTasksProgressManager = new ConcurrentTasksProgressManager(indicator, providers.size());

    List<Runnable> tasks = ContainerUtil.map(providers, provider -> {
      SubTaskProgressIndicator subTaskIndicator = concurrentTasksProgressManager.createSubTaskIndicator(1);
      List<VirtualFile> files = new ArrayList<>();
      providerToFiles.put(provider, files);
      ContentIterator collectingIterator = fileOrDir -> {
        if (subTaskIndicator.isCanceled()) {
          return false;
        }
        if (unindexedFileFilter.accept(fileOrDir)) {
          files.add(fileOrDir);
        }
        return true;
      };
      return () -> {
        subTaskIndicator.setText(provider.getRootsScanningProgressText());
        try {
          provider.iterateFiles(project, collectingIterator, visitedFileSet);
        } finally {
          subTaskIndicator.finished();
        }
      };
    });
    PushedFilePropertiesUpdaterImpl.invokeConcurrentlyIfPossible(tasks);
    return providerToFiles;
  }

  private void scheduleInitialVfsRefresh() {
    ProjectRootManagerEx.getInstanceEx(myProject).markRootsForRefresh();

    Application app = ApplicationManager.getApplication();
    if (!app.isCommandLine()) {
      long sessionId = VirtualFileManager.getInstance().asyncRefresh(null);
      MessageBusConnection connection = app.getMessageBus().connect();
      connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
        @Override
        public void projectClosed(@NotNull Project project) {
          if (project == myProject) {
            RefreshQueue.getInstance().cancelSession(sessionId);
            connection.disconnect();
          }
        }
      });
    }
    else {
      ApplicationManager.getApplication().invokeAndWait(() -> VirtualFileManager.getInstance().syncRefresh());
    }
  }

  private static double getPowerForSmoothProgressIndicator() {
    String rawValue = Registry.stringValue("indexing.progress.indicator.power");
    if ("-".equals(rawValue)) {
      return 1.0;
    }
    try {
      return Double.parseDouble(rawValue);
    }
    catch (NumberFormatException e) {
      return 1.0;
    }
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

  /**
   * Returns the best number of threads to be used for indexing at this moment.
   * It may change during execution of the IDE depending on other activities' load.
   */
  public static int getNumberOfIndexingThreads() {
    int threadsCount = Registry.intValue("caches.indexerThreadsCount");
    if (threadsCount <= 0) {
      int coresToLeaveForOtherActivity = ApplicationManager.getApplication().isCommandLine() ? 0 : 1;
      threadsCount = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors() - coresToLeaveForOtherActivity, DEFAULT_MAX_INDEXER_THREADS));
    }
    return threadsCount;
  }

  /**
   * Returns the maximum number of threads to be used for indexing during this execution of the IDE.
   */
  public static int getMaxNumberOfIndexingThreads() {
    // Change of the registry option requires IDE restart.
    int threadsCount = Registry.intValue("caches.indexerThreadsCount");
    if (threadsCount <= 0) {
      return DEFAULT_MAX_INDEXER_THREADS;
    }
    return threadsCount;
  }
}
