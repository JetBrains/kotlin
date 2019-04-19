/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package org.jetbrains.plugins.gradle.importing;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Vladislav.Soroka
 */
public class GradleClassFinderTest extends GradleImportingTestCase {

  /**
   * It's sufficient to run the test against one gradle version
   */
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{BASE_GRADLE_VERSION}});
  }

  @Test
  public void testClassesFilter() throws Exception {
    createProjectSubFile("settings.gradle", "rootProject.name = 'multiproject'\n" +
                                            "include ':app'");

    // app module files
    createProjectSubFile("app/src/main/groovy/App.groovy", "class App {}");
    // buildSrc module files
    createProjectSubFile("buildSrc/src/main/groovy/org/buildsrc/BuildSrcClass.groovy", "package org.buildsrc;\n" +
                                                                                       "import groovy.util.AntBuilder;\n" +
                                                                                       "public class BuildSrcClass {}");
    importProject("subprojects {\n" +
                  "    apply plugin: 'groovy'\n" +
                  "}");
    assertModules("multiproject",
                  "multiproject.app", "multiproject.app.main", "multiproject.app.test",
                  "multiproject.buildSrc", "multiproject.buildSrc.main", "multiproject.buildSrc.test");
    Module buildSrcModule = getModule("multiproject.buildSrc.main");
    assertNotNull(buildSrcModule);
    ApplicationManager.getApplication().runReadAction(() -> {
      PsiClass[] appClasses = JavaPsiFacade.getInstance(myProject).findClasses("App", GlobalSearchScope.allScope(myProject));
      assertEquals(1, appClasses.length);

      PsiClass[] buildSrcClasses =
        JavaPsiFacade.getInstance(myProject).findClasses("org.buildsrc.BuildSrcClass", GlobalSearchScope.moduleScope(buildSrcModule));
      assertEquals(1, buildSrcClasses.length);
    });
  }
}
