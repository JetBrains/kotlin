// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.importing;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.Trinity;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.usageView.UsageInfo;
import org.gradle.initialization.BuildLayoutParameters;
import org.gradle.wrapper.PathAssembler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static com.intellij.openapi.util.Pair.pair;
import static com.intellij.testFramework.EdtTestUtil.runInEdtAndGet;

/**
 * @author Vladislav.Soroka
 */
public class GradleFindUsagesTest extends GradleImportingTestCase {

  /**
   * It's sufficient to run the test against one gradle version
   */
  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][]{{BASE_GRADLE_VERSION}});
  }

  @Override
  protected void collectAllowedRoots(List<String> roots, PathAssembler.LocalDistribution distribution) {
    super.collectAllowedRoots(roots, distribution);
    File gradleUserHomeDir = new BuildLayoutParameters().getGradleUserHomeDir();
    File generatedGradleJarsDir = new File(gradleUserHomeDir, "caches/" + gradleVersion + "/generated-gradle-jars");
    roots.add(generatedGradleJarsDir.getPath());
    File gradleDistLibDir = new File(distribution.getDistributionDir(), "gradle-" + gradleVersion + "/lib");
    roots.add(gradleDistLibDir.getPath());
  }

  @Test
  public void testBuildSrcClassesUsages() throws Exception {
    createProjectSubFile("settings.gradle", "rootProject.name = 'multiproject'\n" +
                                            "include ':app'");

    createProjectSubFile("buildSrc/src/main/groovy/testBuildSrcClassesUsages/BuildSrcClass.groovy", "package testBuildSrcClassesUsages;\n" +
                                                                                                    "public class BuildSrcClass {" +
                                                                                                    "   public String sayHello() { 'Hello!' }" +
                                                                                                    "}");
    createProjectSubFile("app/build.gradle", "def foo = new testBuildSrcClassesUsages.BuildSrcClass().sayHello()");

    importProject();
    assertModules("multiproject", "multiproject.app",
                  "multiproject.buildSrc", "multiproject.buildSrc.main", "multiproject.buildSrc.test");

    Module buildSrcModule = getModule("multiproject.buildSrc.main");
    assertNotNull(buildSrcModule);
    assertUsages("testBuildSrcClassesUsages.BuildSrcClass", GlobalSearchScope.moduleScope(buildSrcModule), 1);
    assertUsages("testBuildSrcClassesUsages.BuildSrcClass", "sayHello", GlobalSearchScope.moduleScope(buildSrcModule), 1);
  }

  @Test
  public void testMultiModuleBuildSrcClassesUsages() throws Exception {
    createProjectSubFile("settings.gradle", "rootProject.name = 'multiproject'\n" +
                                            "include ':app'");
    // buildSrc module files
    createProjectSubFile("buildSrc/settings.gradle", "include 'buildSrcSubProject'");
    createProjectSubFile("buildSrc/build.gradle", "allprojects {\n" +
                                                  "    apply plugin: 'groovy'\n" +
                                                  "    dependencies {\n" +
                                                  "        compile gradleApi()\n" +
                                                  "        compile localGroovy()\n" +
                                                  "    }\n" +
                                                  "    repositories {\n" +
                                                  "        mavenCentral()\n" +
                                                  "    }\n" +
                                                  "\n" +
                                                  "    if (it != rootProject) {\n" +
                                                  "        rootProject.dependencies {\n" +
                                                  "            runtime project(path)\n" +
                                                  "        }\n" +
                                                  "    }\n" +
                                                  "}\n");
    createProjectSubFile("buildSrc/src/main/groovy/testMultiModuleBuildSrcClassesUsages/BuildSrcClass.groovy",
                         "package testMultiModuleBuildSrcClassesUsages;\n" +
                         "public class BuildSrcClass {}");
    createProjectSubFile("buildSrc/buildSrcSubProject/src/main/java/testMultiModuleBuildSrcClassesUsages/BuildSrcAdditionalClass.java",
                         "package testMultiModuleBuildSrcClassesUsages;\n" +
                         "public class BuildSrcAdditionalClass {}");

    createProjectSubFile("build.gradle", "def foo = new testMultiModuleBuildSrcClassesUsages.BuildSrcClass()");
    createProjectSubFile("app/build.gradle", "def foo1 = new testMultiModuleBuildSrcClassesUsages.BuildSrcClass()\n" +
                                             "def foo2 = new testMultiModuleBuildSrcClassesUsages.BuildSrcAdditionalClass()");

    importProject();
    assertModules("multiproject", "multiproject.app",
                  "multiproject.buildSrc", "multiproject.buildSrc.main", "multiproject.buildSrc.test",
                  "multiproject.buildSrc.buildSrcSubProject", "multiproject.buildSrc.buildSrcSubProject.main", "multiproject.buildSrc.buildSrcSubProject.test");

    assertUsages(pair("testMultiModuleBuildSrcClassesUsages.BuildSrcClass", 2),
                 pair("testMultiModuleBuildSrcClassesUsages.BuildSrcAdditionalClass", 1));

    importProjectUsingSingeModulePerGradleProject();
    assertModules("multiproject", "multiproject.app",
                  "multiproject.buildSrc",
                  "multiproject.buildSrc.buildSrcSubProject");

    assertUsages(pair("testMultiModuleBuildSrcClassesUsages.BuildSrcClass", 2),
                 pair("testMultiModuleBuildSrcClassesUsages.BuildSrcAdditionalClass", 1));
  }

  @Test
  public void testIncludedBuildSrcClassesUsages_nonQN() throws Exception {
    createProjectWithIncludedBuildAndBuildSrcModules("testIncludedBuildSrcClassesUsages_nonQN");
    // check for non-qualified module names
    getCurrentExternalProjectSettings().setUseQualifiedModuleNames(false);

    importProject();
    assertModules("multiproject", "app",
                  "multiproject_buildSrc", "multiproject_buildSrc_main", "multiproject_buildSrc_test",
                  "gradle-plugin", "gradle-plugin_test", "gradle-plugin_main",
                  "gradle-plugin_buildSrc", "gradle-plugin_buildSrc_main", "gradle-plugin_buildSrc_test");

    assertUsages("testIncludedBuildSrcClassesUsages_nonQN.BuildSrcClass", 2);
    assertUsages("testIncludedBuildSrcClassesUsages_nonQN.IncludedBuildSrcClass", 1);
    assertUsages("testIncludedBuildSrcClassesUsages_nonQN.IncludedBuildClass", 2);
  }

  @Test
  public void testIncludedBuildSrcClassesUsages_merged_nonQN() throws Exception {
    createProjectWithIncludedBuildAndBuildSrcModules("testIncludedBuildSrcClassesUsages_merged_nonQN");
    // check for non-qualified module names
    getCurrentExternalProjectSettings().setUseQualifiedModuleNames(false);

    importProjectUsingSingeModulePerGradleProject();
    assertModules("multiproject", "app",
                  "multiproject_buildSrc",
                  "gradle-plugin",
                  "gradle-plugin_buildSrc");
    assertUsages(pair("testIncludedBuildSrcClassesUsages_merged_nonQN.BuildSrcClass", 2),
                 pair("testIncludedBuildSrcClassesUsages_merged_nonQN.IncludedBuildSrcClass", 1));
    assertUsages("testIncludedBuildSrcClassesUsages_merged_nonQN.IncludedBuildClass", 2);
  }

  @Test
  public void testIncludedBuildSrcClassesUsages_qualified_names() throws Exception {
    createProjectWithIncludedBuildAndBuildSrcModules("testIncludedBuildSrcClassesUsages_qualified_names");
    importProject();
    assertModules("multiproject", "multiproject.app",
                  "multiproject.buildSrc", "multiproject.buildSrc.main", "multiproject.buildSrc.test",
                  "gradle-plugin", "gradle-plugin.test", "gradle-plugin.main",
                  "gradle-plugin.buildSrc", "gradle-plugin.buildSrc.main", "gradle-plugin.buildSrc.test");
    assertUsages(pair("testIncludedBuildSrcClassesUsages_qualified_names.BuildSrcClass", 2),
                 pair("testIncludedBuildSrcClassesUsages_qualified_names.IncludedBuildSrcClass", 1));
    assertUsages("testIncludedBuildSrcClassesUsages_qualified_names.IncludedBuildClass", 2);
  }

  @Test
  public void testIncludedBuildSrcClassesUsages_merged_qualified_names() throws Exception {
    createProjectWithIncludedBuildAndBuildSrcModules("testIncludedBuildSrcClassesUsages_merged_qualified_names");
    // check for qualified module names
    getCurrentExternalProjectSettings().setUseQualifiedModuleNames(true);
    importProjectUsingSingeModulePerGradleProject();
    assertModules("multiproject", "multiproject.app",
                  "multiproject.buildSrc",
                  "gradle-plugin",
                  "gradle-plugin.buildSrc");
    assertUsages(pair("testIncludedBuildSrcClassesUsages_merged_qualified_names.BuildSrcClass", 2),
                 pair("testIncludedBuildSrcClassesUsages_merged_qualified_names.IncludedBuildSrcClass", 1));
    assertUsages("testIncludedBuildSrcClassesUsages_merged_qualified_names.IncludedBuildClass", 2);
  }

  private void createProjectWithIncludedBuildAndBuildSrcModules(@NotNull String classPackage) throws IOException {
    createProjectSubFile("settings.gradle", "rootProject.name = 'multiproject'\n" +
                                            "include ':app'\n" +
                                            "includeBuild 'gradle-plugin'");
    createProjectSubFile("buildSrc/src/main/groovy/" + classPackage + "/BuildSrcClass.groovy", "package " + classPackage + ";\n" +
                                                                                               "public class BuildSrcClass {}");

    createProjectSubFile("build.gradle", "buildscript {\n" +
                                         "    dependencies {\n" +
                                         "        classpath 'my.included:gradle-plugin:0'\n" +
                                         "    }\n" +
                                         "}\n" +
                                         "def foo1 = new " + classPackage + ".BuildSrcClass()\n" +
                                         "def foo2 = new " + classPackage + ".IncludedBuildClass()");
    createProjectSubFile("app/build.gradle", "def foo1 = new " + classPackage + ".BuildSrcClass()\n" +
                                             "def foo2 = new " + classPackage + ".IncludedBuildClass()");

    // included build
    createProjectSubFile("gradle-plugin/settings.gradle", "");
    createProjectSubFile("gradle-plugin/build.gradle", "group 'my.included'\n" +
                                                       "apply plugin: 'java'\n" +
                                                       "def foo = new " + classPackage + ".IncludedBuildSrcClass()");
    createProjectSubFile("gradle-plugin/buildSrc/src/main/groovy/" + classPackage + "/IncludedBuildSrcClass.groovy",
                         "package " + classPackage + ";\n" +
                         "public class IncludedBuildSrcClass {}");
    createProjectSubFile("gradle-plugin/src/main/java/" + classPackage + "/IncludedBuildClass.java",
                         "package " + classPackage + ";\n" +
                         "public class IncludedBuildClass {}");
  }

  private void assertUsages(String fqn, GlobalSearchScope scope, int count) throws Exception {
    assertUsages(fqn, null, scope, count);
  }

  private void assertUsages(@NotNull String fqn, @Nullable String methodName, GlobalSearchScope scope, int count) throws Exception {
    PsiClass[] psiClasses = runInEdtAndGet(() -> JavaPsiFacade.getInstance(myProject).findClasses(fqn, scope));
    assertEquals(1, psiClasses.length);
    PsiClass aClass = psiClasses[0];
    if (methodName != null) {
      PsiMethod[] methods = runInEdtAndGet(() -> aClass.findMethodsByName(methodName, false));
      List<UsageInfo> actualUsages = new ArrayList<>();
      for (PsiMethod method : methods) {
        actualUsages.addAll(findUsages(method));
      }
      assertUsagesCount(count, actualUsages);
    }
    else {
      assertUsagesCount(count, aClass);
    }
  }

  private void assertUsages(String fqn, int count) throws Exception {
    assertUsages(fqn, GlobalSearchScope.projectScope(myProject), count);
  }

  @SafeVarargs
  private final void assertUsages(Trinity<String, GlobalSearchScope, Integer>... classUsageCount) throws Exception {
    for (Trinity<String, GlobalSearchScope, Integer> trinity : classUsageCount) {
      assertUsages(trinity.first, trinity.second, trinity.third);
    }
  }

  @SafeVarargs
  private final void assertUsages(Pair<String, Integer>... classUsageCount) throws Exception {
    for (Pair<String, Integer> pair : classUsageCount) {
      assertUsages(Trinity.create(pair.first, GlobalSearchScope.projectScope(myProject), pair.second));
    }
  }

  private static void assertUsagesCount(int expectedUsagesCount, PsiElement element) throws Exception {
    assertUsagesCount(expectedUsagesCount, findUsages(element));
  }

  private static void assertUsagesCount(int expectedUsagesCount, Collection<UsageInfo> usages) throws Exception {
    String message = "Found usges: " + runInEdtAndGet(() -> {
      StringBuilder buf = new StringBuilder();
      for (UsageInfo usage : usages) {
        buf.append(usage).append(", from ").append(usage.getVirtualFile().getPath());
        Segment navigationRange = usage.getNavigationRange();
        if (navigationRange != null) {
          buf.append(": ").append(usage.getNavigationRange().getStartOffset())
            .append(",").append(usage.getNavigationRange().getEndOffset());
        }
        buf.append(" \n");
      }

      return buf.toString();
    });
    assertEquals(message, expectedUsagesCount, usages.size());
  }
}
