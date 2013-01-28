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
import org.jetbrains.jps.model.java.*;

import java.io.File;

public class KotlinJpsBuildTestCase extends AbstractKotlinJpsBuildTestCase {
    private static final String PROJECT_NAME = "kotlinProject";
    private static final String JDK_NAME = "IDEA_JDK";

    @Override
    public void setUp() throws Exception {
        super.setUp();
        File sourceFilesRoot = new File(TEST_DATA_PATH + getTestName(false));
        workDir = copyTestDataToTmpDir(sourceFilesRoot);
    }

    @Override
    public void tearDown() throws Exception {
        FileUtil.delete(workDir);
        super.tearDown();
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

    public void testTestDependencyLibrary() throws Throwable {
        initProject();
        addKotlinRuntimeDependency(JpsJavaDependencyScope.TEST);
        makeAll().assertSuccessful();
        change(workDir + "/src/src.kt", "fun foo() { println() }");
        makeAll().assertFailed();
    }

}
