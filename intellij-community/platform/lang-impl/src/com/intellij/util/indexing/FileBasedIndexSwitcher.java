// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.util.indexing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

public class FileBasedIndexSwitcher {
    private static final Logger LOG = Logger.getInstance(FileBasedIndexSwitcher.class);

    @NotNull
    private final FileBasedIndexImpl myFileBasedIndex;

    @TestOnly
    public FileBasedIndexSwitcher() {
        this(((FileBasedIndexImpl) FileBasedIndex.getInstance()));
    }

    public FileBasedIndexSwitcher(@NotNull FileBasedIndexImpl index) {
        myFileBasedIndex = index;
    }

    public void turnOff() {
        LOG.assertTrue(ApplicationManager.getApplication().isWriteAccessAllowed());
        myFileBasedIndex.performShutdown(true);
        myFileBasedIndex.dropRegisteredIndexes();
        IndexingStamp.flushCaches();
        ID.clearIdRegistry();
    }

    public void turnOn() {
        LOG.assertTrue(ApplicationManager.getApplication().isWriteAccessAllowed());
        myFileBasedIndex.initComponent();
        if (ApplicationManager.getApplication().isUnitTestMode()) {
            myFileBasedIndex.waitUntilIndicesAreInitialized();
        }
    }
}
