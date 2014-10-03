/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.k2js.test;

import com.google.common.collect.Lists;
import com.google.dart.compiler.backend.js.ast.JsNode;
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
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.OutputFileCollection;
import org.jetbrains.jet.cli.common.output.outputUtils.OutputUtilsPackage;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.k2js.config.Config;
import org.jetbrains.k2js.config.EcmaVersion;
import org.jetbrains.k2js.facade.MainCallParameters;
import org.jetbrains.k2js.test.config.LibrarySourcesConfigWithCaching;
import org.jetbrains.k2js.test.rhino.RhinoResultChecker;
import org.jetbrains.k2js.test.utils.JsTestUtils;
import org.jetbrains.k2js.translate.context.Namer;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.k2js.facade.K2JSTranslator.translateWithMainCallParameters;
import static org.jetbrains.k2js.test.rhino.RhinoUtils.runRhinoTest;
import static org.jetbrains.k2js.test.utils.JsTestUtils.convertFileNameToDotJsFile;

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

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return JetCoreEnvironment.createForTests(getTestRootDisposable(), new CompilerConfiguration());
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

    protected void generateJavaScriptFiles(
            @NotNull String kotlinFilename,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull Iterable<EcmaVersion> ecmaVersions
    ) throws Exception {
        generateJavaScriptFiles(Collections.singletonList(getInputFilePath(kotlinFilename)),
                                kotlinFilename, mainCallParameters, ecmaVersions);
    }

    protected void generateJavaScriptFiles(
            @NotNull List<String> files,
            @NotNull String testName,
            @NotNull MainCallParameters mainCallParameters,
            @NotNull Iterable<EcmaVersion> ecmaVersions
    ) throws Exception {
        Project project = getProject();
        List<String> allFiles = withAdditionalKotlinFiles(files);
        List<JetFile> jetFiles = createJetFileList(project, allFiles, null);

        for (EcmaVersion version : ecmaVersions) {
            Config config = createConfig(getProject(), TEST_MODULE, version);
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
        //noinspection unchecked
        OutputFileCollection outputFiles =
                translateWithMainCallParameters(mainCallParameters, jetFiles, outputFile,
                                                getOutputPrefixFile(), getOutputPostfixFile(),
                                                config, getConsumer());

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

    protected boolean shouldGenerateSourcemap() {
        return false;
    }

    protected Consumer<JsNode> getConsumer() {
        //noinspection unchecked
        return Consumer.EMPTY_CONSUMER;
    }

    protected void runRhinoTests(
            @NotNull String filename, 
            @NotNull Iterable<EcmaVersion> ecmaVersions,
            @NotNull RhinoResultChecker checker
    ) throws Exception {
        for (EcmaVersion ecmaVersion : ecmaVersions) {
            runRhinoTest(withAdditionalJsFiles(getOutputFilePath(filename, ecmaVersion), ecmaVersion),
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
        additionalFiles.addAll(JsTestUtils.kotlinFilesInDirectory(TEST_DATA_DIR_PATH + COMMON_FILES_DIR));
        additionalFiles.addAll(JsTestUtils.kotlinFilesInDirectory(pathToTestDir() + COMMON_FILES_DIR));
        return additionalFiles;
    }

    @NotNull
    protected List<String> additionalJsFiles(@NotNull EcmaVersion ecmaVersion) {
        return Lists.newArrayList();
    }

    // helpers

    @NotNull
    protected final String pathToTestDir() {
        return TEST_DATA_DIR_PATH + relativePathToTestDir;
    }

    @NotNull
    protected final String getOutputFilePath(@NotNull String filename, @NotNull EcmaVersion ecmaVersion) {
        return getOutputPath() + convertFileNameToDotJsFile(filename, ecmaVersion);
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
    private Config createConfig(@NotNull Project project, @NotNull String moduleId, @NotNull EcmaVersion ecmaVersion) {
        return new LibrarySourcesConfigWithCaching(project, moduleId, ecmaVersion,
                                                   shouldGenerateSourcemap(), IS_INLINE_ENABLED, shouldBeTranslateAsUnitTestClass());
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
}
