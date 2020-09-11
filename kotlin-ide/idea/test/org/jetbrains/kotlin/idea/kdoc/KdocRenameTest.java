/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.kdoc;

import com.intellij.codeInsight.TargetElementUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenameProcessor;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.test.TestMetadata;
import org.jetbrains.kotlin.test.TestRoot;
import org.junit.internal.runners.JUnit38ClassRunner;
import org.junit.runner.RunWith;

@TestRoot("idea")
@TestMetadata("testData/kdoc/rename")
@RunWith(JUnit38ClassRunner.class)
public class KdocRenameTest extends KotlinLightCodeInsightFixtureTestCase {
    public void testParamReference() {
        doTest("bar");
    }

    public void testTypeParamReference() {
        doTest("R");
    }

    public void testCodeReference() {
        doTest("xyzzy");
    }

    @Override
    protected void setUp() {
        super.setUp();
    }

    private void doTest(String newName) {
        myFixture.configureByFile(getTestName(false) + ".kt");
        PsiElement element = TargetElementUtil
                .findTargetElement(getEditor(),
                                   TargetElementUtil.ELEMENT_NAME_ACCEPTED | TargetElementUtil.REFERENCED_ELEMENT_ACCEPTED);
        assertNotNull(element);
        new RenameProcessor(getProject(), element, newName, true, true).run();
        myFixture.checkResultByFile(getTestName(false) + ".kt.after");
    }
}
