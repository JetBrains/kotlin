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

import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.codeInsight.daemon.quickFix.ActionHint;
import com.intellij.codeInsight.daemon.quickFix.QuickFixTestCase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.SuppressIntentionAction;
import com.intellij.codeInspection.SuppressableProblemGroup;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.lang.annotation.ProblemGroup;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.StubUpdatingIndex;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import com.intellij.testFramework.InspectionTestUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.indexing.FileBasedIndex;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.jvm.functions.Function1;
import kotlin.text.StringsKt;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLightQuickFixTestCase;
import org.jetbrains.kotlin.idea.quickfix.utils.QuickfixTestUtilsKt;
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.idea.test.TestFixtureExtension;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.intellij.util.ObjectUtils.notNull;

public abstract class AbstractQuickFixTest extends KotlinLightQuickFixTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        ((StartupManagerImpl) StartupManager.getInstance(getProject())).runPostStartupActivities();
    }

    protected void doTest(@NotNull String beforeFileName) throws Exception {
        try {
            configureRuntimeIfNeeded(beforeFileName);

            enableInspections(beforeFileName);

            doSingleTest(getTestName(false) + ".kt");
            checkForUnexpectedErrors();
        }
        finally {
            unConfigureRuntimeIfNeeded(beforeFileName);
        }
    }

    //region Severe hack - lot of code copied from LightQuickFixTestCase to workaround stupid format of test data with before/after prefixes
    @Override
    protected void doSingleTest(String fileSuffix) {
        doKotlinQuickFixTest(fileSuffix, createWrapper());
    }

    private static QuickFixTestCase myWrapper;

    @Override
    protected boolean shouldBeAvailableAfterExecution() {
        return InTextDirectivesUtils.isDirectiveDefined(myWrapper.getFile().getText(), "// SHOULD_BE_AVAILABLE_AFTER_EXECUTION");
    }

    @NotNull
    @Override
    protected LocalInspectionTool[] configureLocalInspectionTools() {
        if (KotlinTestUtils.isAllFilesPresentTest(getTestName(false))) return super.configureLocalInspectionTools();

        String testRoot = KotlinTestUtils.getTestsRoot(this.getClass());
        String configFileText = FilesKt.readText(new File(testRoot, getTestName(true) + ".kt"), Charset.defaultCharset());
        List<String> toolsStrings = InTextDirectivesUtils.findListWithPrefixes(configFileText, "TOOL:");

        if (toolsStrings.isEmpty()) return super.configureLocalInspectionTools();

        return ArrayUtil.toObjectArray(CollectionsKt.map(toolsStrings, new Function1<String, LocalInspectionTool>() {
            @Override
            public LocalInspectionTool invoke(String toolFqName) {
                try {
                    Class<?> aClass = Class.forName(toolFqName);
                    return (LocalInspectionTool) aClass.newInstance();
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Failed to create inspection for key '" + toolFqName + "'", e);
                }
            }
        }), LocalInspectionTool.class);
    }

    protected void configExtra(String options) {

    }

    private void doKotlinQuickFixTest(final String testName, final QuickFixTestCase quickFixTestCase) {
        String relativePath = notNull(quickFixTestCase.getBasePath(), "") + "/" + StringsKt.decapitalize(testName);
        final String testFullPath = quickFixTestCase.getTestDataPath().replace(File.separatorChar, '/') + relativePath;
        final File testFile = new File(testFullPath);
        CommandProcessor.getInstance().executeCommand(quickFixTestCase.getProject(), new Runnable() {
            @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "CallToPrintStackTrace"})
            @Override
            public void run() {
                String fileText = "";
                String expectedErrorMessage = "";
                List<String> fixtureClasses = Collections.emptyList();
                try {
                    fileText = FileUtil.loadFile(testFile, CharsetToolkit.UTF8_CHARSET);
                    assertTrue("\"<caret>\" is missing in file \"" + testName + "\"", fileText.contains("<caret>"));

                    fixtureClasses = InTextDirectivesUtils.findListWithPrefixes(fileText, "// FIXTURE_CLASS: ");
                    for (String fixtureClass : fixtureClasses) {
                        TestFixtureExtension.Companion.loadFixture(fixtureClass, getModule());
                    }

                    expectedErrorMessage = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// SHOULD_FAIL_WITH: ");
                    String contents = StringUtil.convertLineSeparators(fileText);
                    quickFixTestCase.configureFromFileText(testFile.getName(), contents);
                    quickFixTestCase.bringRealEditorBack();

                    checkForUnexpectedActions();

                    configExtra(fileText);

                    applyAction(contents, quickFixTestCase, testName, testFullPath);

                    assertEmpty(expectedErrorMessage);
                }
                catch (FileComparisonFailure e) {
                    throw e;
                }
                catch (AssertionError e) {
                    throw e;
                }
                catch (Throwable e) {
                    if (expectedErrorMessage == null || !expectedErrorMessage.equals(e.getMessage())) {
                        e.printStackTrace();
                        fail(testName);
                    }
                }
                finally {
                    for (String fixtureClass : fixtureClasses) {
                        TestFixtureExtension.Companion.unloadFixture(fixtureClass);
                    }
                    ConfigLibraryUtil.unconfigureLibrariesByDirective(getModule(), fileText);
                }
            }
        }, "", "");
    }

    private static void applyAction(String contents, QuickFixTestCase quickFixTestCase, String testName, String testFullPath)
            throws Exception {
        String fileName = StringsKt.substringAfterLast(testFullPath, "/", "");
        ActionHint actionHint = ActionHint.parse(quickFixTestCase.getFile(), contents.replace("${file}", fileName));

        quickFixTestCase.beforeActionStarted(testName, contents);

        try {
            myWrapper = quickFixTestCase;
            quickFixTestCase.doAction(actionHint, testFullPath, testName);
        }
        finally {
            myWrapper = null;
            quickFixTestCase.afterActionCompleted(testName, contents);
        }
    }


    @Override
    protected void doAction(ActionHint actionHint, String testFullPath, String testName) throws Exception {
        doAction(actionHint, testFullPath, testName, myWrapper);
    }

    @Override
    protected void checkResultByFile(@Nullable String message, @NotNull String filePath, boolean ignoreTrailingSpaces) {
        File file = new File(filePath);
        String afterFileName = file.getName();
        assert afterFileName.startsWith(AFTER_PREFIX);
        String newAfterFileName = StringsKt.decapitalize(afterFileName.substring(AFTER_PREFIX.length())) + ".after";

        super.checkResultByFile(message, new File(file.getParent(), newAfterFileName).getPath(), ignoreTrailingSpaces);
    }
    //endregion

    private static void configureRuntimeIfNeeded(@NotNull String beforeFileName) throws IOException {
        if (beforeFileName.endsWith("JsRuntime.kt")) {
            // Without the following line of code subsequent tests with js-runtime will be prone to failure due "outdated stub in index" error.
            FileBasedIndex.getInstance().requestRebuild(StubUpdatingIndex.INDEX_ID);

            ConfigLibraryUtil.configureKotlinJsRuntimeAndSdk(getModule(), getFullJavaJDK());
        }
        else if (isRuntimeNeeded(beforeFileName)) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(getModule(), getFullJavaJDK());
        }
        else if (beforeFileName.contains("Runtime") || beforeFileName.contains("JsRuntime")) {
            Assert.fail("Runtime marker is used in test name, but not in test file end. " +
                        "This can lead to false-positive absent of actions");
        }
    }

    private static boolean isRuntimeNeeded(@NotNull String beforeFileName) throws IOException {
        return beforeFileName.endsWith("Runtime.kt") ||
               beforeFileName.toLowerCase().contains("createfromusage") ||
               InTextDirectivesUtils.isDirectiveDefined(FileUtil.loadFile(new File(beforeFileName)), "WITH_RUNTIME");
    }

    private void unConfigureRuntimeIfNeeded(@NotNull String beforeFileName) throws IOException {
        if (beforeFileName.endsWith("JsRuntime.kt")) {
            ConfigLibraryUtil.unConfigureKotlinJsRuntimeAndSdk(getModule(), getProjectJDK());
        }
        else if (isRuntimeNeeded(beforeFileName)) {
            ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(getModule(), getProjectJDK());
        }
    }

    private void enableInspections(String beforeFileName) throws IOException, ClassNotFoundException {
        File inspectionFile = QuickfixTestUtilsKt.findInspectionFile(new File(beforeFileName).getParentFile());
        if (inspectionFile != null) {
            String className = FileUtil.loadFile(inspectionFile).trim();
            Class<InspectionProfileEntry> inspectionClass = (Class<InspectionProfileEntry>) Class.forName(className);
            List<InspectionProfileEntry> tools = InspectionTestUtil.instantiateTools(
                    Collections.<Class<? extends InspectionProfileEntry>>singletonList(inspectionClass));
            enableInspectionTools(tools.get(0));
        }
    }

    private void checkForUnexpectedActions() throws ClassNotFoundException {
        String text = getEditor().getDocument().getText();
        ActionHint actionHint = ActionHint.parse(getFile(), text);
        if (!actionHint.shouldPresent()) {
            List<IntentionAction> actions = getAvailableActions();

            String prefix = "class ";
            if (actionHint.getExpectedText().startsWith(prefix)) {
                String className = actionHint.getExpectedText().substring(prefix.length());
                final Class<?> aClass = Class.forName(className);
                assert IntentionAction.class.isAssignableFrom(aClass) : className + " should be inheritor of IntentionAction";

                final Set<String> validActions =
                        new HashSet<String>(InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, "// ACTION:"));

                CollectionsKt.removeAll(actions, new Function1<IntentionAction, Boolean>() {
                    @Override
                    public Boolean invoke(IntentionAction action) {
                        return !aClass.isAssignableFrom(action.getClass()) || validActions.contains(action.getText());
                    }
                });

                if (!actions.isEmpty()) {
                    Assert.fail("Unexpected intention actions present\n " +
                                CollectionsKt.map(actions, new Function1<IntentionAction, String>() {
                                    @Override
                                    public String invoke(IntentionAction action) {
                                        return action.getClass().toString() + " " + action.toString() + "\n";
                                    }
                                })
                    );
                }

                for (IntentionAction action : actions) {
                    if (aClass.isAssignableFrom(action.getClass()) && !validActions.contains(action.getText())) {
                        Assert.fail("Unexpected intention action " + action.getClass() + " found");
                    }
                }
            }
            else {
                // Action shouldn't be found. Check that other actions are expected and thus tested action isn't there under another name.
                DirectiveBasedActionUtils.INSTANCE.checkAvailableActionsAreExpected(getFile(), actions);
            }
        }
    }

    public static void checkForUnexpectedErrors() {
        DirectiveBasedActionUtils.INSTANCE.checkForUnexpectedErrors((KtFile) getFile());
    }

    @Override
    protected IntentionAction findActionWithText(String text) {
        IntentionAction intention = super.findActionWithText(text);
        if (intention != null) return intention;

        // Support warning suppression
        int caretOffset = myEditor.getCaretModel().getOffset();
        for (HighlightInfo highlight : doHighlighting()) {
            if (highlight.startOffset <= caretOffset && caretOffset <= highlight.endOffset) {
                ProblemGroup group = highlight.getProblemGroup();
                if (group instanceof SuppressableProblemGroup) {
                    SuppressableProblemGroup problemGroup = (SuppressableProblemGroup) group;
                    PsiElement at = getFile().findElementAt(highlight.getActualStartOffset());
                    SuppressIntentionAction[] actions = problemGroup.getSuppressActions(at);
                    for (SuppressIntentionAction action : actions) {
                        if (action.getText().equals(text)) {
                            return action;
                        }
                    }
                }
            }
        }
        return null;
    }

    @Override
    protected void checkResultByText(String message, @NotNull String fileText, boolean ignoreTrailingSpaces, String filePath) {
        super.checkResultByText(message, fileText, ignoreTrailingSpaces, new File(filePath).getAbsolutePath());
    }

    @Override
    protected String getBasePath() {
        return KotlinTestUtils.getTestsRoot(getClass());
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "./";
    }

    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.mockJdk();
    }

    protected static Sdk getFullJavaJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }
}
