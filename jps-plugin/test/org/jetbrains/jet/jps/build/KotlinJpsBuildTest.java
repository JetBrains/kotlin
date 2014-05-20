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

package org.jetbrains.jet.jps.build;

import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.jet.codegen.PackageCodegen;
import org.jetbrains.jet.lang.resolve.kotlin.PackagePartClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jps.builders.BuildResult;
import org.jetbrains.jps.model.java.JpsJavaDependencyScope;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Opcodes;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

public class KotlinJpsBuildTest extends AbstractKotlinJpsBuildTestCase {
    private static final String PROJECT_NAME = "kotlinProject";
    private static final String JDK_NAME = "IDEA_JDK";

    private static final String[] EXCLUDE_FILES = { "Excluded.class", "YetAnotherExcluded.class" };
    private static final String[] NOTHING = {};

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

    public void testKotlinProject() {
        doTest();

        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/test1.kt", "_DefaultPackage");
    }

    public void testExcludeFolderInSourceRoot() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "Foo.class");
        assertFilesNotExistInOutput(module, EXCLUDE_FILES);

        checkClassesDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/foo.kt", "Foo");
    }

    public void testExcludeModuleFolderInSourceRootOfAnotherModule() {
        doTest();

        for (JpsModule module : myProject.getModules()) {
            assertFilesExistInOutput(module, "Foo.class");
        }

        checkClassesDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/foo.kt", "Foo");
        checkClassesDeletedFromOutputWhen(Operation.CHANGE, "module2", "src/module2/src/foo.kt", "Foo");
    }

    public void testExcludeFileUsingCompilerSettings() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "Foo.class", "Bar.class");
        assertFilesNotExistInOutput(module, EXCLUDE_FILES);

        checkClassesDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/foo.kt", "Foo");
        checkExcludesNotAffectedToOutput("kotlinProject", "src/Excluded.kt", "src/dir/YetAnotherExcluded.kt");
    }

    public void testExcludeFolderNonRecursivelyUsingCompilerSettings() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "Foo.class", "Bar.class");
        assertFilesNotExistInOutput(module, EXCLUDE_FILES);

        checkClassesDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/foo.kt", "Foo");
        checkClassesDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/dir/subdir/bar.kt", "Bar");
        checkExcludesNotAffectedToOutput("kotlinProject", "src/dir/Excluded.kt", "src/dir/subdir/YetAnotherExcluded.kt");
    }

    public void testExcludeFolderRecursivelyUsingCompilerSettings() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "Foo.class", "Bar.class");
        assertFilesNotExistInOutput(module, EXCLUDE_FILES);

        checkClassesDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/foo.kt", "Foo");
        checkExcludesNotAffectedToOutput("kotlinProject",
                                         "src/exclude/Excluded.kt", "src/exclude/YetAnotherExcluded.kt",
                                         "src/exclude/subdir/Excluded.kt", "src/exclude/subdir/YetAnotherExcluded.kt");
    }

    public void testManyFiles() {
        doTest();

        JpsModule module = myProject.getModules().get(0);
        assertFilesExistInOutput(module, "foo/FooPackage.class", "boo/BooPackage.class", "foo/Bar.class");

        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/main.kt", "foo.FooPackage");
        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/boo.kt", "boo.BooPackage");
        checkClassesDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/Bar.kt", "foo.Bar");

        checkPackageDeletedFromOutputWhen(Operation.DELETE, "kotlinProject", "src/main.kt", "foo.FooPackage");
        assertFilesNotExistInOutput(module, "foo/FooPackage.class");

        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/boo.kt", "boo.BooPackage");
        checkClassesDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/Bar.kt", "foo.Bar");
    }

    public void testKotlinProjectTwoFilesInOnePackage() {
        doTest();

        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/test1.kt", "_DefaultPackage");
        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/test2.kt", "_DefaultPackage");
    }

    public void testKotlinJavaProject() {
        doTest();
    }

    public void testJKJProject() {
        doTest();
    }

    public void testKJKProject() {
        doTest();
    }

    public void testKJCircularProject() {
        doTest();
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

        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "kotlinProject", "src/kt2.kt", "kt2.Kt2Package");
        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "module2", "module2/src/kt1.kt", "kt1.Kt1Package");
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

        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "module1", "module1/src/a.kt", "test.TestPackage");
        checkPackageDeletedFromOutputWhen(Operation.CHANGE, "module2", "module2/src/b.kt", "test.TestPackage");
    }

    @NotNull
    private static Set<String> getMethodsOfClass(@NotNull File classFile) throws IOException {
        final Set<String> result = new TreeSet<String>();
        new ClassReader(FileUtil.loadFileBytes(classFile)).accept(new ClassVisitor(Opcodes.ASM5) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
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

    private void checkExcludesNotAffectedToOutput(String module, String... excludeRelativePaths) {
        for (String path : excludeRelativePaths) {
            checkClassesDeletedFromOutputWhen(Operation.CHANGE, module, path, NOTHING);
        }
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

    private void checkPackageDeletedFromOutputWhen(
            Operation operation,
            String moduleName,
            String sourceFileName,
            String packageClassFqNamesToDelete
    ) {
        File file = new File(workDir, sourceFileName);
        String[] packageClasses = { packageClassFqNamesToDelete, getInternalNameForPackagePartClass(file, packageClassFqNamesToDelete) };

        checkClassesDeletedFromOutputWhen(operation, moduleName, sourceFileName, packageClasses);
    }

    private void checkClassesDeletedFromOutputWhen(
            Operation operation,
            final String moduleName,
            String sourceFileName,
            String... classFqNamesToDelete
    ) {
        String[] paths = ContainerUtil.map2Array(classFqNamesToDelete, String.class, new Function<String, String>() {
            @Override
            public String fun(String classFqName) {
                return outputPathInModuleByClassFqName(moduleName, classFqName);
            }
        });

        checkFilesDeletedFromOutputWhen(operation, sourceFileName, paths);
    }

    private void checkFilesDeletedFromOutputWhen(Operation operation, String sourceFileName, String... pathsToDelete) {
        File file = new File(workDir, sourceFileName);

        if (operation == Operation.CHANGE) {
            change(file.getAbsolutePath());
        }
        else if(operation == Operation.DELETE) {
            assertTrue("Can not delete file \"" + file.getAbsolutePath() + "\"",
                       file.delete());
        }
        else {
            fail("Unknown operation");
        }

        makeAll().assertSuccessful();

        assertDeleted(pathsToDelete);
    }

    private static String outputPathInModuleByClassFqName(String moduleName, String classFqName) {
        String outputDirPrefix = "out/production/" + moduleName + "/";
        return outputDirPrefix + classFqName.replace('.', '/') + ".class";
    }

    private static String getInternalNameForPackagePartClass(File sourceFile, String packageClassFqName) {
        LightVirtualFile fakeVirtualFile = new LightVirtualFile(sourceFile.getPath()) {
            @Override
            public String getPath() {
                // strip extra "/" from the beginning
                return super.getPath().substring(1);
            }
        };

        FqName packagePartFqName = PackagePartClassUtils.getPackagePartFqName(new FqName(packageClassFqName), fakeVirtualFile);
        return AsmUtil.internalNameByFqNameWithoutInnerClasses(packagePartFqName);
    }

    private static enum Operation {
        CHANGE, DELETE
    }
}
