/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea;

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import static com.intellij.testFramework.RunAll.runAll;

abstract public class KotlinDaemonAnalyzerTestCase extends DaemonAnalyzerTestCase {
    private Disposable vfsDisposable;
    @Override
    protected void setUp() throws Exception {
        vfsDisposable = Disposer.newDisposable(getTestRootDisposable(), getClass().getName());
        VfsRootAccess.allowRootAccess(vfsDisposable, KotlinTestUtils.getHomeDirectory());
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        runAll(
                () -> super.tearDown(),
                () -> {
                    if (!Disposer.isDisposed(vfsDisposable)) {
                        Disposer.dispose(vfsDisposable);
                        vfsDisposable = null;
                    }
                }
        );
    }
}
