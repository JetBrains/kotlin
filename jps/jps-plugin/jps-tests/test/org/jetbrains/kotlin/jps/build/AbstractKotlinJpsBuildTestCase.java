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

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.builders.JpsBuildTestCase;
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.JpsModuleRootModificationUtil;
import org.jetbrains.jps.model.JpsProject;
import org.jetbrains.jps.model.java.JpsJavaDependencyScope;
import org.jetbrains.jps.model.java.JpsJavaLibraryType;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public abstract class AbstractKotlinJpsBuildTestCase extends BaseKotlinJpsBuildTestCase {
    public static final String TEST_DATA_PATH = "jps-plugin/testData/";

    protected File workDir;

    protected static File copyTestDataToTmpDir(File testDataDir) throws IOException {
        assert testDataDir.exists() : "Cannot find source folder " + testDataDir.getAbsolutePath();
        File tmpDir = FileUtil.createTempDirectory("jps-build", null);
        FileUtil.copyDir(testDataDir, tmpDir);
        return tmpDir;
    }

    @Override
    protected File doGetProjectDir() throws IOException {
        return workDir;
    }

    @Override
    protected JpsSdk<JpsDummyElement> addJdk(String name, String path) {
        String homePath = System.getProperty("java.home");
        String versionString = System.getProperty("java.version");
        JpsTypedLibrary<JpsSdk<JpsDummyElement>> jdk = myModel.getGlobal().addSdk(name, homePath, versionString, JpsJavaSdkType.INSTANCE);
        jdk.addRoot(JpsPathUtil.pathToUrl(path), JpsOrderRootType.COMPILED);
        return jdk.getProperties();
    }

    protected JpsLibrary addKotlinMockRuntimeDependency() {
        return addDependency(JpsJavaDependencyScope.COMPILE, myProject.getModules(), false, "kotlin-mock-runtime", ForTestCompileRuntime.mockRuntimeJarForTests());
    }

    protected JpsLibrary addKotlinRuntimeDependency() {
       return addKotlinRuntimeDependency(myProject);
    }

    protected JpsLibrary addKotlinJavaScriptStdlibDependency() {
        return addKotlinJavaScriptStdlibDependency(myProject);
    }

    protected JpsLibrary addKotlinJavaScriptDependency(String libraryName, File libraryFile) {
        return addDependency(JpsJavaDependencyScope.COMPILE, myProject.getModules(), false, libraryName, libraryFile);
    }

    static JpsLibrary addKotlinRuntimeDependency(@NotNull JpsProject project) {
       return addKotlinRuntimeDependency(JpsJavaDependencyScope.COMPILE, project.getModules(), false);
    }

    static JpsLibrary addKotlinTestRuntimeDependency(@NotNull JpsProject project) {
        return addDependency(JpsJavaDependencyScope.COMPILE, project.getModules(), false, "kotlin-test", PathUtil.getKotlinPathsForDistDirectory().getKotlinTestPath());
    }

    static JpsLibrary addKotlinJavaScriptStdlibDependency(@NotNull JpsProject project) {
        return addKotlinJavaScriptStdlibDependency(JpsJavaDependencyScope.COMPILE, project.getModules(), false);
    }

    protected static JpsLibrary addKotlinRuntimeDependency(JpsJavaDependencyScope type, Collection<JpsModule> modules, boolean exported) {
        return addDependency(type, modules, exported, "kotlin-runtime", PathUtil.getKotlinPathsForDistDirectory().getRuntimePath());
    }

    protected static JpsLibrary addKotlinJavaScriptStdlibDependency(JpsJavaDependencyScope type, Collection<JpsModule> modules, boolean exported) {
        return addDependency(type, modules, exported, "KotlinJavaScript", PathUtil.getKotlinPathsForDistDirectory().getJsStdLibJarPath());
    }

    protected static JpsLibrary addDependency(JpsJavaDependencyScope type, Collection<JpsModule> modules, boolean exported, String libraryName, File... file) {
        JpsLibrary library = modules.iterator().next().getProject().addLibrary(libraryName, JpsJavaLibraryType.INSTANCE);

        for (File fileRoot : file) {
            library.addRoot(fileRoot, JpsOrderRootType.COMPILED);
        }

        for (JpsModule module : modules) {
            JpsModuleRootModificationUtil.addDependency(module, library, type, exported);
        }
        return library;
    }
}
