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
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.ui.UIUtil;
import junit.framework.ComparisonFailure;
import kotlin.KotlinPackage;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinDaemonAnalyzerTestCase;
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil;
import org.jetbrains.kotlin.idea.test.DirectiveBasedActionUtils;
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.test.InTextDirectivesUtils;
import org.jetbrains.kotlin.test.JetTestUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
        doTest(beforeFileName, true);
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
                final String extraFileNamePrefix = mainFileName.replace(".Main.kt", ".");
                File[] extraFiles = mainFileDir.listFiles(
                        new FilenameFilter() {
                            @Override
                            public boolean accept(@NotNull File dir, @NotNull String name) {
                                return name.startsWith(extraFileNamePrefix) && !name.equals(mainFileName);
                            }
                        }
                );
                assert extraFiles != null;

                List<String> testFiles = new ArrayList<String>();
                testFiles.add(beforeFileName);
                KotlinPackage.mapTo(
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

                        if (psiFile instanceof JetFile) {
                            DirectiveBasedActionUtils.checkForUnexpectedErrors((JetFile) psiFile);
                        }

                        doAction(text, actionShouldBeAvailable, beforeFileName);
                    }
                    catch (ComparisonFailure e) {
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
    public void doAction(String text, boolean actionShouldBeAvailable, String testFullPath)
            throws Exception {
        List<IntentionAction> availableActions = getAvailableActions();
        IntentionAction action = LightQuickFixTestCase.findActionWithText(availableActions, text);

        if (action == null) {
            if (actionShouldBeAvailable) {
                List<String> texts = getActionsTexts(availableActions);
                Collection<HighlightInfo> infos = doHighlighting();
                fail("Action with text '" + text + "' is not available in test " + testFullPath + "\n" +
                     "Available actions (" + texts.size() + "): " + texts + "\n" +
                     availableActions + "\n" +
                     "Infos:" + infos);
            }
            else {
                DirectiveBasedActionUtils.checkAvailableActionsAreExpected((JetFile) getFile(), availableActions);
            }
        }
        else {
            if (!actionShouldBeAvailable) {
                fail("Action '" + text + "' is available (but must not) in test " + testFullPath);
            }

            ShowIntentionActionsHandler.chooseActionAndInvoke(getFile(), getEditor(), action, action.getText());

            UIUtil.dispatchAllInvocationEvents();

            if (!shouldBeAvailableAfterExecution()) {
                IntentionAction afterAction = LightQuickFixTestCase.findActionWithText(getAvailableActions(), text);

                if (afterAction != null) {
                    fail("Action '" + text + "' is still available after its invocation in test " + testFullPath);
                }
            }

            checkResultByFile(testFullPath.replace(".before.Main.", ".after."));

            PsiFile mainFile = myFile;
            String mainFileName = mainFile.getName();
            String extraFileNamePrefix = mainFileName.replace(".Main.kt", ".");
            for (PsiFile file : mainFile.getContainingDirectory().getFiles()) {
                String fileName = file.getName();
                if (fileName.equals(mainFileName) || !fileName.startsWith(extraFileNamePrefix)) continue;

                myFile = file;
                String extraFileFullPath = testFullPath.replace(mainFileName, fileName);
                try {
                    checkResultByFile(extraFileFullPath.replace(".before.", ".after."));
                }
                catch (AssertionError e) {
                    if (e.getMessage().startsWith("Cannot find file")) {
                        checkResultByFile(extraFileFullPath);
                    }
                    else throw e;
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
        return JetTestUtils.getHomeDirectory() + "/";
    }
}
