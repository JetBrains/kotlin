// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.util.concurrency.Semaphore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

public class FileBasedIndexSwitcher {
    @NotNull
    private final FileBasedIndexImpl myFileBasedIndex;
    @NotNull
    private final Semaphore mySemaphore = new Semaphore();

    @TestOnly
    public FileBasedIndexSwitcher() {
        this(((FileBasedIndexImpl) FileBasedIndex.getInstance()));
    }

    public FileBasedIndexSwitcher(@NotNull FileBasedIndexImpl index) {
        myFileBasedIndex = index;
    }

    public void turnOff() {
        assert ApplicationManager.getApplication().isDispatchThread();
        FileBasedIndexImpl.LOG.assertTrue(mySemaphore.isUp());
        mySemaphore.down();

        if (!ApplicationManager.getApplication().isUnitTestMode()) {
            for (Project project : ProjectManager.getInstance().getOpenProjects()) {
                DumbService.getInstance(project).queueTask(new DumbModeTask() {
                    @Override
                    public void performInDumbMode(@NotNull ProgressIndicator indicator) {
                        indicator.setText(IdeBundle.message("progress.indexing.reload"));
                        mySemaphore.waitFor();
                    }
                });
            }
        }

        myFileBasedIndex.performShutdown(true);
        myFileBasedIndex.dropRegisteredIndexes();
    }

    public void turnOn() {
        assert ApplicationManager.getApplication().isDispatchThread();
        myFileBasedIndex.initComponent();

        FileBasedIndexImpl.LOG.assertTrue(!mySemaphore.isUp());
        mySemaphore.up();

        if (ApplicationManager.getApplication().isUnitTestMode()) {
            myFileBasedIndex.waitUntilIndicesAreInitialized();
        }
    }
}
