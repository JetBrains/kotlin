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
    public void testImplementFunInJava() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementKotlinClassInJava() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementKotlinAbstractClassInJava() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementKotlinTraitInJava() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementJavaClassInKotlin() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementJavaAbstractClassInKotlin() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementJavaInterfaceInKotlin() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementMethodInKotlin() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementVarInJava() throws Exception {
        doKotlinJavaTest();
    }

    public void testImplementJavaInnerInterface() throws Exception {
        doJavaKotlinTest();
    }

    public void testImplementJavaInnerInterfaceFromUsage() throws Exception {
        doJavaKotlinTest();
    }

    private void doKotlinJavaTest() throws Exception {
        doMultifileTest(getTestName(false) + ".kt", getTestName(false) + ".java");
    }

    private void doJavaKotlinTest() throws Exception {
        doMultifileTest(getTestName(false) + ".java", getTestName(false) + ".kt");
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinLightProjectDescriptor.INSTANCE;
    }

    private void doMultifileTest(String ... fileNames) throws Exception {
        myFixture.configureByFiles(fileNames);
        GotoTargetHandler.GotoData gotoData = NavigationTestUtils.invokeGotoImplementations(getEditor(), getFile());
        NavigationTestUtils.assertGotoDataMatching(getEditor(), gotoData);
    }

    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(),
                        "/navigation/implementations/multifile/" + getTestName(false)).getPath() + File.separator;
    }
}
