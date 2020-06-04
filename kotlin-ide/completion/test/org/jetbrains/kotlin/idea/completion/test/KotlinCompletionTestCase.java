/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CompletionTestCase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.WithMutedInDatabaseRunTest;

import java.io.File;

import static com.intellij.testFramework.RunAll.runAll;
import static org.jetbrains.kotlin.test.KotlinTestUtils.getTestDataFileName;
import static org.jetbrains.kotlin.test.KotlinTestUtils.getTestsRoot;

@WithMutedInDatabaseRunTest
abstract public class KotlinCompletionTestCase extends CompletionTestCase {
    private Disposable vfsDisposable;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        vfsDisposable = Disposer.newDisposable(getTestRootDisposable(), getClass().getName());
        VfsRootAccess.allowRootAccess(vfsDisposable, KotlinTestUtils.getHomeDirectory());
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = new String[]{"excludedPackage", "somePackage.ExcludedClass"};
    }

    @Override
    protected void tearDown() throws Exception {
        runAll(
                () -> CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = ArrayUtil.EMPTY_STRING_ARRAY,
                () -> {
                    if (!Disposer.isDisposed(vfsDisposable)) {
                        Disposer.dispose(vfsDisposable);
                        vfsDisposable = null;
                    }
                },
                () -> super.tearDown()
        );
    }

    protected File testDataFile(String fileName) {
        return new File(getTestDataPath(), fileName);
    }

    protected File testDataFile() {
        return testDataFile(fileName());
    }

    protected String fileName() {
        String name = getTestDataFileName(getClass(), this.getName());
        return name != null ? name : (getTestName(false) + ".kt");
    }

    @Override
    protected @NotNull String getTestDataPath() {
        return getTestsRoot(getClass());
    }

    @Override
    protected void runTest() throws Throwable {
        //noinspection Convert2MethodRef
        KotlinTestUtils.runTestWithThrowable(this, () -> super.runTest());
    }
}
