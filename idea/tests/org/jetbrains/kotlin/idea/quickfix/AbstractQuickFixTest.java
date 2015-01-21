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
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInspection.SuppressIntentionAction;
import com.intellij.codeInspection.SuppressableProblemGroup;
import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.lang.annotation.ProblemGroup;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiElement;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.DirectiveBasedActionUtils;
import org.jetbrains.kotlin.idea.KotlinLightQuickFixTestCase;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
        boolean isWithRuntime = beforeFileName.endsWith("Runtime.kt");

        try {
            if (isWithRuntime) {
                ConfigLibraryUtil.configureKotlinRuntime(getModule(), getFullJavaJDK());
            }

            enableInspections(beforeFileName);

            doSingleTest(getTestName(false) + ".kt");
            checkForUnexpectedActions();
            checkForUnexpectedErrors();
        }
        finally {
            if (isWithRuntime) {
                ConfigLibraryUtil.unConfigureKotlinRuntime(getModule(), getProjectJDK());
            }
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
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    protected static Sdk getFullJavaJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }
}
