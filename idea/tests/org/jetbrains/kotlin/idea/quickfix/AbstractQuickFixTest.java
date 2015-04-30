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
import com.intellij.codeInsight.daemon.quickFix.QuickFixTestCase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.SuppressIntentionAction;
import com.intellij.codeInspection.SuppressableProblemGroup;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.lang.annotation.ProblemGroup;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.rt.execution.junit.FileComparisonFailure;
import kotlin.KotlinPackage;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.KotlinLightQuickFixTestCase;
import org.jetbrains.kotlin.idea.js.KotlinJavaScriptLibraryManager;
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
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

    @Nullable
    private static File findInspectionFile(@NotNull File startDir) {
        File currentDir = startDir;
        while (currentDir != null) {
            File inspectionFile = new File(currentDir, ".inspection");
            if (inspectionFile.exists()) {
                return inspectionFile;
            }
            currentDir = currentDir.getParentFile();
        }
        return null;
    }

    protected void doTest(@NotNull String beforeFileName) throws Exception {
        try {
            configureRuntimeIfNeeded(beforeFileName);

            enableInspections(beforeFileName);

            doSingleTest(getTestName(false) + ".kt");
            checkForUnexpectedActions();
            checkForUnexpectedErrors();
        }
        finally {
            unConfigureRuntimeIfNeeded(beforeFileName);
        }
    }

    //region Severe hack - lot of code copied from LightQuickFixTestCase to workaround stupid format of test data with before/after prefixes
    @Override
    protected void doSingleTest(String fileSuffix) {
        doTestFor(fileSuffix, createWrapper());
    }

    private static QuickFixTestCase myWrapper;

    private static void doTestFor(final String testName, final QuickFixTestCase quickFixTestCase) {
        String relativePath = notNull(quickFixTestCase.getBasePath(), "") + "/" + KotlinPackage.decapitalize(testName);
        final String testFullPath = quickFixTestCase.getTestDataPath().replace(File.separatorChar, '/') + relativePath;
        final File testFile = new File(testFullPath);
        CommandProcessor.getInstance().executeCommand(quickFixTestCase.getProject(), new Runnable() {
            @SuppressWarnings({"AssignmentToStaticFieldFromInstanceMethod", "CallToPrintStackTrace"})
            @Override
            public void run() {
                try {
                    String contents = StringUtil.convertLineSeparators(FileUtil.loadFile(testFile, CharsetToolkit.UTF8_CHARSET));
                    quickFixTestCase.configureFromFileText(testFile.getName(), contents);
                    quickFixTestCase.bringRealEditorBack();
                    Pair<String, Boolean> pair = quickFixTestCase.parseActionHintImpl(quickFixTestCase.getFile(), contents);
                    String text = pair.getFirst();
                    boolean actionShouldBeAvailable = pair.getSecond().booleanValue();

                    quickFixTestCase.beforeActionStarted(testName, contents);

                    try {
                        myWrapper = quickFixTestCase;
                        quickFixTestCase.doAction(text, actionShouldBeAvailable, testFullPath, testName);
                    }
                    finally {
                        myWrapper = null;
                        quickFixTestCase.afterActionCompleted(testName, contents);
                    }
                }
                catch (FileComparisonFailure e) {
                    throw e;
                }
                catch (Throwable e) {
                    e.printStackTrace();
                    fail(testName);
                }
            }
        }, "", "");
    }

    @Override
    protected void doAction(String text, boolean actionShouldBeAvailable, String testFullPath, String testName)
            throws Exception {
        doAction(text, actionShouldBeAvailable, testFullPath, testName, myWrapper);
    }

    @Override
    protected void checkResultByFile(@Nullable String message, @NotNull String filePath, boolean ignoreTrailingSpaces) {
        File file = new File(filePath);
        String afterFileName = file.getName();
        assert afterFileName.startsWith(AFTER_PREFIX);
        String newAfterFileName = KotlinPackage.decapitalize(afterFileName.substring(AFTER_PREFIX.length())) + ".after";

        super.checkResultByFile(message, new File(file.getParent(), newAfterFileName).getPath(), ignoreTrailingSpaces);
    }
    //endregion

    private void configureRuntimeIfNeeded(@NotNull String beforeFileName) {
        if (beforeFileName.endsWith("JsRuntime.kt")) {
            ConfigLibraryUtil.configureKotlinJsRuntimeAndSdk(getModule(), getFullJavaJDK());
            KotlinJavaScriptLibraryManager.getInstance(getProject()).syncUpdateProjectLibrary();
        }
        else if (beforeFileName.endsWith("Runtime.kt")) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(getModule(), getFullJavaJDK());
        }
    }

    private void unConfigureRuntimeIfNeeded(@NotNull String beforeFileName) {
        if (beforeFileName.endsWith("JsRuntime.kt")) {
            ConfigLibraryUtil.unConfigureKotlinJsRuntimeAndSdk(getModule(), getProjectJDK());
        }
        else if (beforeFileName.endsWith("Runtime.kt")) {
            ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(getModule(), getProjectJDK());
        }
    }

    private void enableInspections(String beforeFileName) throws IOException, ClassNotFoundException {
        File inspectionFile = findInspectionFile(new File(beforeFileName).getParentFile());
        if (inspectionFile != null) {
            String className = FileUtil.loadFile(inspectionFile).trim();
            Class<?> inspectionClass = Class.forName(className);
            enableInspectionTools(inspectionClass);
        }
    }

    private void checkForUnexpectedActions() throws ClassNotFoundException {
        String text = getEditor().getDocument().getText();
        Pair<String, Boolean> pair = parseActionHintImpl(getFile(), text);
        if (!pair.second) {
            List<IntentionAction> actions = getAvailableActions();

            String prefix = "class ";
            if (pair.first.startsWith(prefix)) {
                String className = pair.first.substring(prefix.length());
                Class<?> aClass = Class.forName(className);
                assert IntentionAction.class.isAssignableFrom(aClass) : className + " should be inheritor of IntentionAction";

                Set<String> validActions = new HashSet<String>(InTextDirectivesUtils.findLinesWithPrefixesRemoved(text, "// ACTION:"));

                for (IntentionAction action : actions) {
                    if (aClass.isAssignableFrom(action.getClass()) && !validActions.contains(action.getText())) {
                        Assert.fail("Unexpected intention action " + action.getClass() + " found");
                    }
                }
            }
            else {
                // Action shouldn't be found. Check that other actions are expected and thus tested action isn't there under another name.
                DirectiveBasedActionUtils.checkAvailableActionsAreExpected((JetFile) getFile(), actions);
            }
        }
    }

    public static void checkForUnexpectedErrors() {
        DirectiveBasedActionUtils.checkForUnexpectedErrors((JetFile) getFile());
    }

    @Override
    protected IntentionAction findActionWithText(String text) {
        IntentionAction intention = super.findActionWithText(text);
        if (intention != null) return intention;

        // Support warning suppression
        for (HighlightInfo highlight : doHighlighting()) {
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
        return null;
    }

    @Override
    protected void checkResultByText(String message, @NotNull String fileText, boolean ignoreTrailingSpaces, String filePath) {
        super.checkResultByText(message, fileText, ignoreTrailingSpaces, new File(filePath).getAbsolutePath());
    }

    @Override
    protected String getBasePath() {
        return getClass().getAnnotation(TestMetadata.class).value();
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
