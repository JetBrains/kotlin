/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.stubs;

import com.intellij.openapi.projectRoots.Sdk;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.AstAccessControl;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;

import java.io.File;

// This test is quite old and is partially failing after IDEA 2018.2
// ALLOW_AST_ACCESS is added to 'util.kt' in test data to mute the failure
// Possible solutions:
// 1. Review and expand test data and fix platform issues leading to test failures
// 2. Remove the test completely if it's considered to have no value anymore
public abstract class AbstractMultiFileHighlightingTest extends AbstractMultiHighlightingTest {

    public void doTest(@NotNull String filePath) throws Exception {
        configureByFile(new File(filePath).getName(), "");
        boolean shouldFail = getName().contains("UnspecifiedType");
        AstAccessControl.INSTANCE.testWithControlledAccessToAst(
                shouldFail, getFile().getVirtualFile(), getProject(), getTestRootDisposable(),
                new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        checkHighlighting(myEditor, true, false);
                        return Unit.INSTANCE;
                    }
                }
        );
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/multiFileHighlighting/";
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.mockJdk();
    }
}
