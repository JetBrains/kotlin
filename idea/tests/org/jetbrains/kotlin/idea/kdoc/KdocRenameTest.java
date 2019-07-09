/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.kdoc;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner;
import org.junit.runner.RunWith;

@RunWith(JUnit3WithIdeaConfigurationRunner.class)
public class KdocRenameTest extends LightCodeInsightTestCase {
    @NotNull
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/kdoc/rename/";
    }

    public void testParamReference() throws Exception {
        doTest("bar");
    }

    public void testTypeParamReference() throws Exception {
        doTest("R");
    }

    public void testCodeReference() throws Exception {
        doTest("xyzzy");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
    }

    private void doTest(String newName) throws Exception {
        configureByFile(getTestName(false) + ".kt");
        PsiElement element = TargetElementUtil
                .findTargetElement(myEditor,
                                   TargetElementUtil.ELEMENT_NAME_ACCEPTED | TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED);
        assertNotNull(element);
        new RenameProcessor(getProject(), element, newName, true, true).run();
        checkResultByFile(getTestName(false) + ".kt.after");
    }
}
