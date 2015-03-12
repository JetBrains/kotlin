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

package org.jetbrains.kotlin.js.test;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsNode;
import com.google.dart.compiler.backend.js.ast.JsProgram;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.StandardFileSystems;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection;
import org.jetbrains.kotlin.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.js.JavaScript;
import org.jetbrains.kotlin.js.config.Config;
import org.jetbrains.kotlin.js.config.EcmaVersion;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfig;
import org.jetbrains.kotlin.js.config.LibrarySourcesConfigWithCaching;
import org.jetbrains.kotlin.js.facade.K2JSTranslator;
import org.jetbrains.kotlin.js.facade.MainCallParameters;
import org.jetbrains.kotlin.js.facade.TranslationResult;
import org.jetbrains.kotlin.js.test.rhino.RhinoResultChecker;
import org.jetbrains.kotlin.js.test.utils.JsTestUtils;
import org.jetbrains.kotlin.js.translate.context.Namer;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.psi.JetFile;
import org.jetbrains.kotlin.psi.JetPsiFactory;
import org.jetbrains.kotlin.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.test.JetTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.js.test.rhino.RhinoUtils.runRhinoTest;
import static org.jetbrains.kotlin.js.test.utils.JsTestUtils.convertFileNameToDotJsFile;

public abstract class BasicTest extends KotlinTestWithEnvironment {
    // predictable order of ecma version in tests
    protected static final Iterable<EcmaVersion> DEFAULT_ECMA_VERSIONS = Lists.newArrayList(EcmaVersion.v5);

    private static final boolean DELETE_OUT = false;

    public static final String TEST_DATA_DIR_PATH = "js/js.translator/testData/";
    public static final String DIST_DIR_PATH = "dist/";

    private static final String CASES = "cases/";
    private static final String OUT = "out/";
    private static final String EXPECTED = "expected/";
    private static final String COMMON_FILES_DIR = "_commonFiles/";

    public static final String TEST_MODULE = "JS_TESTS";
    public static final String TEST_PACKAGE = "foo";
    public static final String TEST_FUNCTION = "box";
    public static final boolean IS_INLINE_ENABLED = true;

    @NotNull
    private String relativePathToTestDir = "";

    @SuppressWarnings("JUnitTestCaseWithNonTrivialConstructors")
    public BasicTest(@NotNull String relativePathToTestDir) {
        this.relativePathToTestDir = relativePathToTestDir;
    }

    protected abstract void checkFooBoxIsOkByPath(String filePath) throws Exception;

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return JetCoreEnvironment.createForTests(getTestRootDisposable(), new CompilerConfiguration(), EnvironmentConfigFiles.JS_CONFIG_FILES);
    }

    protected boolean shouldCreateOut() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        if (!shouldCreateOut()) {
            return;
        }
        File outDir = new File(getOutputPath());

        JetTestUtils.mkdirs(outDir);
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        //noinspection ConstantConditions,PointlessBooleanExpression
        if (!shouldCreateOut() || !DELETE_OUT) {
            return;
        }
        File outDir = new File(getOutputPath());
        assert outDir.exists();
        boolean success = FileUtil.delete(outDir);
        assert success;
    }

    public void doTest(@NotNull String filePath) {
        try {
            checkFooBoxIsOkByPath(filePath);
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void generateJavaScriptFiles(
            @NotNull String kotlinFilePath,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull Iterable<EcmaVersion> ecmaVersions
    ) throws Exception {
        generateJavaScriptFiles(Collections.singletonList(kotlinFilePath), getBaseName(kotlinFilePath), mainCallParameters, ecmaVersions);
    }

    protected void generateJavaScriptFiles(
            @NotNull List<String> files,
            @NotNull String testName,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull Iterable<EcmaVersion> ecmaVersions
    ) throws Exception {
        generateJavaScriptFiles(files, testName, mainCallParameters, ecmaVersions, TEST_MODULE, null);
    }

    protected void generateJavaScriptFiles(
            @NotNull List<String> files,
            @NotNull String testName,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull String moduleName,
            @Nullable List<String> libraries
    ) throws Exception {
        Project project = getProject();
        List<String> allFiles = withAdditionalKotlinFiles(files);
        List<JetFile> jetFiles = createJetFileList(project, allFiles, null);

        for (EcmaVersion version : ecmaVersions) {
            Config config = createConfig(getProject(), moduleName, version, libraries);
            File outputFile = new File(getOutputFilePath(testName, version));

            translateFiles(jetFiles, outputFile, mainCallParameters, config);
        }
    }

    protected void translateFiles(
            @NotNull List<JetFile> jetFiles,
            @NotNull File outputFile,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull Config config
    ) throws Exception {
        K2JSTranslator translator = new K2JSTranslator(config);
        TranslationResult translationResult = translator.translate(jetFiles, mainCallParameters);

        if (!(translationResult instanceof TranslationResult.Success)) return;

        TranslationResult.Success successResult = (TranslationResult.Success) translationResult;
        processJsProgram(successResult.getProgram());

        OutputFileCollection outputFiles = successResult.getOutputFiles(outputFile, getOutputPrefixFile(), getOutputPostfixFile());
        File outputDir = outputFile.getParentFile();
        assert outputDir != null : "Parent file for output file should not be null, outputFilePath: " + outputFile.getPath();
        OutputUtilsPackage.writeAllTo(outputFiles, outputDir);
    }

    protected File getOutputPostfixFile() {
        return null;
    }

    protected File getOutputPrefixFile() {
        return null;
    }

    protected boolean shouldBeTranslateAsUnitTestClass() {
        return false;
    }

    protected boolean shouldGenerateSourceMap() {
        return false;
    }

    protected void processJsProgram(@NotNull JsProgram program) throws Exception { }

    protected void runRhinoTests(
            @NotNull String testName,
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull RhinoResultChecker checker
    ) throws Exception {
        for (EcmaVersion ecmaVersion : ecmaVersions) {
            runRhinoTest(withAdditionalJsFiles(getOutputFilePath(testName, ecmaVersion), ecmaVersion),
                         checker,
                         getRhinoTestVariables(),
                         ecmaVersion);
        }
    }

    protected Map<String, Object> getRhinoTestVariables() throws Exception {
        return null;
    }

    @NotNull
    protected List<String> additionalKotlinFiles() {
        List<String> additionalFiles = Lists.newArrayList();

        // add all kotlin files from testData/_commonFiles
        additionalFiles.addAll(JsTestUtils.getFilesInDirectoryByExtension(TEST_DATA_DIR_PATH + COMMON_FILES_DIR, JetFileType.EXTENSION));
        // add all kotlin files from <testDir>/_commonFiles
        additionalFiles.addAll(JsTestUtils.getFilesInDirectoryByExtension(pathToTestDir() + COMMON_FILES_DIR, JetFileType.EXTENSION));

        return additionalFiles;
    }

    @NotNull
    protected List<String> additionalJsFiles(@NotNull EcmaVersion ecmaVersion) {
        List<String> additionalFiles = Lists.newArrayList();

        // add all js files from testData/_commonFiles
        additionalFiles.addAll(JsTestUtils.getFilesInDirectoryByExtension(TEST_DATA_DIR_PATH + COMMON_FILES_DIR, JavaScript.EXTENSION));
        // add all js files from <testDir>/_commonFiles
        additionalFiles.addAll(JsTestUtils.getFilesInDirectoryByExtension(pathToTestDir() + COMMON_FILES_DIR, JavaScript.EXTENSION));

        // add <testDir>/cases/<testName>.js if it exists
        String jsFilePath = getInputFilePath(getTestName(true) + JavaScript.DOT_EXTENSION);
        File jsFile = new File(jsFilePath);
        if (jsFile.exists() && jsFile.isFile()) {
            additionalFiles.add(jsFilePath);
        }

        return additionalFiles;
    }

    // helpers

    @NotNull
    protected final String pathToTestDir() {
        return TEST_DATA_DIR_PATH + relativePathToTestDir;
    }

    @NotNull
    protected final String getOutputFilePath(@NotNull String testName, @NotNull EcmaVersion ecmaVersion) {
        return getOutputPath() + convertFileNameToDotJsFile(testName, ecmaVersion);
    }

    @NotNull
    protected final String getInputFilePath(@NotNull String filename) {
        return getInputPath() + filename;
    }

    @NotNull
    protected final String expectedFilePath(@NotNull String testName) {
        return getExpectedPath() + testName + ".out";
    }

    @NotNull
    private Config createConfig(@NotNull Project project, @NotNull String moduleId, @NotNull EcmaVersion ecmaVersion, @Nullable List<String> libraries) {
        if (libraries == null) {
            return new LibrarySourcesConfigWithCaching(project, moduleId, ecmaVersion, shouldGenerateSourceMap(), IS_INLINE_ENABLED, shouldBeTranslateAsUnitTestClass());
        }
        else {
            return new LibrarySourcesConfig(project, moduleId, librariesWithJsStdlib(libraries), ecmaVersion, shouldGenerateSourceMap(), IS_INLINE_ENABLED);
        }
    }

    @NotNull
    private static List<String> librariesWithJsStdlib(@NotNull List<String> libraries) {
        List<String> result = new ArrayList<String>(libraries);
        result.addAll(0, LibrarySourcesConfigWithCaching.JS_STDLIB);
        return result;
    }

    @NotNull
    private String getOutputPath() {
        return pathToTestDir() + OUT;
    }

    @NotNull
    protected String getInputPath() {
        return pathToTestDir() + CASES;
    }

    @NotNull
    private String getExpectedPath() {
        return pathToTestDir() + EXPECTED;
    }

    @NotNull
    private List<String> withAdditionalKotlinFiles(@NotNull List<String> files) {
        List<String> result = Lists.newArrayList(files);
        result.addAll(additionalKotlinFiles());
        return result;
    }

    @NotNull
    private List<String> withAdditionalJsFiles(@NotNull String inputFile, @NotNull EcmaVersion ecmaVersion) {
        List<String> allFiles = Lists.newArrayList(additionalJsFiles(ecmaVersion));
        allFiles.add(inputFile);
        return allFiles;
    }

    private static List<JetFile> createJetFileList(@NotNull Project project, @NotNull List<String> list, @Nullable String root) {
        List<JetFile> libFiles = Lists.newArrayList();

        PsiManager psiManager = PsiManager.getInstance(project);
        VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(StandardFileSystems.FILE_PROTOCOL);

        VirtualFile rootFile = root == null ? null : fileSystem.findFileByPath(root);

        for (String libFileName : list) {
            VirtualFile virtualFile = rootFile == null ? fileSystem.findFileByPath(libFileName) : rootFile.findFileByRelativePath(libFileName);
            //TODO logging?
            assert virtualFile != null;
            PsiFile psiFile = psiManager.findFile(virtualFile);
            libFiles.add((JetFile) psiFile);
        }
        return libFiles;
    }

    @NotNull
    protected String getPackageName(@NotNull String filename) throws IOException {
        String content = FileUtil.loadFile(new File(filename), true);
        JetPsiFactory psiFactory = new JetPsiFactory(getProject());
        JetFile jetFile = psiFactory.createFile(content);
        String packageName = jetFile.getPackageFqName().asString();
        return packageName.isEmpty() ? Namer.getRootPackageName() : packageName;
    }

    protected static String getBaseName(String path) {
        String systemIndependentPath = FileUtil.toSystemIndependentName(path);

        int start = systemIndependentPath.lastIndexOf("/");
        if (start == -1) {
            start = 0;
        }

        int end = systemIndependentPath.lastIndexOf(".");
        if (end == -1) {
            end = path.length();
        }

        return path.substring(start, end);
    }
}
