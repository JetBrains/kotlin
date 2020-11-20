/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions.declarations;

import com.intellij.codeInsight.editorActions.JoinLinesHandler;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;

@SuppressWarnings("deprecation")
public abstract class AbstractJoinLinesTest extends LightCodeInsightTestCase {
    public void doTest(@NotNull String path) throws Exception {
        configureByFile(path);
        new JoinLinesHandler(null).execute(getEditor(), getEditor().getCaretModel().getCurrentCaret(),
                                           getCurrentEditorDataContext());
        String afterFilePath = path + ".after";
        try {
            checkResultByFile(afterFilePath);
        }
        catch (FileComparisonFailure e) {
            KotlinTestUtils.assertEqualsToFile(new File(afterFilePath), getEditor());
        }
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }

    protected static Sdk getFullJavaJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }
}
