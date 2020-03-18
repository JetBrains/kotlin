// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.Semaphore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

public class FileBasedIndexSwitcher {
    private static final Logger LOG = Logger.getInstance(FileBasedIndexSwitcher.class);

    @NotNull
    private final FileBasedIndexImpl myFileBasedIndex;

    // accessed only in EDT
    private Semaphore myDumbModeSemaphore;

    @TestOnly
    public FileBasedIndexSwitcher() {
        this(((FileBasedIndexImpl) FileBasedIndex.getInstance()));
    }

    public FileBasedIndexSwitcher(@NotNull FileBasedIndexImpl index) {
        myFileBasedIndex = index;
    }

    public void turnOff() {
        LOG.assertTrue(ApplicationManager.getApplication().isDispatchThread());
        LOG.assertTrue(!ApplicationManager.getApplication().isWriteAccessAllowed());
        Project[] projects = ProjectManager.getInstance().getOpenProjects();
        boolean unitTestMode = ApplicationManager.getApplication().isUnitTestMode();
        if (!unitTestMode) {
            myDumbModeSemaphore = new Semaphore(1);
            for (Project project : projects) {
                DumbService.getInstance(project).cancelAllTasksAndWait();
                DumbService.getInstance(project).queueTask(new DumbModeTask() {
                    @Override
                    public void performInDumbMode(@NotNull ProgressIndicator indicator) {
                        indicator.setText(IndexingBundle.message("indexes.reloading"));
                        myDumbModeSemaphore.waitFor();
                    }
                });
            }
        }
        myFileBasedIndex.performShutdown(true);
        myFileBasedIndex.dropRegisteredIndexes();
        IndexingStamp.flushCaches();
    }

    public void turnOn() {
        LOG.assertTrue(ApplicationManager.getApplication().isWriteThread());
        RebuildStatus.reset();
        myFileBasedIndex.initComponent();
        boolean unitTestMode = ApplicationManager.getApplication().isUnitTestMode();

        if (unitTestMode) {
            myFileBasedIndex.waitUntilIndicesAreInitialized();
        }

        if (!unitTestMode) {
            myDumbModeSemaphore.up();
        }

        FileBasedIndexImpl.cleanupProcessedFlag();
        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            DumbService.getInstance(project).queueTask(new UnindexedFilesUpdater(project));
        }
    }
}
