/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.rename;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenameProcessor;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;

public class SimpleNameReferenceRenameTest extends LightCodeInsightTestCase {

    @NotNull
    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase() + "/refactoring/rename/simpleNameReference/";
    }

    public void testRenameLabel() throws Exception {
        doTest("foo");
    }

    public void testRenameLabel2() throws Exception {
        doTest("anotherFoo");
    }

    public void testRenameField() throws Exception {
        doTest("renamed");
    }

    public void testRenameFieldIdentifier() throws Exception {
        doTest("anotherRenamed");
    }

    public void testMemberOfLocalObject() throws Exception {
        doTest("bar");
    }

    public void testLocalFunction() throws Exception {
        doTest("xyzzy");
    }

    public void testParameterOfCopyMethod() throws Exception {
        doTest("y");
    }

    private void doTest(String newName) throws Exception {
        configureByFile(getTestName(true) + ".kt");
        PsiElement element = TargetElementUtil
                .findTargetElement(myEditor,
                                   TargetElementUtil.ELEMENT_NAME_ACCEPTED | TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED);
        assertNotNull(element);
        new RenameProcessor(getProject(), element, newName, true, true).run();
        checkResultByFile(getTestName(true) + ".kt.after");
    }
}
