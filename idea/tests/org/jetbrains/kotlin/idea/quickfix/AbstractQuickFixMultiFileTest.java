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

package org.jetbrains.kotlin.idea.quickfix;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.codeInspection.InspectionEP;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.LocalInspectionEP;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import junit.framework.ComparisonFailure;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinDaemonAnalyzerTestCase;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.quickfix.utils.QuickfixTestUtilsKt;
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public abstract class AbstractQuickFixMultiFileTest extends KotlinDaemonAnalyzerTestCase {

    protected static boolean shouldBeAvailableAfterExecution() {
        return false;
    }

    private static List<String> getActionsTexts(List<IntentionAction> availableActions) {
        List<String> texts = new ArrayList<String>();
        for (IntentionAction intentionAction : availableActions) {
            texts.add(intentionAction.getText());
        }
        return texts;
    }

    protected void doTestWithoutExtraFile(String beforeFileName) throws Exception {
        doTest(beforeFileName, false);
    }

    protected void doTestWithExtraFile(String beforeFileName) throws Exception {
        enableInspections(beforeFileName);

        if (beforeFileName.endsWith(".test")) {
            doMultiFileTest(beforeFileName);
        }
        else {
            doTest(beforeFileName, true);
        }
    }

    private void enableInspections(String beforeFileName) throws IOException, ClassNotFoundException {
        File inspectionFile = QuickfixTestUtilsKt.findInspectionFile(new File(beforeFileName).getParentFile());
        if (inspectionFile != null) {
            String className = FileUtil.loadFile(inspectionFile).trim();
            Class<?> inspectionClass = Class.forName(className);
            enableInspectionTools(inspectionClass);
        }
    }

    private void enableInspectionTools(@NotNull Class<?> klass) {
        List<InspectionEP> eps = ContainerUtil.newArrayList();
        ContainerUtil.addAll(eps, Extensions.getExtensions(LocalInspectionEP.LOCAL_INSPECTION));
        ContainerUtil.addAll(eps, Extensions.getExtensions(InspectionEP.GLOBAL_INSPECTION));

        InspectionProfileEntry tool = null;
        for (InspectionEP ep : eps) {
            if (klass.getName().equals(ep.implementationClass)) {
                tool = ep.instantiateTool();
            }
        }
        assert tool != null : "Could not find inspection tool for class: " + klass;

        enableInspectionTools(tool);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = new String[]{"excludedPackage", "somePackage.ExcludedClass"};
    }

    @Override
    protected void tearDown() throws Exception {
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = ArrayUtil.EMPTY_STRING_ARRAY;
        super.tearDown();
    }

    protected void doMultiFileTest(final String beforeFileName) throws Exception {
        String multifileText = FileUtil.loadFile(new File(beforeFileName), true);

        final List<TestFile> subFiles = KotlinTestUtils.createTestFiles(
                "single.kt",
                multifileText,
                new KotlinTestUtils.TestFileFactoryNoModules<TestFile>() {
                    @NotNull
                    @Override
                    public TestFile create(@NotNull String fileName, @NotNull String text, @NotNull Map<String, String> directives) {
                        if (text.startsWith("// FILE")) {
                            String firstLineDropped = StringUtil.substringAfter(text, "\n");
                            assert firstLineDropped != null;

                            text = firstLineDropped;
                        }
                        return new TestFile(fileName, text);
                    }
                });

        final TestFile afterFile = CollectionsKt.firstOrNull(subFiles, new Function1<TestFile, Boolean>() {
            @Override
            public Boolean invoke(TestFile file) {
                return file.name.contains(".after");
            }
        });
        final TestFile beforeFile = CollectionsKt.firstOrNull(subFiles, new Function1<TestFile, Boolean>() {
            @Override
            public Boolean invoke(TestFile file) {
                return file.name.contains(".before");
            }
        });

        assert beforeFile != null;
        assert afterFile != null;

        subFiles.remove(afterFile);
        subFiles.remove(beforeFile);

        for (TestFile file : subFiles) {
            configureByText(KotlinFileType.INSTANCE, file.content);
        }

        configureByText(KotlinFileType.INSTANCE, beforeFile.content);

        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
            @Override
            public void run() {
                try {
                    PsiFile psiFile = getFile();

                    Pair<String, Boolean> pair = LightQuickFixTestCase.parseActionHint(psiFile, beforeFile.content);
                    String text = pair.getFirst();

                    boolean actionShouldBeAvailable = pair.getSecond();

                    if (psiFile instanceof KtFile) {
                        DirectiveBasedActionUtils.INSTANCE.checkForUnexpectedErrors((KtFile) psiFile);
                    }

                    doAction(text, actionShouldBeAvailable, getTestName(false));

                    String actualText = getFile().getText();
                    String afterText = new StringBuilder(actualText).insert(getEditor().getCaretModel().getOffset(), "<caret>").toString();

                    if (pair.second && !afterText.equals(afterFile.content)) {
                        StringBuilder actualTestFile = new StringBuilder();
                        actualTestFile.append("// FILE: ").append(beforeFile.name).append("\n").append(beforeFile.content);
                        for (TestFile file : subFiles) {
                            actualTestFile.append("// FILE: ").append(file.name).append("\n").append(file.content);
                        }
                        actualTestFile.append("// FILE: ").append(afterFile.name).append("\n").append(afterText);

                        KotlinTestUtils.assertEqualsToFile(new File(beforeFileName), actualTestFile.toString());
                    }
                }
                catch (ComparisonFailure e) {
                    throw e;
                }
                catch (AssertionError e) {
                    throw e;
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    fail(getTestName(true));
                }
            }
        }, "", "");
    }

    private void doTest(final String beforeFileName, boolean withExtraFile) throws Exception {
        String testDataPath = getTestDataPath();
        File mainFile = new File(testDataPath + beforeFileName);
        final String originalFileText = FileUtil.loadFile(mainFile, true);

        boolean withRuntime = InTextDirectivesUtils.isDirectiveDefined(originalFileText, "// WITH_RUNTIME");
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk());
        }

        try {
            if (withExtraFile) {
                File mainFileDir = mainFile.getParentFile();
                assert mainFileDir != null;

                final String mainFileName = mainFile.getName();
                File[] extraFiles = mainFileDir.listFiles(
                        new FilenameFilter() {
                            @Override
                            public boolean accept(@NotNull File dir, @NotNull String name) {
                                return name.startsWith(extraFileNamePrefix(mainFileName)) && !name.equals(mainFileName);
                            }
                        }
                );
                assert extraFiles != null;

                List<String> testFiles = new ArrayList<String>();
                testFiles.add(beforeFileName);
                ArraysKt.mapTo(
                        extraFiles,
                        testFiles,
                        new Function1<File, String>() {
                            @Override
                            public String invoke(File file) {
                                return beforeFileName.replace(mainFileName, file.getName());
                            }
                        }
                );

                configureByFiles(null, ArrayUtil.toStringArray(testFiles));
            }
            else {
                configureByFiles(null, beforeFileName);
            }

            CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
                @Override
                public void run() {
                    try {
                        PsiFile psiFile = getFile();

                        Pair<String, Boolean> pair = LightQuickFixTestCase.parseActionHint(psiFile, originalFileText);
                        String text = pair.getFirst();

                        boolean actionShouldBeAvailable = pair.getSecond();

                        if (psiFile instanceof KtFile) {
                            DirectiveBasedActionUtils.INSTANCE.checkForUnexpectedErrors((KtFile) psiFile);
                        }

                        doAction(text, actionShouldBeAvailable, beforeFileName);

                        if (actionShouldBeAvailable) {
                            String afterFilePath = beforeFileName.replace(".before.Main.", ".after.");
                            try {
                                checkResultByFile(afterFilePath);
                            }
                            catch (ComparisonFailure e) {
                                KotlinTestUtils.assertEqualsToFile(new File(afterFilePath), getEditor());
                            }

                            PsiFile mainFile = myFile;
                            String mainFileName = mainFile.getName();
                            for (PsiFile file : mainFile.getContainingDirectory().getFiles()) {
                                String fileName = file.getName();
                                if (fileName.equals(mainFileName) || !fileName.startsWith(extraFileNamePrefix(myFile.getName()))) continue;

                                String extraFileFullPath = beforeFileName.replace(mainFileName, fileName);
                                File afterFile = new File(extraFileFullPath.replace(".before.", ".after."));
                                if (afterFile.exists()) {
                                    KotlinTestUtils.assertEqualsToFile(afterFile, file.getText());
                                }
                                else {
                                    KotlinTestUtils.assertEqualsToFile(new File(extraFileFullPath), file.getText());
                                }
                            }
                        }
                    }
                    catch (ComparisonFailure e) {
                        throw e;
                    }
                    catch (AssertionError e) {
                        throw e;
                    }
                    catch (Throwable e) {
                        e.printStackTrace();
                        fail(getTestName(true));
                    }
                }
            }, "", "");
        }
        finally {
            if (withRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk());
            }
        }
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public void doAction(String text, boolean actionShouldBeAvailable, String testFilePath) throws Exception {
        List<IntentionAction> availableActions = getAvailableActions();
        IntentionAction action = LightQuickFixTestCase.findActionWithText(availableActions, text);

        if (action == null) {
            if (actionShouldBeAvailable) {
                List<String> texts = getActionsTexts(availableActions);
                Collection<HighlightInfo> infos = doHighlighting();
                fail("Action with text '" + text + "' is not available in test " + testFilePath + "\n" +
                     "Available actions (" + texts.size() + "): \n" +
                     StringUtil.join(texts, "\n") +
                     "\nActions:\n" +
                     StringUtil.join(availableActions, "\n") +
                     "\nInfos:\n" +
                     StringUtil.join(infos, "\n"));
            }
            else {
                DirectiveBasedActionUtils.INSTANCE.checkAvailableActionsAreExpected(getFile(), availableActions);
            }
        }
        else {
            if (!actionShouldBeAvailable) {
                fail("Action '" + text + "' is available (but must not) in test " + testFilePath);
            }

            ShowIntentionActionsHandler.chooseActionAndInvoke(getFile(), getEditor(), action, action.getText());

            UIUtil.dispatchAllInvocationEvents();

            //noinspection ConstantConditions
            if (!shouldBeAvailableAfterExecution()) {
                IntentionAction afterAction = LightQuickFixTestCase.findActionWithText(getAvailableActions(), text);

                if (afterAction != null) {
                    fail("Action '" + text + "' is still available after its invocation in test " + testFilePath);
                }
            }
        }
    }

    private List<IntentionAction> getAvailableActions() {
        doHighlighting();
        return LightQuickFixTestCase.getAvailableActions(getEditor(), getFile());
    }

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.mockJdk();
    }

    @Override
    protected String getTestDataPath() {
        return KotlinTestUtils.getHomeDirectory() + "/";
    }

    @NotNull
    private static String extraFileNamePrefix(@NotNull  String mainFileName) {
        return mainFileName.replace(".Main.kt", ".").replace(".Main.java", ".");
    }

    private static class TestFile {
        public final String name;
        public final String content;

        TestFile(String name, String content) {
            this.name = name;
            this.content = content;
        }
    }

    @NotNull
    private VirtualFile findVirtualFile(@NotNull String filePath) {
        String absolutePath = getTestDataPath() + filePath;
        return VfsTestUtil.findFileByCaseSensitivePath(absolutePath);
    }
}
