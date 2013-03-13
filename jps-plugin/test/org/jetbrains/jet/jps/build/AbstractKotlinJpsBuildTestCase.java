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

import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.utils.PathUtil;
import org.jetbrains.jps.builders.JpsBuildTestCase;
import org.jetbrains.jps.model.JpsDummyElement;
import org.jetbrains.jps.model.JpsModuleRootModificationUtil;
import org.jetbrains.jps.model.java.JpsAnnotationRootType;
import org.jetbrains.jps.model.java.JpsJavaDependencyScope;
import org.jetbrains.jps.model.java.JpsJavaLibraryType;
import org.jetbrains.jps.model.java.JpsJavaSdkType;
import org.jetbrains.jps.model.library.JpsLibrary;
import org.jetbrains.jps.model.library.JpsOrderRootType;
import org.jetbrains.jps.model.library.JpsTypedLibrary;
import org.jetbrains.jps.model.library.sdk.JpsSdk;
import org.jetbrains.jps.model.module.JpsModule;
import org.jetbrains.jps.util.JpsPathUtil;

import java.io.File;
import java.io.IOException;

public abstract class AbstractKotlinJpsBuildTestCase extends JpsBuildTestCase {
    protected static final String TEST_DATA_PATH = "jps-plugin/testData/";

    protected File workDir;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        System.setProperty("kotlin.jps.tests", "true");
    }

    @Override
    public void tearDown() throws Exception {
        System.clearProperty("kotlin.jps.tests");
        super.tearDown();
    }

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
        jdk.addRoot(JpsPathUtil.pathToUrl(PathUtil.getKotlinPathsForDistDirectory().getJdkAnnotationsPath().getAbsolutePath()), JpsAnnotationRootType.INSTANCE);
        return jdk.getProperties();
    }

    protected JpsLibrary addKotlinRuntimeDependency() {
       return addKotlinRuntimeDependency(JpsJavaDependencyScope.COMPILE);
    }

    protected JpsLibrary addKotlinRuntimeDependency(JpsJavaDependencyScope type) {
        JpsLibrary library = myProject.addLibrary("kotlin-runtime", JpsJavaLibraryType.INSTANCE);
        File runtime = PathUtil.getKotlinPathsForDistDirectory().getRuntimePath();
        library.addRoot(runtime, JpsOrderRootType.COMPILED);
        for (JpsModule module : myProject.getModules()) {
            JpsModuleRootModificationUtil.addDependency(module, library, type, false);
        }
        return library;
    }
}
