/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CompletionTestCase;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Ref;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;

import static com.intellij.testFramework.RunAll.runAll;
import static org.jetbrains.kotlin.test.KotlinTestUtils.getTestDataFileName;
import static org.jetbrains.kotlin.test.KotlinTestUtils.getTestsRoot;

abstract public class KotlinCompletionTestCase extends CompletionTestCase {
    private Ref<Disposable> vfsDisposable;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        vfsDisposable = KotlinTestUtils.allowProjectRootAccess(this);
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = new String[]{"excludedPackage", "somePackage.ExcludedClass"};
    }

    @Override
    protected void tearDown() throws Exception {
        runAll(
                () -> CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = ArrayUtil.EMPTY_STRING_ARRAY,
                () -> KotlinTestUtils.disposeVfsRootAccess(vfsDisposable),
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
    protected final @NotNull String getTestDataPath() {
        return KotlinTestUtils.toSlashEndingDirPath(getTestDataDirectory().getAbsolutePath());
    }

    protected @NotNull File getTestDataDirectory() {
        return new File(getTestsRoot(getClass()));
    }
}
