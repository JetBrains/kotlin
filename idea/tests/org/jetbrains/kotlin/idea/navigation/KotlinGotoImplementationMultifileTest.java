/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
