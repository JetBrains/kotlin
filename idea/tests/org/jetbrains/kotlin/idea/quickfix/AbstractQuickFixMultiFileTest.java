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
import com.intellij.codeInsight.daemon.quickFix.ActionHint;
import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.intention.impl.ShowIntentionActionsHandler;
import com.intellij.codeInspection.InspectionEP;
import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.LocalInspectionEP;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.VfsTestUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.UIUtil;
import junit.framework.ComparisonFailure;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
import java.util.*;
import java.util.regex.Pattern;

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

    @NotNull
    private static String extraFileNamePrefix(@NotNull String mainFileName) {
        return mainFileName.replace(".Main.kt", ".").replace(".Main.java", ".");
    }

    protected static FileType guessFileType(TestFile file) {
        if (file.path.contains("." + KotlinFileType.EXTENSION)) {
            return KotlinFileType.INSTANCE;
        }
        else if (file.path.contains("." + JavaFileType.DEFAULT_EXTENSION)) {
            return JavaFileType.INSTANCE;
        }
        else {
            return PlainTextFileType.INSTANCE;
        }
    }

    /**
     * @param sourceRootDir Base path of test file(Test source directory)
     * @param testFile      source of VFile content
     * @return created VirtualFile
     */
    protected static VirtualFile createVirtualFileFromTestFile(File sourceRootDir, final TestFile testFile) {
        try {
            assertFalse("Please don't use absolute path for multifile test 'FILE' directive: " + testFile.path,
                        FileUtil.isAbsolutePlatformIndependent(testFile.path));
            FileType fileType = guessFileType(testFile);
            String extension = fileType.getDefaultExtension();


            final File fileInSourceRoot = new File(testFile.path);
            File container = FileUtil.getParentFile(fileInSourceRoot);
            if (container == null) {
                container = sourceRootDir;
            }
            else {
                container = new File(sourceRootDir, container.getPath());
            }

            if (!container.exists()) {
                assertTrue(container.mkdirs());
            }

            final File tempFile =
                    FileUtil.createTempFile(container, FileUtil.getNameWithoutExtension(testFile.path), "." + extension, true);


            final VirtualFile vFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempFile);
            assert vFile != null;
            new WriteAction() {
                @Override
                protected void run(@NotNull Result result) throws Throwable {
                    vFile.setCharset(CharsetToolkit.UTF8_CHARSET);
                    VfsUtil.saveText(vFile, testFile.content);
                }
            }.execute();


            return vFile;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = new String[] {"excludedPackage", "somePackage.ExcludedClass"};
    }

    @Override
    protected void tearDown() throws Exception {
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = ArrayUtil.EMPTY_STRING_ARRAY;
        super.tearDown();
    }

    /**
     * @param subFiles   subFiles of multiFile test
     * @param beforeFile will be added last, as subFiles are dependencies of it
     */
    protected void configureMultiFileTest(List<TestFile> subFiles, TestFile beforeFile) {
        try {
            File sourceRootDir = createTempDirectory();
            Map<TestFile, VirtualFile> virtualFiles = new HashMap<TestFile, VirtualFile>();

            for (TestFile file : subFiles) {
                virtualFiles.put(file, createVirtualFileFromTestFile(sourceRootDir, file));
            }
            virtualFiles.put(beforeFile, createVirtualFileFromTestFile(sourceRootDir, beforeFile));

            VirtualFile sourceRootVFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(sourceRootDir);
            PsiTestUtil.addSourceRoot(myModule, sourceRootVFile);

            for (TestFile file : subFiles) {
                configureByExistingFile(virtualFiles.get(file));
                assertEquals(guessFileType(file), myFile.getVirtualFile().getFileType());
            }

            configureByExistingFile(virtualFiles.get(beforeFile));
            assertEquals(guessFileType(beforeFile), myFile.getVirtualFile().getFileType());

            assertTrue("\"<caret>\" is probably missing in file \"" + beforeFile.path + "\"", myEditor.getCaretModel().getOffset() != 0);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    protected void doMultiFileTest(final String beforeFileName) throws Exception {
        String multifileText = FileUtil.loadFile(new File(beforeFileName), true);

        boolean withRuntime = InTextDirectivesUtils.isDirectiveDefined(multifileText, "// WITH_RUNTIME");
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, PluginTestCaseBase.mockJdk());
        }

        try {
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
                    return file.path.contains(".after");
                }
            });
            final TestFile beforeFile = CollectionsKt.firstOrNull(subFiles, new Function1<TestFile, Boolean>() {
                @Override
                public Boolean invoke(TestFile file) {
                    return file.path.contains(".before");
                }
            });

            assert beforeFile != null;

            subFiles.remove(beforeFile);
            if (afterFile != null) {
                subFiles.remove(afterFile);
            }

            configureMultiFileTest(subFiles, beforeFile);

            CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
                @Override
                public void run() {
                    try {
                        PsiFile psiFile = getFile();

                        ActionHint actionHint = ActionHint.parse(psiFile, beforeFile.content);
                        String text = actionHint.getExpectedText();

                        boolean actionShouldBeAvailable = actionHint.shouldPresent();

                        if (psiFile instanceof KtFile) {
                            DirectiveBasedActionUtils.INSTANCE.checkForUnexpectedErrors((KtFile) psiFile);
                        }

                        doAction(text, actionShouldBeAvailable, getTestName(false));

                        String actualText = getFile().getText();
                        String afterText =
                                new StringBuilder(actualText).insert(getEditor().getCaretModel().getOffset(), "<caret>").toString();

                        if (actionShouldBeAvailable) {
                            assertNotNull(".after file should exist", afterFile);
                            if (!afterText.equals(afterFile.content)) {
                                StringBuilder actualTestFile = new StringBuilder();
                                actualTestFile.append("// FILE: ").append(beforeFile.path).append("\n").append(beforeFile.content);
                                for (TestFile file : subFiles) {
                                    actualTestFile.append("// FILE: ").append(file.path).append("\n").append(file.content);
                                }
                                actualTestFile.append("// FILE: ").append(afterFile.path).append("\n").append(afterText);

                                KotlinTestUtils.assertEqualsToFile(new File(beforeFileName), actualTestFile.toString());
                            }
                        }
                        else {
                            assertNull(".after file should not exist", afterFile);
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

    private void doTest(final String beforeFileName, boolean withExtraFile) throws Exception {
        String testDataPath = getTestDataPath();
        File mainFile = new File(testDataPath + beforeFileName);
        final String originalFileText = FileUtil.loadFile(mainFile, true);

        boolean withRuntime = InTextDirectivesUtils.isDirectiveDefined(originalFileText, "// WITH_RUNTIME");
        boolean fullJdk = InTextDirectivesUtils.isDirectiveDefined(originalFileText, "// FULL_JDK");
        if (withRuntime) {
            ConfigLibraryUtil.configureKotlinRuntimeAndSdk(myModule, fullJdk ? PluginTestCaseBase.fullJdk() : PluginTestCaseBase.mockJdk());
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

                        ActionHint actionHint = ActionHint.parse(psiFile, originalFileText);
                        String text = actionHint.getExpectedText();

                        boolean actionShouldBeAvailable = actionHint.shouldPresent();

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
                ConfigLibraryUtil
                        .unConfigureKotlinRuntimeAndSdk(myModule, fullJdk ? PluginTestCaseBase.fullJdk() : PluginTestCaseBase.mockJdk());
            }
        }
    }

    @SuppressWarnings({"HardCodedStringLiteral"})
    public void doAction(String text, boolean actionShouldBeAvailable, String testFilePath) throws Exception {
        Pattern pattern = text.startsWith("/")
                          ? Pattern.compile(text.substring(1, text.length()-1))
                          : Pattern.compile(StringUtil.escapeToRegexp(text));

        List<IntentionAction> availableActions = getAvailableActions();
        IntentionAction action = findActionByPattern(pattern, availableActions);

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
                IntentionAction afterAction = findActionByPattern(pattern, getAvailableActions());

                if (afterAction != null) {
                    fail("Action '" + text + "' is still available after its invocation in test " + testFilePath);
                }
            }
        }
    }

    @Nullable
    private static IntentionAction findActionByPattern(Pattern pattern, List<IntentionAction> availableActions) {
        for (IntentionAction availableAction : availableActions) {
            if (pattern.matcher(availableAction.getText()).matches()) {
                return availableAction;
            }
        }
        return null;
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
    private VirtualFile findVirtualFile(@NotNull String filePath) {
        String absolutePath = getTestDataPath() + filePath;
        return VfsTestUtil.findFileByCaseSensitivePath(absolutePath);
    }

    private static class TestFile {
        public final String path;
        public final String content;

        TestFile(String path, String content) {
            this.path = path;
            this.content = content;
        }
    }
}
