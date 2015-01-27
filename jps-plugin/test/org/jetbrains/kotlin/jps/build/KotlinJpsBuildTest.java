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

package org.jetbrains.kotlin.jps.build;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.io.ZipUtil;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jps.builders.BuildResult;
import org.jetbrains.jps.builders.impl.BuildDataPathsImpl;
import org.jetbrains.jps.model.java.JpsJavaDependencyScope;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.utils.PathUtil;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipOutputStream;

public class KotlinJpsBuildTest extends AbstractKotlinJpsBuildTestCase {
    private static final String PROJECT_NAME = "kotlinProject";
    private static final String JDK_NAME = "IDEA_JDK";

    private static final String[] EXCLUDE_FILES = { "Excluded.class", "YetAnotherExcluded.class" };
    private static final String[] NOTHING = {};
    private static final String KOTLIN_JS_LIBRARY = "jslib-example";
    private static final String PATH_TO_KOTLIN_JS_LIBRARY = TEST_DATA_PATH + "general/KotlinJavaScriptProjectWithDirectoryAsLibrary/" + KOTLIN_JS_LIBRARY;
    private static final String KOTLIN_JS_LIBRARY_JAR = KOTLIN_JS_LIBRARY + ".jar";
    private static final Set<String> EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY = KotlinPackage.hashSetOf(PROJECT_NAME + ".js", "lib/kotlin.js", "lib/stdlib.meta.js");
    private static final Set<String> EXPECTED_JS_FILES_IN_OUTPUT_NO_COPY = KotlinPackage.hashSetOf(PROJECT_NAME + ".js");
    private static final Set<String> EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_DEFAULT_DIR =
        KotlinPackage.hashSetOf(
            PROJECT_NAME + ".js",
            "lib/kotlin.js",
            "lib/stdlib.meta.js",
            "lib/jslib-example.js",
            "lib/file0.js",
            "lib/dir/file1.js",
            "lib/META-INF-ex/file2.js",
            "lib/res0.js",
            "lib/resdir/res1.js");
    private static final Set<String> EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_CUSTOM_DIR =
        KotlinPackage.hashSetOf(
            PROJECT_NAME + ".js",
            "custom/kotlin.js",
            "custom/stdlib.meta.js",
            "custom/jslib-example.js",
            "custom/file0.js",
            "custom/dir/file1.js",
            "custom/META-INF-ex/file2.js",
            "custom/res0.js",
            "custom/resdir/res1.js");
    @Override
    public void setUp() throws Exception {
        super.setUp();
        File sourceFilesRoot = new File(TEST_DATA_PATH + "general/" + getTestName(false));
        workDir = copyTestDataToTmpDir(sourceFilesRoot);
        getOrCreateProjectDir();
    }

    @Override
    public void tearDown() throws Exception {
        FileUtil.delete(workDir);
        super.tearDown();
    }

    @Override
    protected File doGetProjectDir() throws IOException {
        return workDir;
    }

    private void initProject() {
        addJdk(JDK_NAME);
        loadProject(workDir.getAbsolutePath() + File.separator + PROJECT_NAME + ".ipr");
    }

    public void doTest() {
        initProject();
        makeAll().assertSuccessful();
    }

    public void doTestWithRuntime() {
        initProject();
        addKotlinRuntimeDependency();
        makeAll().assertSuccessful();
    }

    public void doTestWithKotlinJavaScriptLibrary() {
        initProject();
        addKotlinJavaScriptStdlibDependency();
        createKotlinJavaScriptLibraryArchive();
        addKotlinJavaScriptDependency(KOTLIN_JS_LIBRARY, new File(workDir, KOTLIN_JS_LIBRARY_JAR));
        makeAll().assertSuccessful();
    }

    public void testKotlinProject() {
        doTest();

        checkWhen(touch("src/test1.kt"), null, packageClasses("kotlinProject", "src/test1.kt", "_DefaultPackage"));
    }

    public void testKotlinJavaScriptProject() {
        initProject();
        addKotlinJavaScriptStdlibDependency();
        makeAll().assertSuccessful();

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY, contentOfOutputDir(PROJECT_NAME));
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME));
    }

    public void testKotlinJavaScriptProjectWithDirectoryAsStdlib() {
        initProject();
        File jslibJar = PathUtil.getKotlinPathsForDistDirectory().getJsStdLibJarPath();
        File jslibDir = new File(workDir, "KotlinJavaScript");
        try {
            ZipUtil.extract(jslibJar, jslibDir, null);
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
        addKotlinJavaScriptDependency("KotlinJavaScript", jslibDir);
        makeAll().assertSuccessful();

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_FOR_STDLIB_ONLY, contentOfOutputDir(PROJECT_NAME));
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME));
    }

    public void testKotlinJavaScriptProjectWithDirectoryAsLibrary() {
        initProject();
        addKotlinJavaScriptStdlibDependency();
        addKotlinJavaScriptDependency(KOTLIN_JS_LIBRARY, new File(workDir, KOTLIN_JS_LIBRARY));
        makeAll().assertSuccessful();

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_DEFAULT_DIR, contentOfOutputDir(PROJECT_NAME));
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME));
    }

    public void testKotlinJavaScriptProjectWithLibrary() {
        doTestWithKotlinJavaScriptLibrary();

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_DEFAULT_DIR, contentOfOutputDir(PROJECT_NAME));
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME));
    }

    public void testKotlinJavaScriptProjectWithLibraryCustomOutputDir() {
        doTestWithKotlinJavaScriptLibrary();

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_WITH_ADDITIONAL_LIB_AND_CUSTOM_DIR, contentOfOutputDir(PROJECT_NAME));
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME));
    }

    public void testKotlinJavaScriptProjectWithLibraryNoCopy() {
        doTestWithKotlinJavaScriptLibrary();

        assertEquals(EXPECTED_JS_FILES_IN_OUTPUT_NO_COPY, contentOfOutputDir(PROJECT_NAME));
        checkWhen(touch("src/test1.kt"), null, k2jsOutput(PROJECT_NAME));
    }

    public void testKotlinJavaScriptProjectWithLibraryAndErrors() {
        initProject();
        addKotlinJavaScriptStdlibDependency();
        createKotlinJavaScriptLibraryArchive();
        addKotlinJavaScriptDependency(KOTLIN_JS_LIBRARY, new File(workDir, KOTLIN_JS_LIBRARY_JAR));
        makeAll().assertFailed();

        assertEquals(Collections.EMPTY_SET, contentOfOutputDir(PROJECT_NAME));
    }

    public void testExcludeFolderInSourceRoot() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "Foo.class");
        assertFilesNotExistInOutput(module, EXCLUDE_FILES);

        checkWhen(touch("src/foo.kt"), null, new String[] {klass("kotlinProject", "Foo")});
    }

    public void testExcludeModuleFolderInSourceRootOfAnotherModule() {
        doTest();

        for (JpsModule module : myProject.getModules()) {
            assertFilesExistInOutput(module, "Foo.class");
        }

        checkWhen(touch("src/foo.kt"), null, new String[] {klass("kotlinProject", "Foo")});
        checkWhen(touch("src/module2/src/foo.kt"), null, new String[] {klass("module2", "Foo")});
    }

    public void testExcludeFileUsingCompilerSettings() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "Foo.class", "Bar.class");
        assertFilesNotExistInOutput(module, EXCLUDE_FILES);

        checkWhen(touch("src/foo.kt"), null, new String[] { klass("kotlinProject", "Foo")} );
        checkWhen(touch("src/Excluded.kt"), null, NOTHING );
        checkWhen(touch("src/dir/YetAnotherExcluded.kt"), null, NOTHING);
    }

    public void testExcludeFolderNonRecursivelyUsingCompilerSettings() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "Foo.class", "Bar.class");
        assertFilesNotExistInOutput(module, EXCLUDE_FILES);

        checkWhen(touch("src/foo.kt"), null, new String[] { klass("kotlinProject", "Foo")} );
        checkWhen(touch("src/dir/subdir/bar.kt"), null, new String[] { klass("kotlinProject", "Bar")} );

        checkWhen(touch("src/dir/Excluded.kt"), null, NOTHING );
        checkWhen(touch("src/dir/subdir/YetAnotherExcluded.kt"), null, NOTHING);
    }

    public void testExcludeFolderRecursivelyUsingCompilerSettings() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "Foo.class", "Bar.class");
        assertFilesNotExistInOutput(module, EXCLUDE_FILES);

        checkWhen(touch("src/foo.kt"), null, new String[] { klass("kotlinProject", "Foo")} );

        checkWhen(touch("src/exclude/Excluded.kt"), null, NOTHING);
        checkWhen(touch("src/exclude/YetAnotherExcluded.kt"), null, NOTHING);
        checkWhen(touch("src/exclude/subdir/Excluded.kt"), null, NOTHING);
        checkWhen(touch("src/exclude/subdir/YetAnotherExcluded.kt"), null, NOTHING);
    }

    public void testManyFiles() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "foo/FooPackage.class", "boo/BooPackage.class", "foo/Bar.class");

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.FooPackage"));
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooPackage"));
        checkWhen(touch("src/Bar.kt"),
                  new String[] {"src/Bar.kt", "src/boo.kt", "src/main.kt"},
                  new String[] {klass("kotlinProject", "foo.Bar")});

        checkWhen(del("src/main.kt"),
                  new String[] {"src/Bar.kt", "src/boo.kt"},
                  packageClasses("kotlinProject", "src/main.kt", "foo.FooPackage"));
        assertFilesExistInOutput(module, "boo/BooPackage.class", "foo/Bar.class");
        assertFilesNotExistInOutput(module, "foo/FooPackage.class");

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooPackage"));
        checkWhen(touch("src/Bar.kt"), null, new String[] {klass("kotlinProject", "foo.Bar")});
    }

    public void testManyFilesForPackage() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "foo/FooPackage.class", "boo/BooPackage.class", "foo/Bar.class");

        checkWhen(touch("src/main.kt"), null, packageClasses("kotlinProject", "src/main.kt", "foo.FooPackage"));
        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooPackage"));
        checkWhen(touch("src/Bar.kt"),
                  new String[] {"src/Bar.kt", "src/boo.kt", "src/main.kt"},
                  new String[] {
                          klass("kotlinProject", "foo.Bar"),
                          klass("kotlinProject", "foo.FooPackage"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.FooPackage")});

        checkWhen(del("src/main.kt"),
                  new String[] {"src/Bar.kt", "src/boo.kt"},
                  packageClasses("kotlinProject", "src/main.kt", "foo.FooPackage"));
        assertFilesExistInOutput(module, "foo/FooPackage.class", "boo/BooPackage.class", "foo/Bar.class");

        checkWhen(touch("src/boo.kt"), null, packageClasses("kotlinProject", "src/boo.kt", "boo.BooPackage"));
        checkWhen(touch("src/Bar.kt"), null,
                  new String[] {
                          klass("kotlinProject", "foo.Bar"),
                          klass("kotlinProject", "foo.FooPackage"),
                          packagePartClass("kotlinProject", "src/Bar.kt", "foo.FooPackage")
                  });
    }

    public void testKotlinProjectTwoFilesInOnePackage() {
        doTest();

        checkWhen(touch("src/test1.kt"), null, packageClasses("kotlinProject", "src/test1.kt", "_DefaultPackage"));
        checkWhen(touch("src/test2.kt"), null, packageClasses("kotlinProject", "src/test2.kt", "_DefaultPackage"));

        checkWhen(new Action[]{ del("src/test1.kt"), del("src/test2.kt") }, NOTHING,
                  new String[] {
                          packagePartClass("kotlinProject", "src/test1.kt", "_DefaultPackage"),
                          packagePartClass("kotlinProject", "src/test2.kt", "_DefaultPackage"),
                          klass("kotlinProject", "_DefaultPackage")
                  });

        assertFilesNotExistInOutput(myProject.getModules().get(0), "_DefaultPackage.class");
    }

    public void testKotlinJavaProject() {
        doTestWithRuntime();
    }

    public void testJKJProject() {
        doTestWithRuntime();
    }

    public void testKJKProject() {
        doTestWithRuntime();
    }

    public void testKJCircularProject() {
        doTestWithRuntime();
    }

    public void testJKJInheritanceProject() {
        doTestWithRuntime();
    }

    public void testKJKInheritanceProject() {
        doTestWithRuntime();
    }

    public void testCircularDependenciesNoKotlinFiles() {
        doTest();
    }

    public void testCircularDependenciesDifferentPackages() {
        initProject();
        BuildResult result = makeAll();

        // Check that outputs are located properly
        assertFilesExistInOutput(findModule("module2"), "kt1/Kt1Package.class");
        assertFilesExistInOutput(findModule("kotlinProject"), "kt2/Kt2Package.class");

        result.assertSuccessful();

        checkWhen(touch("src/kt2.kt"), null, packageClasses("kotlinProject", "src/kt2.kt", "kt2.Kt2Package"));
        checkWhen(touch("module2/src/kt1.kt"), null, packageClasses("module2", "module2/src/kt1.kt", "kt1.Kt1Package"));
    }

    public void testCircularDependenciesSamePackage() throws IOException {
        initProject();
        BuildResult result = makeAll();
        result.assertSuccessful();

        // Check that outputs are located properly
        File facadeWithA = findFileInOutputDir(findModule("module1"), "test/TestPackage.class");
        File facadeWithB = findFileInOutputDir(findModule("module2"), "test/TestPackage.class");
        assertSameElements(getMethodsOfClass(facadeWithA), "<clinit>", "a", "getA");
        assertSameElements(getMethodsOfClass(facadeWithB), "<clinit>", "b", "getB", "setB");

        checkWhen(touch("module1/src/a.kt"), null, packageClasses("module1", "module1/src/a.kt", "test.TestPackage"));
        checkWhen(touch("module2/src/b.kt"), null, packageClasses("module2", "module2/src/b.kt", "test.TestPackage"));
    }

    private void createKotlinJavaScriptLibraryArchive() {
        File jarFile = new File(workDir, KOTLIN_JS_LIBRARY_JAR);
        try {
            ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(jarFile));
            ZipUtil.addDirToZipRecursively(zip, jarFile, new File(PATH_TO_KOTLIN_JS_LIBRARY), "", null, null);
            zip.close();
        }
        catch (FileNotFoundException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
        catch (IOException ex) {
            throw new IllegalStateException(ex.getMessage());
        }
    }

    @NotNull
    private static String[] k2jsOutput(String moduleName) {
        String outputDir = "out/production/" + moduleName;
        String[] result = new String[1];
        result[0] = outputDir + "/" + moduleName + ".js";
        return result;
    }

    @NotNull
    private Set<String> contentOfOutputDir(String moduleName) {
        String outputDir = "out/production/" + moduleName;
        File baseDir = new File(workDir, outputDir);
        List<File> files = FileUtil.findFilesByMask(Pattern.compile(".*"), baseDir);
        Set<String> result = new HashSet<String>();
        for(File file : files) {
            String relativePath = FileUtil.getRelativePath(baseDir, file);
            assert relativePath != null : "relativePath should not be null";
            result.add(FileUtil.toSystemIndependentName(relativePath));
        }
        return result;
    }

    @NotNull
    private static Set<String> getMethodsOfClass(@NotNull File classFile) throws IOException {
        final Set<String> result = new TreeSet<String>();
        new ClassReader(FileUtil.loadFileBytes(classFile)).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, @NotNull String name, @NotNull String desc, String signature, String[] exceptions) {
                result.add(name);
                return null;
            }
        }, 0);
        return result;
    }

    public void testReexportedDependency() {
        initProject();
        addKotlinRuntimeDependency(JpsJavaDependencyScope.COMPILE,
                                   ContainerUtil.filter(myProject.getModules(), new Condition<JpsModule>() {
                                       @Override
                                       public boolean value(JpsModule module) {
                                           return module.getName().equals("module2");
                                       }
                                   }), true
        );
        makeAll().assertSuccessful();
    }

    public void testDoNotCreateUselessKotlinIncrementalCaches() throws InterruptedException {
        initProject();
        makeAll().assertSuccessful();

        File storageRoot = new BuildDataPathsImpl(myDataStorageRoot).getDataStorageRoot();
        assertTrue(new File(storageRoot, "targets/java-test/kotlinProject/kotlin").exists());
        assertFalse(new File(storageRoot, "targets/java-production/kotlinProject/kotlin").exists());
    }

    @NotNull
    private JpsModule findModule(@NotNull String name) {
        for (JpsModule module : myProject.getModules()) {
            if (module.getName().equals(name)) {
                return module;
            }
        }
        throw new IllegalStateException("Couldn't find module " + name);
    }

    private static void assertFilesExistInOutput(JpsModule module, String... relativePaths) {
        for (String path : relativePaths) {
            File outputFile = findFileInOutputDir(module, path);
            assertTrue("Output not written: " +
                       outputFile.getAbsolutePath() +
                       "\n Directory contents: \n" +
                       dirContents(outputFile.getParentFile()),
                       outputFile.exists());
        }
    }

    private static File findFileInOutputDir(JpsModule module, String relativePath) {
        String outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false);
        assertNotNull(outputUrl);
        File outputDir = new File(JpsPathUtil.urlToPath(outputUrl));
        return new File(outputDir, relativePath);
    }


    private static void assertFilesNotExistInOutput(JpsModule module, String... relativePaths) {
        String outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false);
        assertNotNull(outputUrl);
        File outputDir = new File(JpsPathUtil.urlToPath(outputUrl));
        for (String path : relativePaths) {
            File outputFile = new File(outputDir, path);
            assertFalse("Output directory \"" + outputFile.getAbsolutePath() + "\" contains \"" + path + "\"",
                        outputFile.exists());
        }
    }

    private static String dirContents(File dir) {
        File[] files = dir.listFiles();
        if (files == null) {
            return "<not found>";
        }
        StringBuilder builder = new StringBuilder();
        for (File file : files) {
            builder.append(" * ").append(file.getName()).append("\n");
        }
        return builder.toString();
    }

    private void checkWhen(Action action, @Nullable String[] pathsToCompile, @Nullable String[] pathsToDelete) {
        checkWhen(new Action[]{action}, pathsToCompile, pathsToDelete);
    }

    private void checkWhen(Action[] actions, @Nullable String[] pathsToCompile, @Nullable String[] pathsToDelete) {
        for (Action action : actions) {
            action.apply();
        }

        makeAll().assertSuccessful();

        if (pathsToCompile != null) {
            assertCompiled(KotlinBuilder.KOTLIN_BUILDER_NAME, pathsToCompile);
        }

        if (pathsToDelete != null) {
            assertDeleted(pathsToDelete);
        }
    }

    private static String klass(String moduleName, String classFqName) {
        String outputDirPrefix = "out/production/" + moduleName + "/";
        return outputDirPrefix + classFqName.replace('.', '/') + ".class";
    }

    private String[] packageClasses(String moduleName, String fileName, String packageClassFqName) {
        return new String[] {klass(moduleName, packageClassFqName), packagePartClass(moduleName, fileName, packageClassFqName)};
    }

    private String packagePartClass(String moduleName, String fileName, String packageClassFqName) {
        File file = new File(workDir, fileName);
        LightVirtualFile fakeVirtualFile = new LightVirtualFile(file.getPath()) {
            @NotNull
            @Override
            public String getPath() {
                // strip extra "/" from the beginning
                return super.getPath().substring(1);
            }
        };

        FqName packagePartFqName = PackagePartClassUtils.getPackagePartFqName(new FqName(packageClassFqName), fakeVirtualFile);
        return klass(moduleName, AsmUtil.internalNameByFqNameWithoutInnerClasses(packagePartFqName));
    }

    private static enum Operation {
        CHANGE, DELETE
    }

    protected Action touch(String path) {
        return new Action(Operation.CHANGE, path);
    }

    protected Action del(String path) {
        return new Action(Operation.DELETE, path);
    }

    protected class Action {
        private final Operation operation;
        private final String path;

        protected Action(Operation operation, String path) {
            this.operation = operation;
            this.path = path;
        }

        protected void apply() {
            File file = new File(workDir, path);

            if (operation == Operation.CHANGE) {
                change(file.getAbsolutePath());
            }
            else if(operation == Operation.DELETE) {
                assertTrue("Can not delete file \"" + file.getAbsolutePath() + "\"", file.delete());
            }
            else {
                fail("Unknown operation");
            }
        }
    }
}
