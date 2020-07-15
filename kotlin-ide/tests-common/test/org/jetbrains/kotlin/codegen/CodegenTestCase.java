/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.TestDataFile;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import kotlin.io.FilesKt;
import kotlin.text.Charsets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.TestHelperGeneratorKt;
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.JVMConfigurationKeys;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class CodegenTestCase extends KotlinBaseTest<KotlinBaseTest.TestFile> {
    private static final String DEFAULT_TEST_FILE_NAME = "a_test";
    private static final String DEFAULT_JVM_TARGET = System.getProperty("kotlin.test.default.jvm.target");

    protected KotlinCoreEnvironment myEnvironment;
    protected CodegenTestFiles myFiles;
    protected ClassFileFactory classFileFactory;
    protected GeneratedClassLoader initializedClassLoader;

    protected final void createEnvironmentWithMockJdkAndIdeaAnnotations(
            @NotNull ConfigurationKind configurationKind,
            @NotNull File... javaSourceRoots
    ) {
        createEnvironmentWithMockJdkAndIdeaAnnotations(configurationKind, Collections.emptyList(), TestJdkKind.MOCK_JDK, javaSourceRoots);
    }

    protected final void createEnvironmentWithMockJdkAndIdeaAnnotations(
            @NotNull ConfigurationKind configurationKind,
            @NotNull List<TestFile> testFilesWithConfigurationDirectives,
            @NotNull TestJdkKind testJdkKind,
            @NotNull File... javaSourceRoots
    ) {
        if (myEnvironment != null) {
            throw new IllegalStateException("must not set up myEnvironment twice");
        }

        CompilerConfiguration configuration = createConfiguration(
                configurationKind,
                testJdkKind,
                Collections.singletonList(KotlinArtifacts.getInstance().getJetbrainsAnnotations()),
                ArraysKt.filterNotNull(javaSourceRoots),
                testFilesWithConfigurationDirectives
        );

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
    }

    @Override
    protected void tearDown() throws Exception {
        myFiles = null;
        myEnvironment = null;
        classFileFactory = null;

        if (initializedClassLoader != null) {
            initializedClassLoader.dispose();
            initializedClassLoader = null;
        }

        super.tearDown();
    }

    protected void loadText(@NotNull String text) {
        myFiles = CodegenTestFiles.create(DEFAULT_TEST_FILE_NAME + ".kt", text, myEnvironment.getProject());
    }

    @NotNull
    protected String loadFile(@NotNull @TestDataFile String name) {
        return loadFileByFullPath(KotlinTestUtils.getTestDataPathBase() + "/codegen/" + name);
    }

    @NotNull
    protected String loadFileByFullPath(@NotNull String fullPath) {
        try {
            File file = new File(fullPath);
            String content = FileUtil.loadFile(file, Charsets.UTF_8.name(), true);
            assert myFiles == null : "Should not initialize myFiles twice";
            myFiles = CodegenTestFiles.create(file.getName(), content, myEnvironment.getProject());
            return content;
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void loadFile() {
        loadFile(getPrefix() + "/" + getTestName(true) + ".kt");
    }

    protected void loadMultiFiles(@NotNull List<TestFile> files) {
        myFiles = loadMultiFiles(files, myEnvironment.getProject());
    }

    @NotNull
    public static CodegenTestFiles loadMultiFiles(@NotNull List<TestFile> files, @NotNull Project project) {
        Collections.sort(files);

        List<KtFile> ktFiles = new ArrayList<>(files.size());
        for (TestFile file : files) {
            if (file.name.endsWith(".kt") || file.name.endsWith(".kts")) {
                // `rangesToDiagnosticNames` parameter is not-null only for diagnostic tests, it's using for lazy diagnostics
                String content = CheckerTestUtil.INSTANCE.parseDiagnosedRanges(file.content, new ArrayList<>(0), null);
                ktFiles.add(KotlinTestUtils.createFile(file.name, content, project));
            }
        }

        return CodegenTestFiles.create(ktFiles);
    }

    @NotNull
    protected String codegenTestBasePath() {
        return "compiler/testData/codegen/";
    }

    @NotNull
    protected String relativePath(@NotNull File file) {
        return FilesKt.toRelativeString(file.getAbsoluteFile(), new File(codegenTestBasePath()).getAbsoluteFile());
    }

    @NotNull
    protected String getPrefix() {
        throw new UnsupportedOperationException();
    }

    private static void setCustomDefaultJvmTarget(CompilerConfiguration configuration) {
        if (DEFAULT_JVM_TARGET != null) {
            JvmTarget customDefaultTarget = JvmTarget.fromString(DEFAULT_JVM_TARGET);
            assert customDefaultTarget != null : "Can't construct JvmTarget for " + DEFAULT_JVM_TARGET;
            JvmTarget originalTarget = configuration.get(JVMConfigurationKeys.JVM_TARGET);
            if (originalTarget == null || customDefaultTarget.getBytecodeVersion() > originalTarget.getBytecodeVersion()) {
                // It's not safe to substitute target in general
                // cause it can affect generated bytecode and original behaviour should be tested somehow.
                // Original behaviour testing is perfomed by
                //
                //      codegenTest(target = 6, jvm = "Last", jdk = mostRecentJdk)
                //      codegenTest(target = 8, jvm = "Last", jdk = mostRecentJdk)
                //
                // in compiler/tests-different-jdk/build.gradle.kts
                configuration.put(JVMConfigurationKeys.JVM_TARGET, customDefaultTarget);
            }
        }
    }

    protected TargetBackend getBackend() {
        return TargetBackend.JVM;
    }

    @Override
    protected void doTest(@NotNull String filePath) throws Exception {
        File file = new File(filePath);

        String expectedText = KotlinTestUtils.doLoadFile(file);
        if (!coroutinesPackage.isEmpty()) {
            expectedText = expectedText.replace("COROUTINES_PACKAGE", coroutinesPackage);
        }

        List<TestFile> testFiles = createTestFilesFromFile(file, expectedText);

        doMultiFileTest(file, testFiles);
    }

    @Override
    protected void doTestWithCoroutinesPackageReplacement(@NotNull String filePath, @NotNull String packageName) throws Exception {
        this.coroutinesPackage = packageName;
        doTest(filePath);
    }

    @Override
    @NotNull
    protected List<TestFile> createTestFilesFromFile(@NotNull File file, @NotNull String expectedText) {
        return createTestFilesFromFile(file, expectedText, coroutinesPackage, parseDirectivesPerFiles(), getBackend());
    }

    @NotNull
    public static List<TestFile> createTestFilesFromFile(
            @NotNull File file,
            @NotNull String expectedText,
            @NotNull String coroutinesPackage,
            boolean parseDirectivesPerFiles,
            @NotNull TargetBackend backend
    ) {
        List<TestFile> testFiles = TestFiles.createTestFiles(file.getName(), expectedText, new TestFiles.TestFileFactoryNoModules<TestFile>() {
            @NotNull
            @Override
            public TestFile create(@NotNull String fileName, @NotNull String text, @NotNull Directives directives) {
                return new TestFile(fileName, text, directives);
            }
        }, false, coroutinesPackage, parseDirectivesPerFiles);
        if (InTextDirectivesUtils.isDirectiveDefined(expectedText, "WITH_HELPERS")) {
            testFiles.add(new TestFile("CodegenTestHelpers.kt", TestHelperGeneratorKt.createTextForCodegenTestHelpers(backend)));
        }
        return testFiles;
    }


    @NotNull
    protected File getJavaSourcesOutputDirectory() {
        return createTempDirectory("java-files");
    }

    @NotNull
    private static File createTempDirectory(String prefix) {
        try {
            return KotlinTestUtils.tmpDir(prefix);
        } catch (IOException e) {
            throw ExceptionUtilsKt.rethrow(e);
        }
    }

    @Nullable
    protected File writeJavaFiles(@NotNull List<TestFile> files) {
        List<TestFile> javaFiles = CollectionsKt.filter(files, file -> file.name.endsWith(".java"));
        if (javaFiles.isEmpty()) return null;

        File dir = getJavaSourcesOutputDirectory();

        for (TestFile testFile : javaFiles) {
            File file = new File(dir, testFile.name);
            KotlinTestUtils.mkdirs(file.getParentFile());
            FilesKt.writeText(file, testFile.content, Charsets.UTF_8);
        }

        return dir;
    }
}
