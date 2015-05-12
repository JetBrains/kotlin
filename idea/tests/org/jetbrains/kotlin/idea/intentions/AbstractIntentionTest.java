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

package org.jetbrains.kotlin.idea.intentions;

import com.google.common.collect.Lists;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.util.PathUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.Convertor;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils;
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.idea.util.application.ApplicationPackage;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.junit.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractIntentionTest extends KotlinCodeInsightTestCase {
    private static IntentionAction createIntention(File testDataFile) throws Exception {
        List<File> candidateFiles = Lists.newArrayList();

        File current = testDataFile.getParentFile();
        while (current != null) {
            File candidate = new File(current, ".intention");
            if (candidate.exists()) {
                candidateFiles.add(candidate);
            }
            current = current.getParentFile();
        }

        if (candidateFiles.isEmpty()) {
            throw new AssertionError(".intention file is not found for " + testDataFile +
                                     "\nAdd it to base directory of test data. It should contain fully-qualified name of intention class.");
        }
        if (candidateFiles.size() > 1) {
            throw new AssertionError("Several .intention files are available for " + testDataFile +
                                     "\nPlease remove some of them\n" + candidateFiles);
        }

        String className = FileUtil.loadFile(candidateFiles.get(0)).trim();
        return (IntentionAction) Class.forName(className).newInstance();
    }

    private static final String[] EXTENSIONS = { ".kt", ".java", ".groovy" };

    protected void doTest(@NotNull String path) throws Exception {
        File mainFile = new File(path);
        String mainFileName = FileUtil.getNameWithoutExtension(mainFile);
        IntentionAction intentionAction = createIntention(mainFile);
        List<String> sourceFilePaths = new ArrayList<String>();
        File parentDir = mainFile.getParentFile();
        extraFileLoop:
        //noinspection ForLoopThatDoesntUseLoopVariable
        for (int i = 1; true; i++) {
            for (String extension : EXTENSIONS) {
                File extraFile = new File(parentDir, mainFileName + "." + i + extension);
                if (extraFile.exists()) {
                    sourceFilePaths.add(extraFile.getPath());
                    continue extraFileLoop;
                }
            }
            break;
        }
        sourceFilePaths.add(path);

        Map<String, PsiFile> pathToFile = ContainerUtil.newMapFromKeys(
                sourceFilePaths.iterator(),
                new Convertor<String, PsiFile>() {
                    @Override
                    public PsiFile convert(String path) {
                        try {
                            configureByFile(path);
                        }
                        catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                        return myFile;
                    }
                }
        );

        String fileText = FileUtil.loadFile(mainFile, true);

        String minJavaVersion = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// MIN_JAVA_VERSION: ");
        if (minJavaVersion != null && !SystemInfo.isJavaVersionAtLeast(minJavaVersion)) return;

        boolean isWithRuntime = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// WITH_RUNTIME") != null;

        try {
            if (isWithRuntime) {
                ConfigLibraryUtil.configureKotlinRuntimeAndSdk(getModule(), PluginTestCaseBase.mockJdk());
            }

            DirectiveBasedActionUtils.checkForUnexpectedErrors((JetFile) getFile());

            doTestFor(pathToFile, intentionAction, fileText);
        }
        finally {
            if (isWithRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(getModule(), getTestProjectJdk());
            }
        }
    }

    private void doTestFor(Map<String, PsiFile> pathToFile, final IntentionAction intentionAction, String fileText) throws Exception {
        String isApplicableString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// IS_APPLICABLE: ");
        boolean isApplicableExpected = isApplicableString == null || isApplicableString.equals("true");

        Assert.assertTrue(
                "isAvailable() for " + intentionAction.getClass() + " should return " + isApplicableExpected,
                isApplicableExpected == intentionAction.isAvailable(getProject(), getEditor(), getFile()));

        String intentionTextString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// INTENTION_TEXT: ");

        if (intentionTextString != null) {
            assertEquals("Intention text mismatch.", intentionTextString, intentionAction.getText());
        }

        String shouldFailString = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// SHOULD_FAIL_WITH: ");

        try {
            if (isApplicableExpected) {
                ApplicationPackage.executeWriteCommand(
                        getProject(),
                        intentionAction.getText(),
                        null,
                        new Function0<Object>() {
                            @Override
                            public Object invoke() {
                                intentionAction.invoke(getProject(), getEditor(), getFile());
                                return null;
                            }
                        }
                );
                // Don't bother checking if it should have failed.
                if (shouldFailString == null) {
                    for (Map.Entry<String, PsiFile> entry: pathToFile.entrySet()) {
                        //noinspection AssignmentToStaticFieldFromInstanceMethod
                        myFile = entry.getValue();
                        String canonicalPathToExpectedFile = PathUtil.getCanonicalPath(entry.getKey() + ".after");

                        checkResultByFile(canonicalPathToExpectedFile);
                    }
                }
            }
            assertNull("Expected test to fail.", shouldFailString);
        }
        catch (BaseRefactoringProcessor.ConflictsInTestsException e) {
            assertEquals("Failure message mismatch.", shouldFailString, StringUtil.join(e.getMessages(), ", "));
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
