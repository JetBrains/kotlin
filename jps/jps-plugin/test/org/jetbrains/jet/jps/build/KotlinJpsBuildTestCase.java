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
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.jet.codegen.NamespaceCodegen;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jps.builders.BuildResult;
import org.jetbrains.jps.model.java.JpsJavaDependencyScope;
import org.jetbrains.jps.model.java.JpsJavaExtensionService;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.IOException;

public class KotlinJpsBuildTestCase extends AbstractKotlinJpsBuildTestCase {
    private static final String PROJECT_NAME = "kotlinProject";
    private static final String JDK_NAME = "IDEA_JDK";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        File sourceFilesRoot = new File(TEST_DATA_PATH + getTestName(false));
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

        assertOutputDeleted("src/test1.kt", "_DefaultPackage", "kotlinProject");
    }

    public void testKotlinProjectTwoFilesInOnePackage() {
        doTest();

        assertOutputDeleted("src/test1.kt", "_DefaultPackage", "kotlinProject");
        assertOutputDeleted("src/test2.kt", "_DefaultPackage", "kotlinProject");
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

    public void testCircularDependenciesWithKotlinFilesDifferentPackages() {
        initProject();
        BuildResult result = makeAll();

        // Check that outputs are located properly
        for (JpsModule module : myProject.getModules()) {
            if (module.getName().equals("module2")) {
                assertFileExistsInOutput(module, "kt1/Kt1Package.class");
            }
            if (module.getName().equals("kotlinProject")) {
                assertFileExistsInOutput(module, "kt2/Kt2Package.class");
            }
        }
        result.assertSuccessful();

        assertOutputDeleted("src/kt2.kt", "kt2.Kt2Package", "kotlinProject");
        assertOutputDeleted("module2/src/kt1.kt", "kt1.Kt1Package", "module2");
    }

    public void testReexportedDependency() {
        initProject();
        addKotlinRuntimeDependency(JpsJavaDependencyScope.COMPILE,
                                   ContainerUtil.filter(myProject.getModules(), new Condition<JpsModule>() {
                                       @Override
                                       public boolean value(JpsModule module) {
                                           return module.getName().equals("module2");
                                       }
                                   }), true);
        makeAll().assertSuccessful();
    }

    private static void assertFileExistsInOutput(JpsModule module, String relativePath) {
        String outputUrl = JpsJavaExtensionService.getInstance().getOutputUrl(module, false);
        assertNotNull(outputUrl);
        File outputDir = new File(JpsPathUtil.urlToPath(outputUrl));
        File outputFile = new File(outputDir, relativePath);
        assertTrue("Output not written: " +
                   outputFile.getAbsolutePath() +
                   "\n Directory contents: \n" +
                   dirContents(outputFile.getParentFile()),
                   outputFile.exists());
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

    private void assertOutputDeleted(String sourceFileName, String packageClassFqName, String moduleName) {
        File file = new File(workDir, sourceFileName);
        change(file.getAbsolutePath());
        makeAll();

        String outputDirPrefix = "out/production/" + moduleName + "/";
        assertDeleted(outputDirPrefix + packageClassFqName.replace('.', '/') + ".class",
                      outputDirPrefix + getInternalNameForPackagePartClass(file, packageClassFqName) + ".class");
    }

    private static String getInternalNameForPackagePartClass(File sourceFile, String packageClassFqName) {
        LightVirtualFile fakeVirtualFile = new LightVirtualFile(sourceFile.getPath()) {
            @Override
            public String getPath() {
                // strip extra "/" from the beginning
                return super.getPath().substring(1);
            }
        };
        return NamespaceCodegen.getNamespacePartType(new FqName(packageClassFqName), fakeVirtualFile).getInternalName();
    }
}
