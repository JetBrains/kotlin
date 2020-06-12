/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
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
import org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettings;
import org.jetbrains.kotlin.checkers.utils.CheckerTestUtil;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.config.*;
import org.jetbrains.kotlin.idea.artifacts.TestKotlinArtifacts;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.test.*;
import org.jetbrains.kotlin.utils.ExceptionUtilsKt;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.kotlin.checkers.CompilerTestLanguageVersionSettingsKt.parseLanguageVersionSettings;

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
                Collections.singletonList(TestKotlinArtifacts.INSTANCE.getJetbrainsAnnotations()),
                ArraysKt.filterNotNull(javaSourceRoots),
                testFilesWithConfigurationDirectives
        );

        myEnvironment = KotlinCoreEnvironment.createForTests(
                getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES
        );
    }

    @NotNull
    protected CompilerConfiguration createConfiguration(
            @NotNull ConfigurationKind kind,
            @NotNull TestJdkKind jdkKind,
            @NotNull List<File> classpath,
            @NotNull List<File> javaSource,
            @NotNull List<TestFile> testFilesWithConfigurationDirectives
    ) {
        CompilerConfiguration configuration = KotlinTestUtils.newConfiguration(kind, jdkKind, classpath, javaSource);
        configuration.put(JVMConfigurationKeys.IR, getBackend().isIR());

        updateConfigurationByDirectivesInTestFiles(testFilesWithConfigurationDirectives, configuration, coroutinesPackage, parseDirectivesPerFiles());
        setCustomDefaultJvmTarget(configuration);

        return configuration;
    }

    private static void updateConfigurationByDirectivesInTestFiles(
            @NotNull List<TestFile> testFilesWithConfigurationDirectives,
            @NotNull CompilerConfiguration configuration,
            @NotNull String coroutinesPackage,
            boolean usePreparsedDirectives
    ) {
        LanguageVersionSettings explicitLanguageVersionSettings = null;
        boolean disableReleaseCoroutines = false;
        boolean includeCompatExperimentalCoroutines = false;

        List<String> kotlinConfigurationFlags = new ArrayList<>(0);
        for (TestFile testFile : testFilesWithConfigurationDirectives) {
            String content = testFile.content;
            Directives directives = usePreparsedDirectives ? testFile.directives : KotlinTestUtils.parseDirectives(content);
            List<String> flags = directives.listValues("KOTLIN_CONFIGURATION_FLAGS");
            if (flags != null) {
                kotlinConfigurationFlags.addAll(flags);
            }

            String targetString = directives.get("JVM_TARGET");
            if (targetString != null) {
                JvmTarget jvmTarget = JvmTarget.Companion.fromString(targetString);
                assert jvmTarget != null : "Unknown target: " + targetString;
                configuration.put(JVMConfigurationKeys.JVM_TARGET, jvmTarget);
            }

            String version = directives.get("LANGUAGE_VERSION");
            if (version != null) {
                throw new AssertionError(
                        "Do not use LANGUAGE_VERSION directive in compiler tests because it's prone to limiting the test\n" +
                        "to a specific language version, which will become obsolete at some point and the test won't check\n" +
                        "things like feature intersection with newer releases. Use `// !LANGUAGE: [+-]FeatureName` directive instead,\n" +
                        "where FeatureName is an entry of the enum `LanguageFeature`\n"
                );
            }

            if (directives.contains("COMMON_COROUTINES_TEST")) {
                assert !directives.contains("COROUTINES_PACKAGE") : "Must replace COROUTINES_PACKAGE prior to tests compilation";
                if (DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_EXPERIMENTAL.asString().equals(coroutinesPackage)) {
                    disableReleaseCoroutines = true;
                    includeCompatExperimentalCoroutines = true;
                }
            }

            if (content.contains(DescriptorUtils.COROUTINES_PACKAGE_FQ_NAME_EXPERIMENTAL.asString())) {
                includeCompatExperimentalCoroutines = true;
            }

            LanguageVersionSettings fileLanguageVersionSettings = parseLanguageVersionSettings(directives);
            if (fileLanguageVersionSettings != null) {
                assert explicitLanguageVersionSettings == null : "Should not specify !LANGUAGE directive twice";
                explicitLanguageVersionSettings = fileLanguageVersionSettings;
            }
        }

        if (disableReleaseCoroutines) {
            explicitLanguageVersionSettings = new CompilerTestLanguageVersionSettings(
                    Collections.singletonMap(LanguageFeature.ReleaseCoroutines, LanguageFeature.State.DISABLED),
                    ApiVersion.LATEST_STABLE,
                    LanguageVersion.LATEST_STABLE,
                    Collections.emptyMap()
            );
        }
        if (includeCompatExperimentalCoroutines) {
            JvmContentRootsKt.addJvmClasspathRoot(configuration, ForTestCompileRuntime.coroutinesCompatForTests());
        }

        if (explicitLanguageVersionSettings != null) {
            CommonConfigurationKeysKt.setLanguageVersionSettings(configuration, explicitLanguageVersionSettings);
        }

        updateConfigurationWithFlags(configuration, kotlinConfigurationFlags);
    }

    private static final Map<String, Class<?>> FLAG_NAMESPACE_TO_CLASS = ImmutableMap.of(
            "CLI", CLIConfigurationKeys.class,
            "JVM", JVMConfigurationKeys.class
    );

    private static final List<Class<?>> FLAG_CLASSES = ImmutableList.of(CLIConfigurationKeys.class, JVMConfigurationKeys.class);

    private static final Pattern BOOLEAN_FLAG_PATTERN = Pattern.compile("([+-])(([a-zA-Z_0-9]*)\\.)?([a-zA-Z_0-9]*)");
    private static final Pattern CONSTRUCTOR_CALL_NORMALIZATION_MODE_FLAG_PATTERN = Pattern.compile(
            "CONSTRUCTOR_CALL_NORMALIZATION_MODE=([a-zA-Z_\\-0-9]*)");
    private static final Pattern ASSERTIONS_MODE_FLAG_PATTERN = Pattern.compile("ASSERTIONS_MODE=([a-zA-Z_0-9-]*)");

    private static void updateConfigurationWithFlags(@NotNull CompilerConfiguration configuration, @NotNull List<String> flags) {
        for (String flag : flags) {
            Matcher m = BOOLEAN_FLAG_PATTERN.matcher(flag);
            if (m.matches()) {
                boolean flagEnabled = !"-".equals(m.group(1));
                String flagNamespace = m.group(3);
                String flagName = m.group(4);

                tryApplyBooleanFlag(configuration, flag, flagEnabled, flagNamespace, flagName);
                continue;
            }

            m = CONSTRUCTOR_CALL_NORMALIZATION_MODE_FLAG_PATTERN.matcher(flag);
            if (m.matches()) {
                String flagValueString = m.group(1);
                JVMConstructorCallNormalizationMode mode = JVMConstructorCallNormalizationMode.fromStringOrNull(flagValueString);
                assert mode != null : "Wrong CONSTRUCTOR_CALL_NORMALIZATION_MODE value: " + flagValueString;
                configuration.put(JVMConfigurationKeys.CONSTRUCTOR_CALL_NORMALIZATION_MODE, mode);
            }

            m = ASSERTIONS_MODE_FLAG_PATTERN.matcher(flag);
            if (m.matches()) {
                String flagValueString = m.group(1);
                JVMAssertionsMode mode = JVMAssertionsMode.fromStringOrNull(flagValueString);
                assert mode != null : "Wrong ASSERTIONS_MODE value: " + flagValueString;
                configuration.put(JVMConfigurationKeys.ASSERTIONS_MODE, mode);
            }
        }
    }

    private static void tryApplyBooleanFlag(
            @NotNull CompilerConfiguration configuration,
            @NotNull String flag,
            boolean flagEnabled,
            @Nullable String flagNamespace,
            @NotNull String flagName
    ) {
        Class<?> configurationKeysClass;
        Field configurationKeyField = null;
        if (flagNamespace == null) {
            for (Class<?> flagClass : FLAG_CLASSES) {
                try {
                    configurationKeyField = flagClass.getField(flagName);
                    break;
                }
                catch (Exception ignored) {
                }
            }
        }
        else {
            configurationKeysClass = FLAG_NAMESPACE_TO_CLASS.get(flagNamespace);
            assert configurationKeysClass != null : "Expected [+|-][namespace.]configurationKey, got: " + flag;
            try {
                configurationKeyField = configurationKeysClass.getField(flagName);
            }
            catch (Exception e) {
                configurationKeyField = null;
            }
        }
        assert configurationKeyField != null : "Expected [+|-][namespace.]configurationKey, got: " + flag;

        try {
            @SuppressWarnings("unchecked")
            CompilerConfigurationKey<Boolean> configurationKey = (CompilerConfigurationKey<Boolean>) configurationKeyField.get(null);
            configuration.put(configurationKey, flagEnabled);
        }
        catch (Exception e) {
            assert false : "Expected [+|-][namespace.]configurationKey, got: " + flag;
        }
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

    protected void setCustomDefaultJvmTarget(CompilerConfiguration configuration) {
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

    protected boolean parseDirectivesPerFiles() {
        return false;
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
