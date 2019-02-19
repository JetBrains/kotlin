/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.navigation;

import com.intellij.codeInsight.navigation.GotoTargetHandler;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.KotlinLightProjectDescriptor;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;

import java.io.File;

public class KotlinGotoImplementationMultifileTest extends KotlinLightCodeInsightFixtureTestCase {
    public void testImplementFunInJava() {
        doKotlinJavaTest();
    }

    public void testImplementKotlinClassInJava() {
        doKotlinJavaTest();
    }

    public void testImplementKotlinAbstractClassInJava() {
        doKotlinJavaTest();
    }

    public void testImplementKotlinTraitInJava() {
        doKotlinJavaTest();
    }

    public void testImplementJavaClassInKotlin() {
        doKotlinJavaTest();
    }

    public void testImplementJavaAbstractClassInKotlin() {
        doKotlinJavaTest();
    }

    public void testImplementJavaInterfaceInKotlin() {
        doKotlinJavaTest();
    }

    public void testImplementMethodInKotlin() {
        doKotlinJavaTest();
    }

    public void testImplementVarInJava() {
        doKotlinJavaTest();
    }

    public void testImplementJavaInnerInterface() {
        doJavaKotlinTest();
    }

    public void testImplementJavaInnerInterfaceFromUsage() {
        doJavaKotlinTest();
    }

    private void doKotlinJavaTest() {
        doMultiFileTest(getTestName(false) + ".kt", getTestName(false) + ".java");
    }

    private void doJavaKotlinTest() {
        doMultiFileTest(getTestName(false) + ".java", getTestName(false) + ".kt");
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinLightProjectDescriptor.INSTANCE;
    }

    private void doMultiFileTest(String... fileNames) {
        myFixture.configureByFiles(fileNames);
        GotoTargetHandler.GotoData gotoData = NavigationTestUtils.invokeGotoImplementations(getEditor(), getFile());
        NavigationTestUtils.assertGotoDataMatching(getEditor(), gotoData);
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(),
                        "/navigation/implementations/multifile/" + getTestName(false)).getPath() + File.separator;
    }
}
